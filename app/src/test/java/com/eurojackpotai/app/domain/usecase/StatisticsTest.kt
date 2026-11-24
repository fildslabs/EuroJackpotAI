package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.DrawResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Covers the checks from product spec section 36 (StatisticsTest): frequency counts,
 * pair counts and gap counts must all be exact over a small, hand-checkable dataset. */
class StatisticsTest {

    private val useCase = ComputeStatisticsUseCase()

    // Three draws, newest first. Number 7 appears in all three (gap 0, total 3).
    // Number 1 appears only in the oldest draw (gap 2, total 1). Pair (7,19) co-occurs twice.
    private val draws = listOf(
        DrawResult(date = LocalDate.of(2026, 1, 9), mainNumbers = listOf(7, 19, 20, 33, 45), euroNumbers = listOf(2, 5)),
        DrawResult(date = LocalDate.of(2026, 1, 6), mainNumbers = listOf(7, 19, 21, 30, 50), euroNumbers = listOf(2, 8)),
        DrawResult(date = LocalDate.of(2026, 1, 2), mainNumbers = listOf(1, 7, 22, 40, 49), euroNumbers = listOf(3, 8))
    )

    @Test
    fun `total frequency is counted correctly`() {
        val stats = useCase(draws)
        val freqFor7 = stats.mainNumberFrequencies.first { it.number == 7 }
        val freqFor1 = stats.mainNumberFrequencies.first { it.number == 1 }
        val freqFor2 = stats.mainNumberFrequencies.first { it.number == 2 } // never drawn as a main number

        assertEquals(3, freqFor7.totalCount)
        assertEquals(1, freqFor1.totalCount)
        assertEquals(0, freqFor2.totalCount)
    }

    @Test
    fun `gap since last seen is counted correctly`() {
        val stats = useCase(draws)
        val freqFor7 = stats.mainNumberFrequencies.first { it.number == 7 } // seen in the newest draw
        val freqFor1 = stats.mainNumberFrequencies.first { it.number == 1 } // seen only in the oldest draw
        val neverSeen = stats.mainNumberFrequencies.first { it.number == 2 }

        assertEquals(0, freqFor7.drawsSinceLastSeen)
        assertEquals(2, freqFor1.drawsSinceLastSeen)
        assertEquals(draws.size, neverSeen.drawsSinceLastSeen) // sentinel: "never seen in this history"
    }

    @Test
    fun `pair co-occurrence is counted correctly`() {
        val stats = useCase(draws)
        val pair7And19 = stats.topPairs.first { it.first == 7 && it.second == 19 }
        assertEquals(2, pair7And19.coOccurrenceCount)
    }

    @Test
    fun `euro number frequency is tracked separately from main numbers`() {
        val stats = useCase(draws)
        val euro2 = stats.euroNumberFrequencies.first { it.number == 2 }
        assertEquals(2, euro2.totalCount)
    }

    @Test
    fun `range distribution counts every main number exactly once`() {
        val stats = useCase(draws)
        val totalCounted = stats.rangeDistribution.sumOf { it.count }
        assertEquals(draws.size * 5, totalCounted)
    }
}
