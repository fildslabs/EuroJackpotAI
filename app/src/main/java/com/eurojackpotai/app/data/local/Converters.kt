package com.eurojackpotai.app.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromEpochSecond(value: Long?): LocalDateTime? =
        value?.let { LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.UTC) }

    @TypeConverter
    fun toEpochSecond(dateTime: LocalDateTime?): Long? =
        dateTime?.toEpochSecond(java.time.ZoneOffset.UTC)
}
