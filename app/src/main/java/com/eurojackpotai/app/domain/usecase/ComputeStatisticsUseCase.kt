package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.LotteryRules
import com.eurojackpotai.app.domain.model.LotteryStatistics
import com.eurojackpotai.app.domain.model.NumberFrequency
import com.eurojackpotai.app.domain.model.NumberPairFrequency
import com.eurojackpotai.app.domain.model.OddEvenSplit
import com.eurojackpotai.app.domain.model.RangeBucket
import com.eurojackpotai.app.domain.model.SumBucket

/**
 * Pure function over already-loaded draw history — no I/O, so it's trivial to unit test
 * and safe to call from a Compose-side ViewModel without worrying about threading beyond
 * "don't do it on the main thread for large histories," which callers handle with
 * `Dispatchers.Default`.
 */
class ComputeStatisticsUseCase {

    operator fun invoke(draws: List<DrawResult>): LotteryStatistics {
        // Expected newest-first; sort defensively so callers can pass any order.
        val sorted = draws.sortedByDescending { it.date }

        return LotteryStatistics(
            drawsAnalyzed = sorted.size,
            mainNumberFrequencies = frequenciesFor(sorted, LotteryRules.MAIN_NUMBER_RANGE) { it.mainNumbers },
            euroNumberFrequencies = frequenciesFor(sorted, LotteryRules.EURO_NUMBER_RANGE) { it.euroNumbers },
            topPairs = topPairs(sorted),
            oddEvenSplits = oddEvenSplits(sorted),
            rangeDistribution = rangeDistribution(sorted),
            sumDistribution = sumDistribution(sorted)
        )
    }

    private fun frequenciesFor(
        sorted: List<DrawResult>,
        range: IntRange,
        numbersOf: (DrawResult) -> List<Int>
    ): List<NumberFrequency> {
        val last10 = sorted.take(10)
        val last30 = sorted.take(30)
        val last100 = sorted.take(100)

        return range.map { number ->
            val appearances = sorted.indexOfFirst { number in numbersOf(it) }
            NumberFrequency(
                number = number,
                totalCount = sorted.count { number in numbersOf(it) },
                last10Count = last10.count { number in numbersOf(it) },
                last30Count = last30.count { number in numbersOf(it) },
                last100Count = last100.count { number in numbersOf(it) },
                drawsSinceLastSeen = if (appearances == -1) sorted.size else appearances
            )
        }
    }

    private fun topPairs(sorted: List<DrawResult>, limit: Int = 15): List<NumberPairFrequency> {
        val counts = HashMap<Pair<Int, Int>, Int>()
        for (draw in sorted) {
            val nums = draw.mainNumbers.sorted()
            for (i in nums.indices) {
                for (j in i + 1 until nums.size) {
                    val key = nums[i] to nums[j]
                    counts[key] = (counts[key] ?: 0) + 1
                }
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { (pair, count) -> NumberPairFrequency(pair.first, pair.second, count) }
    }

    private fun oddEvenSplits(sorted: List<DrawResult>): List<OddEvenSplit> {
        val tally = HashMap<Int, Int>() // key: oddCount (0..5)
        for (draw in sorted) {
            val oddCount = draw.mainNumbers.count { it % 2 != 0 }
            tally[oddCount] = (tally[oddCount] ?: 0) + 1
        }
        return (0..LotteryRules.MAIN_NUMBERS_COUNT).map { oddCount ->
            OddEvenSplit(
                oddCount = oddCount,
                evenCount = LotteryRules.MAIN_NUMBERS_COUNT - oddCount,
                occurrences = tally[oddCount] ?: 0
            )
        }
    }

    private fun rangeDistribution(sorted: List<DrawResult>): List<RangeBucket> {
        val buckets = listOf(1..10, 11..20, 21..30, 31..40, 41..50)
        return buckets.map { bucket ->
            RangeBucket(
                range = bucket,
                count = sorted.sumOf { draw -> draw.mainNumbers.count { it in bucket } }
            )
        }
    }

    private fun sumDistribution(sorted: List<DrawResult>): List<SumBucket> {
        // Theoretical sum range for 5 distinct numbers in 1..50 is 15..240; bucketed in
        // steps of 20 so the chart has a manageable ~12 bars.
        val bucketWidth = 20
        val minSum = 15
        val maxSum = 240
        val buckets = (minSum..maxSum step bucketWidth).map { start ->
            start until (start + bucketWidth)
        }
        return buckets.map { bucket ->
            SumBucket(range = bucket, count = sorted.count { it.sum in bucket })
        }
    }
}
