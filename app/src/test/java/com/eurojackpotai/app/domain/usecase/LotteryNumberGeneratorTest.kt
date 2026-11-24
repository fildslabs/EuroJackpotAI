package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.LotteryRules
import com.eurojackpotai.app.domain.model.LotteryStatistics
import com.eurojackpotai.app.domain.model.NumberFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Covers the exact checks the product spec calls out in section 36
 * (LotteryNumberGeneratorTest): main numbers are 1..50, exactly 5, no duplicates;
 * Euro numbers are 1..12, exactly 2, no duplicates — for every implemented mode.
 */
class LotteryNumberGeneratorTest {

    private val useCase = GenerateNumbersUseCase()
    private val availableModes = GenerationMode.entries.filter { it.isAvailable }

    private fun fakeStatistics(drawsAnalyzed: Int = 50): LotteryStatistics {
        val random = Random(42)
        val mainFreq = LotteryRules.MAIN_NUMBER_RANGE.map {
            NumberFrequency(
                number = it,
                totalCount = random.nextInt(0, drawsAnalyzed),
                last10Count = random.nextInt(0, 5),
                last30Count = random.nextInt(0, 15),
                last100Count = random.nextInt(0, drawsAnalyzed),
                drawsSinceLastSeen = random.nextInt(0, drawsAnalyzed)
            )
        }
        val euroFreq = LotteryRules.EURO_NUMBER_RANGE.map {
            NumberFrequency(
                number = it,
                totalCount = random.nextInt(0, drawsAnalyzed),
                last10Count = random.nextInt(0, 5),
                last30Count = random.nextInt(0, 15),
                last100Count = random.nextInt(0, drawsAnalyzed),
                drawsSinceLastSeen = random.nextInt(0, drawsAnalyzed)
            )
        }
        return LotteryStatistics(
            drawsAnalyzed = drawsAnalyzed,
            mainNumberFrequencies = mainFreq,
            euroNumberFrequencies = euroFreq,
            topPairs = emptyList(),
            oddEvenSplits = emptyList(),
            rangeDistribution = emptyList(),
            sumDistribution = emptyList()
        )
    }

    @Test
    fun `every available mode produces valid combinations`() {
        val stats = fakeStatistics()
        availableModes.forEach { mode ->
            repeat(20) {
                val combination = useCase(mode = mode, statistics = stats, count = 1, random = Random(it)).single()

                assertEquals("main size for $mode", LotteryRules.MAIN_NUMBERS_COUNT, combination.mainNumbers.size)
                assertEquals("euro size for $mode", LotteryRules.EURO_NUMBERS_COUNT, combination.euroNumbers.size)

                assertTrue("main range for $mode", combination.mainNumbers.all { n -> n in LotteryRules.MAIN_NUMBER_RANGE })
                assertTrue("euro range for $mode", combination.euroNumbers.all { n -> n in LotteryRules.EURO_NUMBER_RANGE })

                assertEquals("main duplicates for $mode", combination.mainNumbers.size, combination.mainNumbers.toSet().size)
                assertEquals("euro duplicates for $mode", combination.euroNumbers.size, combination.euroNumbers.toSet().size)
            }
        }
    }

    @Test
    fun `random and balanced modes work without any statistics`() {
        listOf(GenerationMode.RANDOM, GenerationMode.BALANCED).forEach { mode ->
            val combination = useCase(mode = mode, statistics = null, count = 1, random = Random(7)).single()
            assertEquals(LotteryRules.MAIN_NUMBERS_COUNT, combination.mainNumbers.size)
            assertEquals(LotteryRules.EURO_NUMBERS_COUNT, combination.euroNumbers.size)
        }
    }

    @Test
    fun `requested game count is produced exactly`() {
        val combinations = useCase(mode = GenerationMode.RANDOM, statistics = null, count = 20, random = Random(1))
        assertEquals(20, combinations.size)
    }

    @Test
    fun `unavailable modes are rejected`() {
        var threw = false
        try {
            useCase(mode = GenerationMode.AI_MIX, statistics = null, count = 1)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("AI Mix should not be generatable yet", threw)
    }

    @Test
    fun `balanced mode spreads numbers across the five range buckets`() {
        val combination = useCase(mode = GenerationMode.BALANCED, statistics = null, count = 1, random = Random(3)).single()
        val buckets = listOf(1..10, 11..20, 21..30, 31..40, 41..50)
        val bucketsHit = buckets.count { bucket -> combination.mainNumbers.any { it in bucket } }
        assertEquals(5, bucketsHit)
    }

    @Test
    fun `explanations are only produced when statistics are available`() {
        val withStats = useCase(mode = GenerationMode.HOT, statistics = fakeStatistics(), count = 1).single()
        assertFalse(withStats.explanations.isEmpty())

        val withoutStats = useCase(mode = GenerationMode.RANDOM, statistics = null, count = 1).single()
        assertTrue(withoutStats.explanations.isEmpty())
    }
}
