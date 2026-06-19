package com.hullcheck.ui.projecteditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.R
import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.LengthUnit
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.SurveyType
import com.hullcheck.data.repository.ProjectRepository
import com.hullcheck.data.repository.SettingsRepository
import com.hullcheck.data.repository.TemplateRepository
import com.hullcheck.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.util.UUID
import javax.inject.Inject

data class EditorForm(
    val boatName: String = "",
    val boatType: BoatType = BoatType.SAIL_MONOHULL,
    val make: String = "",
    val model: String = "",
    val yearBuilt: String = "",
    val hullId: String = "",
    val length: String = "",
    val registrationNumber: String = "",
    val clientName: String = "",
    val surveyorName: String = "",
    val location: String = "",
    val surveyDate: LocalDate = LocalDate.now(),
    val surveyType: SurveyType = SurveyType.PRE_PURCHASE,
    val generalNotes: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class TemplateOption(val id: String, val name: String)

data class ProjectEditorUiState(
    val loading: Boolean = true,
    val isEditing: Boolean = false,
    val lengthUnit: LengthUnit = LengthUnit.FEET,
    val form: EditorForm = EditorForm(),
    val templates: List<TemplateOption> = emptyList(),
    val selectedTemplateId: String? = null,
    val boatNameError: Boolean = false,
    val yearError: Boolean = false,
    val lengthError: Boolean = false,
    val dirty: Boolean = false,
    val savedProjectId: String? = null,
)

@HiltViewModel
class ProjectEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val templateRepository: TemplateRepository,
) : ViewModel() {

    private val projectId: String? = savedStateHandle[Routes.ARG_PROJECT_ID]
    private var loaded: Project? = null

    private val _state = MutableStateFlow(ProjectEditorUiState(isEditing = projectId != null))
    val state: StateFlow<ProjectEditorUiState> = _state.asStateFlow()

    val maxYear: Int = Year.now().value + 1

    init {
        viewModelScope.launch {
            val settings = settingsRepository.current()
            if (projectId != null) {
                val project = projectRepository.getProjectMeta(projectId)
                if (project != null) {
                    loaded = project
                    _state.update {
                        it.copy(
                            loading = false,
                            lengthUnit = settings.lengthUnit,
                            form = project.toForm(settings.lengthUnit),
                        )
                    }
                } else {
                    _state.update { it.copy(loading = false) }
                }
            } else {
                val templates = templateRepository.getAll().map { TemplateOption(it.id, it.name) }
                val defaultId = templates.firstOrNull { it.id == "builtin_default_full_survey" }?.id
                    ?: templates.firstOrNull()?.id
                _state.update {
                    it.copy(
                        loading = false,
                        lengthUnit = settings.lengthUnit,
                        templates = templates,
                        selectedTemplateId = defaultId,
                        form = EditorForm(
                            surveyorName = settings.surveyorName,
                            surveyType = settings.defaultSurveyType,
                        ),
                    )
                }
            }
        }
    }

    private fun edit(transform: (EditorForm) -> EditorForm) {
        _state.update { it.copy(form = transform(it.form), dirty = true) }
    }

    fun setBoatName(v: String) { _state.update { it.copy(form = it.form.copy(boatName = v), dirty = true, boatNameError = false) } }
    fun setBoatType(v: BoatType) = edit { it.copy(boatType = v) }
    fun setMake(v: String) = edit { it.copy(make = v) }
    fun setModel(v: String) = edit { it.copy(model = v) }
    fun setYear(v: String) { _state.update { it.copy(form = it.form.copy(yearBuilt = v.filter { c -> c.isDigit() }), dirty = true, yearError = false) } }
    fun setHullId(v: String) = edit { it.copy(hullId = v) }
    fun setLength(v: String) { _state.update { it.copy(form = it.form.copy(length = v), dirty = true, lengthError = false) } }
    fun setRegistration(v: String) = edit { it.copy(registrationNumber = v) }
    fun setClientName(v: String) = edit { it.copy(clientName = v) }
    fun setSurveyorName(v: String) = edit { it.copy(surveyorName = v) }
    fun setLocation(v: String) = edit { it.copy(location = v) }
    fun setSurveyDate(v: LocalDate) = edit { it.copy(surveyDate = v) }
    fun setSurveyType(v: SurveyType) = edit { it.copy(surveyType = v) }
    fun setGeneralNotes(v: String) = edit { it.copy(generalNotes = v) }
    fun setLocation(lat: Double, lon: Double) = edit { it.copy(latitude = lat, longitude = lon) }
    fun clearLocation() = edit { it.copy(latitude = null, longitude = null) }
    fun setTemplate(id: String) { _state.update { it.copy(selectedTemplateId = id) } }

    fun save() {
        val form = _state.value.form
        val unit = _state.value.lengthUnit

        val nameError = form.boatName.isBlank()
        val year = form.yearBuilt.trim().toIntOrNull()
        val yearError = form.yearBuilt.isNotBlank() && (year == null || year !in 1900..maxYear)
        val lengthInput = form.length.trim().toDoubleOrNull()
        val lengthFeet = lengthInput?.let { if (unit == LengthUnit.METERS) it / 0.3048 else it }
        val lengthError = form.length.isNotBlank() && (lengthFeet == null || lengthFeet !in 0.0..400.0)

        if (nameError || yearError || lengthError) {
            _state.update { it.copy(boatNameError = nameError, yearError = yearError, lengthError = lengthError) }
            return
        }

        viewModelScope.launch {
            val base = loaded
            val now = Instant.now()
            val project = Project(
                id = base?.id ?: UUID.randomUUID().toString(),
                boatName = form.boatName.trim(),
                boatType = form.boatType,
                make = form.make.blankToNull(),
                model = form.model.blankToNull(),
                yearBuilt = year,
                hullId = form.hullId.blankToNull(),
                lengthFeet = lengthFeet,
                registrationNumber = form.registrationNumber.blankToNull(),
                clientName = form.clientName.blankToNull(),
                surveyorName = form.surveyorName.blankToNull(),
                location = form.location.blankToNull(),
                surveyDate = form.surveyDate,
                surveyType = form.surveyType,
                status = base?.status ?: ProjectStatus.DRAFT,
                generalNotes = form.generalNotes.blankToNull(),
                reportSummary = base?.reportSummary,
                coverPhotoPath = base?.coverPhotoPath,
                latitude = form.latitude,
                longitude = form.longitude,
                createdAt = base?.createdAt ?: now,
                updatedAt = now,
            )
            val id = if (base == null) {
                projectRepository.createProject(project, _state.value.selectedTemplateId)
            } else {
                projectRepository.updateProject(project)
                project.id
            }
            _state.update { it.copy(savedProjectId = id) }
        }
    }

    private fun Project.toForm(unit: LengthUnit): EditorForm = EditorForm(
        boatName = boatName,
        boatType = boatType,
        make = make.orEmpty(),
        model = model.orEmpty(),
        yearBuilt = yearBuilt?.toString().orEmpty(),
        hullId = hullId.orEmpty(),
        length = lengthFeet?.let {
            val v = if (unit == LengthUnit.METERS) it * 0.3048 else it
            if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
        }.orEmpty(),
        registrationNumber = registrationNumber.orEmpty(),
        clientName = clientName.orEmpty(),
        surveyorName = surveyorName.orEmpty(),
        location = location.orEmpty(),
        surveyDate = surveyDate,
        surveyType = surveyType,
        generalNotes = generalNotes.orEmpty(),
        latitude = latitude,
        longitude = longitude,
    )

    private fun String.blankToNull(): String? = trim().ifBlank { null }
}
