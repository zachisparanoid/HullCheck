package com.hullcheck.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hullcheck.data.model.LengthUnit
import com.hullcheck.data.model.PhotoQuality
import com.hullcheck.data.model.ProjectSort
import com.hullcheck.data.model.SurveyType
import com.hullcheck.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of truth for user preferences, backed by Preferences DataStore. */
internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "hullcheck_settings")

/** Immutable snapshot of all app settings. */
data class AppSettings(
    val surveyorName: String = "",
    val defaultSurveyType: SurveyType = SurveyType.PRE_PURCHASE,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val lengthUnit: LengthUnit = LengthUnit.FEET,
    val photoQuality: PhotoQuality = PhotoQuality.STANDARD,
    val dynamicColor: Boolean = false,
    val projectSort: ProjectSort = ProjectSort.DATE,
    // Report letterhead / certification
    val firmName: String = "",
    val licenseNumber: String = "",
    val logoPath: String? = null,
    val signaturePath: String? = null,
    val reportDisclaimer: String = "",
)

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val SURVEYOR_NAME = stringPreferencesKey("surveyor_name")
        val DEFAULT_SURVEY_TYPE = stringPreferencesKey("default_survey_type")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LENGTH_UNIT = stringPreferencesKey("length_unit")
        val PHOTO_QUALITY = stringPreferencesKey("photo_quality")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PROJECT_SORT = stringPreferencesKey("project_sort")
        val FIRM_NAME = stringPreferencesKey("firm_name")
        val LICENSE_NUMBER = stringPreferencesKey("license_number")
        val LOGO_PATH = stringPreferencesKey("logo_path")
        val SIGNATURE_PATH = stringPreferencesKey("signature_path")
        val REPORT_DISCLAIMER = stringPreferencesKey("report_disclaimer")
    }

    val settings: Flow<AppSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            AppSettings(
                surveyorName = prefs[Keys.SURVEYOR_NAME] ?: "",
                defaultSurveyType = prefs[Keys.DEFAULT_SURVEY_TYPE].toEnum(SurveyType.PRE_PURCHASE),
                themeMode = prefs[Keys.THEME_MODE].toEnum(ThemeMode.SYSTEM),
                lengthUnit = prefs[Keys.LENGTH_UNIT].toEnum(LengthUnit.FEET),
                photoQuality = prefs[Keys.PHOTO_QUALITY].toEnum(PhotoQuality.STANDARD),
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
                projectSort = prefs[Keys.PROJECT_SORT].toEnum(ProjectSort.DATE),
                firmName = prefs[Keys.FIRM_NAME] ?: "",
                licenseNumber = prefs[Keys.LICENSE_NUMBER] ?: "",
                logoPath = prefs[Keys.LOGO_PATH],
                signaturePath = prefs[Keys.SIGNATURE_PATH],
                reportDisclaimer = prefs[Keys.REPORT_DISCLAIMER] ?: "",
            )
        }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setSurveyorName(value: String) = dataStore.edit { it[Keys.SURVEYOR_NAME] = value }
    suspend fun setDefaultSurveyType(value: SurveyType) = dataStore.edit { it[Keys.DEFAULT_SURVEY_TYPE] = value.name }
    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[Keys.THEME_MODE] = value.name }
    suspend fun setLengthUnit(value: LengthUnit) = dataStore.edit { it[Keys.LENGTH_UNIT] = value.name }
    suspend fun setPhotoQuality(value: PhotoQuality) = dataStore.edit { it[Keys.PHOTO_QUALITY] = value.name }
    suspend fun setDynamicColor(value: Boolean) = dataStore.edit { it[Keys.DYNAMIC_COLOR] = value }
    suspend fun setProjectSort(value: ProjectSort) = dataStore.edit { it[Keys.PROJECT_SORT] = value.name }
    suspend fun setFirmName(value: String) = dataStore.edit { it[Keys.FIRM_NAME] = value }
    suspend fun setLicenseNumber(value: String) = dataStore.edit { it[Keys.LICENSE_NUMBER] = value }
    suspend fun setReportDisclaimer(value: String) = dataStore.edit { it[Keys.REPORT_DISCLAIMER] = value }

    suspend fun setLogoPath(value: String?) = dataStore.edit {
        if (value == null) it.remove(Keys.LOGO_PATH) else it[Keys.LOGO_PATH] = value
    }

    suspend fun setSignaturePath(value: String?) = dataStore.edit {
        if (value == null) it.remove(Keys.SIGNATURE_PATH) else it[Keys.SIGNATURE_PATH] = value
    }
}

/** Decode a stored enum name, falling back to [default] for null/unknown values. */
private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default
