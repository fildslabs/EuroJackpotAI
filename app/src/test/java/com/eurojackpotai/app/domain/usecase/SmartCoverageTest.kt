package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.LotteryRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Covers product spec section 36 (SmartCoverageTest): no duplicate combinations, the
 * requested number of games is produced exactly, and every produced number is valid. */
class SmartCoverageTest {

    private val useCase = GenerateNumbersUseCase()

    @Test
    fun `requested game count is produced exactly`() {
        listOf(1, 5, 10, 20, 50).forEach { requested ->
            val combinations = useCase(
                mode = GenerationMode.SMART_COVERAGE,
                statistics = null,
                count = requested,
                random = Random(requested)
            )
            assertEquals("count for $requested games", requested, combinations.size)
        }
    }

    @Test
    fun `no two generated combinations are identical`() {
        val combinations = useCase(
            mode = GenerationMode.SMART_COVERAGE,
            statistics = null,
            count = 50,
            random = Random(11)
        )
        val distinctMainSets = combinations.map { it.mainNumbers.sorted() }.toSet()
        assertEquals("duplicate main-number combinations were produced", 50, distinctMainSets.size)
    }

    @Test
    fun `every generated number is within range with no in-combination duplicates`() {
        val combinations = useCase(
            mode = GenerationMode.SMART_COVERAGE,
            statistics = null,
            count = 30,
            random = Random(5)
        )
        combinations.forEach { combination ->
            assertEquals(LotteryRules.MAIN_NUMBERS_COUNT, combination.mainNumbers.size)
            assertEquals(LotteryRules.EURO_NUMBERS_COUNT, combination.euroNumbers.size)
            assertTrue(combination.mainNumbers.all { it in LotteryRules.MAIN_NUMBER_RANGE })
            assertTrue(combination.euroNumbers.all { it in LotteryRules.EURO_NUMBER_RANGE })
            assertEquals(combination.mainNumbers.size, combination.mainNumbers.toSet().size)
            assertEquals(combination.euroNumbers.size, combination.euroNumbers.toSet().size)
        }
    }

    @Test
    fun `ten games cover most of the 1-50 range instead of clustering`() {
        // 10 games x 5 main numbers = 50 number-slots, exactly the size of the pool —
        // the greedy de-duplication in selectDiverseSet should be able to keep the
        // large majority of picks disjoint from ones already used, so coverage should
        // land well above what clustering on a handful of numbers would produce.
        val combinations = useCase(
            mode = GenerationMode.SMART_COVERAGE,
            statistics = null,
            count = 10,
            random = Random(99)
        )
        val distinctMainNumbersUsed = combinations.flatMap { it.mainNumbers }.toSet().size
        assertTrue(
            "expected broad coverage across 10 games, only touched $distinctMainNumbersUsed distinct main numbers",
            distinctMainNumbersUsed >= 35
        )
    }

    @Test
    fun `requesting zero games returns an empty list without error`() {
        val combinations = useCase(mode = GenerationMode.SMART_COVERAGE, statistics = null, count = 0)
        assertTrue(combinations.isEmpty())
    }
}
