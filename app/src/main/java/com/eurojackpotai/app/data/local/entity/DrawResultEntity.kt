package com.eurojackpotai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.eurojackpotai.app.domain.model.DrawResult
import java.time.LocalDate

/**
 * Numbers are stored as comma-separated strings rather than a separate join table.
 * A EuroJackpot combination is small and fixed-size (5 + 2), never queried per-number
 * at the SQL level — all number-level analysis happens in Kotlin over the full list —
 * so a join table would add real complexity (Phase 37 explicitly asks to avoid that)
 * for no query benefit. [Converters] handles the (de)serialization.
 */
@Entity(tableName = "draw_results")
data class DrawResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drawDate: LocalDate,
    val mainNumbersCsv: String,
    val euroNumbersCsv: String,
    val jackpotEur: Long?,
    val isSampleData: Boolean
)

fun DrawResultEntity.toDomain(): DrawResult = DrawResult(
    id = id,
    date = drawDate,
    mainNumbers = mainNumbersCsv.split(",").map { it.trim().toInt() },
    euroNumbers = euroNumbersCsv.split(",").map { it.trim().toInt() },
    jackpotEur = jackpotEur,
    isSampleData = isSampleData
)

fun DrawResult.toEntity(): DrawResultEntity = DrawResultEntity(
    id = id,
    drawDate = date,
    mainNumbersCsv = mainNumbers.joinToString(","),
    euroNumbersCsv = euroNumbers.joinToString(","),
    jackpotEur = jackpotEur,
    isSampleData = isSampleData
)
