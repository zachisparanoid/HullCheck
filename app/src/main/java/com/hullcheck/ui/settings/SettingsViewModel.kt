package com.hullcheck.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.LengthUnit
import com.hullcheck.data.model.PhotoQuality
import com.hullcheck.data.model.SurveyType
import com.hullcheck.data.model.ThemeMode
import com.hullcheck.data.repository.AppSettings
import com.hullcheck.data.repository.ProjectRepository
import com.hullcheck.data.repository.SettingsRepository
import com.hullcheck.util.FileStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository,
    private val fileStorage: FileStorage,
) : ViewModel() {

    val settings: StateFlow<AppSettings> =
        settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setSurveyorName(value: String) = viewModelScope.launch { settingsRepository.setSurveyorName(value) }
    fun setDefaultSurveyType(value: SurveyType) = viewModelScope.launch { settingsRepository.setDefaultSurveyType(value) }
    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(value) }
    fun setLengthUnit(value: LengthUnit) = viewModelScope.launch { settingsRepository.setLengthUnit(value) }
    fun setPhotoQuality(value: PhotoQuality) = viewModelScope.launch { settingsRepository.setPhotoQuality(value) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(value) }

    fun setFirmName(value: String) = viewModelScope.launch { settingsRepository.setFirmName(value) }
    fun setLicenseNumber(value: String) = viewModelScope.launch { settingsRepository.setLicenseNumber(value) }
    fun setReportDisclaimer(value: String) = viewModelScope.launch { settingsRepository.setReportDisclaimer(value) }

    fun setLogo(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val file = fileStorage.saveBrandingImage(uri, "logo")
        settingsRepository.setLogoPath(file.absolutePath)
    }

    fun clearLogo() = viewModelScope.launch(Dispatchers.IO) {
        fileStorage.deleteBranding("logo")
        settingsRepository.setLogoPath(null)
    }

    fun setSignature(uri: Uri) = viewModelScope.launch(Dispatchers.IO) {
        val file = fileStorage.saveBrandingImage(uri, "signature")
        settingsRepository.setSignaturePath(file.absolutePath)
    }

    fun clearSignature() = viewModelScope.launch(Dispatchers.IO) {
        fileStorage.deleteBranding("signature")
        settingsRepository.setSignaturePath(null)
    }

    fun deleteAllData() = viewModelScope.launch { projectRepository.deleteAllData() }
}
