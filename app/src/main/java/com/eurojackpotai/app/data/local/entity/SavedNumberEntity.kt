package com.eurojackpotai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.NumberSource
import com.eurojackpotai.app.domain.model.SavedNumber
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "saved_numbers")
data class SavedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mainNumbersCsv: String,
    val euroNumbersCsv: String,
    val source: String,
    val generationMode: String?,
    val note: String,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
    val targetDrawDate: LocalDate? = null,
    val drawRoundLabel: String = "",
    val ticketPhotoPath: String? = null
)

fun SavedNumberEntity.toDomain(): SavedNumber = SavedNumber(
    id = id,
    name = name,
    mainNumbers = mainNumbersCsv.split(",").map { it.trim().toInt() },
    euroNumbers = euroNumbersCsv.split(",").map { it.trim().toInt() },
    source = NumberSource.valueOf(source),
    generationMode = generationMode?.let { GenerationMode.valueOf(it) },
    note = note,
    isActive = isActive,
    createdAt = createdAt,
    targetDrawDate = targetDrawDate,
    drawRoundLabel = drawRoundLabel,
    ticketPhotoPath = ticketPhotoPath
)

fun SavedNumber.toEntity(): SavedNumberEntity = SavedNumberEntity(
    id = id,
    name = name,
    mainNumbersCsv = mainNumbers.joinToString(","),
    euroNumbersCsv = euroNumbers.joinToString(","),
    source = source.name,
    generationMode = generationMode?.name,
    note = note,
    isActive = isActive,
    createdAt = createdAt,
    targetDrawDate = targetDrawDate,
    drawRoundLabel = drawRoundLabel,
    ticketPhotoPath = ticketPhotoPath
)
