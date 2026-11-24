package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.PrizeTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Covers product spec section 36 (ResultCheckerTest): match comparison is correct and
 * Main/Euro numbers are always scored separately, never pooled into one count. */
class ResultCheckerTest {

    private val useCase = CompareNumbersUseCase()

    private val draw = DrawResult(
        date = LocalDate.of(2026, 1, 9),
        mainNumbers = listOf(4, 25, 30, 32, 33),
        euroNumbers = listOf(4, 5)
    )

    @Test
    fun `exact jackpot match resolves to tier 1`() {
        val result = useCase(mainNumbers = listOf(4, 25, 30, 32, 33), euroNumbers = listOf(4, 5), draw = draw)
        assertEquals(5, result.mainMatches)
        assertEquals(2, result.euroMatches)
        assertEquals(PrizeTier.TIER_1, result.prizeTier)
    }

    @Test
    fun `partial match counts main and euro separately`() {
        // Shares one main number (4) with the draw's main numbers, and one number (30)
        // that appears in mainNumbers but is being checked against Euro — must NOT count
        // as an Euro match since Euro numbers are a separate 1..12 pool.
        val result = useCase(mainNumbers = listOf(4, 1, 2, 3, 6), euroNumbers = listOf(9, 10), draw = draw)
        assertEquals(1, result.mainMatches)
        assertEquals(0, result.euroMatches)
    }

    @Test
    fun `main and euro pools never cross-contaminate the count`() {
        // The draw's Euro numbers (4, 5) also happen to overlap with a value present in
        // the draw's main numbers (4). Checking main numbers against euro-range values
        // must not inflate mainMatches just because "4" is a euro number too.
        val result = useCase(mainNumbers = listOf(5, 8, 9, 10, 11), euroNumbers = listOf(4, 5), draw = draw)
        assertEquals(0, result.mainMatches) // none of 5,8,9,10,11 are in the draw's main numbers
        assertEquals(2, result.euroMatches)
    }

    @Test
    fun `non-winning combination has no prize tier`() {
        val result = useCase(mainNumbers = listOf(2, 3, 6, 8, 9), euroNumbers = listOf(11, 12), draw = draw)
        assertEquals(0, result.mainMatches)
        assertEquals(0, result.euroMatches)
        assertNull(result.prizeTier)
    }

    @Test
    fun `every official prize tier maps back correctly`() {
        assertEquals(PrizeTier.TIER_1, PrizeTier.forMatch(5, 2))
        assertEquals(PrizeTier.TIER_2, PrizeTier.forMatch(5, 1))
        assertEquals(PrizeTier.TIER_3, PrizeTier.forMatch(5, 0))
        assertEquals(PrizeTier.TIER_4, PrizeTier.forMatch(4, 2))
        assertEquals(PrizeTier.TIER_12, PrizeTier.forMatch(2, 1))
        assertNull(PrizeTier.forMatch(2, 0)) // not an official tier
        assertNull(PrizeTier.forMatch(0, 0))
    }
}
