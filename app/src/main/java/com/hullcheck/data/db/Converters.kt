package com.hullcheck.data.db

import androidx.room.TypeConverter
import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.Severity
import com.hullcheck.data.model.SurveyType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters. Instants persist as epoch-millis, LocalDate as epoch-day, enums
 * as their constant [name] (so SQL string predicates like status != 'PENDING' work in DAO
 * aggregate queries), and List<String> as a JSON array.
 */
class Converters {

    // ----- java.time -----
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun localDateToLong(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun longToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    // ----- enums (stored by name) -----
    @TypeConverter fun boatTypeToString(value: BoatType): String = value.name
    @TypeConverter fun stringToBoatType(value: String): BoatType = BoatType.valueOf(value)

    @TypeConverter fun surveyTypeToString(value: SurveyType): String = value.name
    @TypeConverter fun stringToSurveyType(value: String): SurveyType = SurveyType.valueOf(value)

    @TypeConverter fun projectStatusToString(value: ProjectStatus): String = value.name
    @TypeConverter fun stringToProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)

    @TypeConverter fun itemStatusToString(value: ItemStatus): String = value.name
    @TypeConverter fun stringToItemStatus(value: String): ItemStatus = ItemStatus.valueOf(value)

    @TypeConverter fun conditionToString(value: ConditionRating): String = value.name
    @TypeConverter fun stringToCondition(value: String): ConditionRating = ConditionRating.valueOf(value)

    @TypeConverter fun severityToString(value: Severity): String = value.name
    @TypeConverter fun stringToSeverity(value: String): Severity = Severity.valueOf(value)

    // ----- List<String> -----
    @TypeConverter
    fun listToString(value: List<String>?): String? =
        value?.let { Json.encodeToString(ListSerializer(String.serializer()), it) }

    @TypeConverter
    fun stringToList(value: String?): List<String>? =
        value?.let { Json.decodeFromString(ListSerializer(String.serializer()), it) }
}
