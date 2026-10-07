package com.xiaojidiu.philosophy.data.local.converter

import androidx.room.TypeConverter
import com.xiaojidiu.philosophy.domain.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @TypeConverter fun stringsToJson(value: List<String>) = Json.encodeToString(value)
    @TypeConverter fun jsonToStrings(value: String): List<String> = Json.decodeFromString(value)
    @TypeConverter fun perspectivesToJson(value: List<Perspective>) = Json.encodeToString(value)
    @TypeConverter fun jsonToPerspectives(value: String): List<Perspective> = Json.decodeFromString(value)
    @TypeConverter fun scoresToJson(value: Map<PhilosophyDimension, Int>) = Json.encodeToString(value)
    @TypeConverter fun jsonToScores(value: String): Map<PhilosophyDimension, Int> = Json.decodeFromString(value)
    @TypeConverter fun dimensionToString(value: PhilosophyDimension) = value.name
    @TypeConverter fun stringToDimension(value: String) = PhilosophyDimension.valueOf(value)
}
