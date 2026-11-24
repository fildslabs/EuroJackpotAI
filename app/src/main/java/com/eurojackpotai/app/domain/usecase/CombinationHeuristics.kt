package com.eurojackpotai.app.domain.usecase

/**
 * Shared "does this look like a sensible spread of numbers" filter, used by any generation
 * mode that wants to avoid handing back oddly-shaped combinations (all bunched in one
 * decade, a long run of consecutive numbers, or an extreme odd/even split) — regardless of
 * *how* the candidate numbers were picked: uniformly at random for [SmartCoverageGenerator],
 * or weighted by statistics for AI Mix in [GenerateNumbersUseCase].
 *
 * This is a structural check only, applied after the numbers are already valid (5 distinct
 * main numbers in range). It never changes any combination's odds of winning — every
 * combination that passes here has identical odds to one that doesn't (see
 * generation_disclaimer in strings.xml).
 */
object CombinationHeuristics {

    private val RANGE_BUCKETS = listOf(1..10, 11..20, 21..30, 31..40, 41..50)
    private const val MIN_SUM = 70
    private const val MAX_SUM = 190 // theoretical range for 5 distinct numbers in 1..50 is 15..240
    private const val MIN_BUCKETS_HIT = 3
    private const val MAX_CONSECUTIVE_RUN = 2
    private const val MIN_ABOVE_CALENDAR_DAY_COUNT = 2 // see isUnlikelyPopularPick

    fun isWellFormedMainSet(sortedMain: List<Int>): Boolean {
        val oddCount = sortedMain.count { it % 2 != 0 }
        if (oddCount !in 1..4) return false // reject the 5/0 and 0/5 odd/even splits

        val sum = sortedMain.sum()
        if (sum !in MIN_SUM..MAX_SUM) return false

        val bucketsHit = RANGE_BUCKETS.count { bucket -> sortedMain.any { it in bucket } }
        if (bucketsHit < MIN_BUCKETS_HIT) return false

        if (longestConsecutiveRun(sortedMain) > MAX_CONSECUTIVE_RUN) return false

        return true
    }

    /**
     * Used by [GenerateNumbersUseCase.avoidPopularPick]: rejects main-number sets that
     * still read as a hand-picked, "popular" combination even after the weighting in
     * that function already favors numbers above 31. Two extra conditions beyond
     * [isWellFormedMainSet]:
     *
     * 1. At least [MIN_ABOVE_CALENDAR_DAY_COUNT] numbers must be above 31 — a set drawn
     *    entirely from 1-31 reads exactly like a set of birthdays/anniversaries, which is
     *    the single most common way players hand-pick numbers.
     * 2. The set can't be a perfect arithmetic sequence (5, 10, 15, 20, 25) — a simple,
     *    memorable pattern many players choose on purpose.
     *
     * Like every other check in this object, this never changes any combination's actual
     * odds of winning — it only changes which equally-likely combinations get suggested
     * (see avoid_popular_disclaimer in strings.xml).
     */
    fun isUnlikelyPopularPick(sortedMain: List<Int>): Boolean {
        if (!isWellFormedMainSet(sortedMain)) return false
        if (sortedMain.count { it > 31 } < MIN_ABOVE_CALENDAR_DAY_COUNT) return false
        if (isArithmeticSequence(sortedMain)) return false
        return true
    }

    private fun isArithmeticSequence(sortedMain: List<Int>): Boolean {
        val diff = sortedMain[1] - sortedMain[0]
        if (diff <= 0) return false
        return (2 until sortedMain.size).all { sortedMain[it] - sortedMain[it - 1] == diff }
    }

    private fun longestConsecutiveRun(sortedMain: List<Int>): Int {
        var longest = 1
        var current = 1
        for (i in 1 until sortedMain.size) {
            if (sortedMain[i] == sortedMain[i - 1] + 1) {
                current++
                longest = maxOf(longest, current)
            } else {
                current = 1
            }
        }
        return longest
    }
}
