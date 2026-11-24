package com.eurojackpotai.app.domain.model

/** Per-number statistics used by both the Statistics screen and the generators. */
data class NumberFrequency(
    val number: Int,
    val totalCount: Int,
    val last10Count: Int,
    val last30Count: Int,
    val last100Count: Int,
    val drawsSinceLastSeen: Int
)

data class NumberPairFrequency(
    val first: Int,
    val second: Int,
    val coOccurrenceCount: Int
)

data class OddEvenSplit(
    val oddCount: Int,
    val evenCount: Int,
    val occurrences: Int
)

data class RangeBucket(
    val range: IntRange,
    val count: Int
)

data class SumBucket(
    val range: IntRange,
    val count: Int
)

/** Full snapshot the Statistics screen renders in one pass. */
data class LotteryStatistics(
    val drawsAnalyzed: Int,
    val mainNumberFrequencies: List<NumberFrequency>,
    val euroNumberFrequencies: List<NumberFrequency>,
    val topPairs: List<NumberPairFrequency>,
    val oddEvenSplits: List<OddEvenSplit>,
    val rangeDistribution: List<RangeBucket>,
    val sumDistribution: List<SumBucket>
) {
    val hotMainNumbers: List<NumberFrequency>
        get() = mainNumberFrequencies.sortedWith(
            compareByDescending<NumberFrequency> { it.last30Count }.thenByDescending { it.totalCount }
        )
    val coldMainNumbers: List<NumberFrequency>
        get() = mainNumberFrequencies.sortedWith(
            compareBy<NumberFrequency> { it.last30Count }.thenBy { it.totalCount }
        )
}
