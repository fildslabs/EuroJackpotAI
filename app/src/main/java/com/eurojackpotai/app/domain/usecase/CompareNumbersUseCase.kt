package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.PrizeTier
import com.eurojackpotai.app.domain.model.SavedNumber

data class ComparisonResult(
    val mainMatches: Int,
    val euroMatches: Int,
    val prizeTier: PrizeTier?
)

/** Compares a saved combination against an official draw. Main and Euro numbers are
 * always scored separately — never pooled together — since they're drawn from two
 * different, non-overlapping number ranges. */
class CompareNumbersUseCase {

    operator fun invoke(saved: SavedNumber, draw: DrawResult): ComparisonResult =
        invoke(saved.mainNumbers, saved.euroNumbers, draw)

    operator fun invoke(mainNumbers: List<Int>, euroNumbers: List<Int>, draw: DrawResult): ComparisonResult {
        val mainMatches = mainNumbers.count { it in draw.mainNumbers }
        val euroMatches = euroNumbers.count { it in draw.euroNumbers }
        return ComparisonResult(
            mainMatches = mainMatches,
            euroMatches = euroMatches,
            prizeTier = PrizeTier.forMatch(mainMatches, euroMatches)
        )
    }
}
