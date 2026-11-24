package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.GeneratedCombination
import com.eurojackpotai.app.domain.model.LotteryRules
import com.eurojackpotai.app.domain.model.LotteryStatistics
import com.eurojackpotai.app.domain.model.NumberExplanation
import com.eurojackpotai.app.domain.model.NumberFrequency
import com.eurojackpotai.app.domain.model.WeightIntensity
import kotlin.random.Random

/**
 * Produces one or more combinations for a given [GenerationMode].
 *
 * IMPORTANT (product requirement, not just a comment): none of these modes make a
 * combination more likely to win. Every valid EuroJackpot combination has identical
 * odds; what differs here is only *which* numbers get proposed and why, which is
 * surfaced to the user via [GeneratedCombination.explanations] and the disclaimer
 * strings shown next to each mode in the UI (see strings.xml: generation_disclaimer,
 * hot_disclaimer, cold_disclaimer, avoid_popular_disclaimer).
 *
 * [hotColdIntensity] is a user-adjustable multiplier (see [WeightIntensity]) applied
 * only to the Hot / Cold / Hot+Cold statistical bias — it lets the user dial that bias
 * up or down without changing anything about how any other mode works.
 */
class GenerateNumbersUseCase {

    operator fun invoke(
        mode: GenerationMode,
        statistics: LotteryStatistics?,
        count: Int = 1,
        random: Random = Random.Default,
        hotColdIntensity: WeightIntensity = WeightIntensity.NORMAL
    ): List<GeneratedCombination> {
        require(mode.isAvailable) { "$mode is not implemented yet" }

        // Smart Coverage optimizes the *set* of `count` games together (minimizing
        // overlap between them), so it can't be built by calling generateOne() in a
        // loop like every other mode — each of those picks one combination in
        // isolation, with no memory of what the previous ones already used.
        if (mode == GenerationMode.SMART_COVERAGE) {
            return SmartCoverageGenerator.generate(count = count, statistics = statistics, random = random)
        }

        return List(count) { generateOne(mode, statistics, random, hotColdIntensity) }
    }

    private fun generateOne(
        mode: GenerationMode,
        statistics: LotteryStatistics?,
        random: Random,
        hotColdIntensity: WeightIntensity
    ): GeneratedCombination {
        val (main, euro) = when (mode) {
            GenerationMode.RANDOM -> randomPick(random)
            GenerationMode.BALANCED -> balancedPick(random)
            GenerationMode.HOT -> weightedPick(statistics, random) { freq, draws -> hotWeight(freq, draws, hotColdIntensity) }
            GenerationMode.COLD -> weightedPick(statistics, random) { freq, draws -> coldWeight(freq, draws, hotColdIntensity) }
            GenerationMode.HOT_COLD -> hotColdPick(statistics, random, hotColdIntensity)
            GenerationMode.STATISTICAL_MIX -> weightedPick(statistics, random, ::statisticalWeight)
            GenerationMode.AI_MIX -> aiMixPick(statistics, random)
            GenerationMode.AVOID_POPULAR -> avoidPopularPick(random)
            GenerationMode.SMART_COVERAGE ->
                error("Smart Coverage is generated as a batch by SmartCoverageGenerator, not per-combination")
        }

        val explanations = if (statistics == null) {
            emptyList()
        } else {
            explain(main, statistics.mainNumberFrequencies) + explain(euro, statistics.euroNumberFrequencies)
        }

        return GeneratedCombination(
            mainNumbers = main.sorted(),
            euroNumbers = euro.sorted(),
            mode = mode,
            explanations = explanations
        )
    }

    // ---- Individual modes -------------------------------------------------

    private fun randomPick(random: Random): Pair<List<Int>, List<Int>> = Pair(
        randomDistinct(random, LotteryRules.MAIN_NUMBER_RANGE, LotteryRules.MAIN_NUMBERS_COUNT),
        randomDistinct(random, LotteryRules.EURO_NUMBER_RANGE, LotteryRules.EURO_NUMBERS_COUNT)
    )

    /** One number from each of the five 1-10/11-20/.../41-50 buckets, plus one Euro
     * number from each half of the 1-12 range — spreads the combination out without
     * ever looking at historical frequency. */
    private fun balancedPick(random: Random): Pair<List<Int>, List<Int>> {
        val mainBuckets = listOf(1..10, 11..20, 21..30, 31..40, 41..50)
        val main = mainBuckets.map { it.random(random) }.toMutableList()
        deduplicateWithinRange(main, LotteryRules.MAIN_NUMBER_RANGE, random)

        val euroBuckets = listOf(1..6, 7..12)
        val euro = euroBuckets.map { it.random(random) }.toMutableList()
        deduplicateWithinRange(euro, LotteryRules.EURO_NUMBER_RANGE, random)

        return main to euro
    }

    private fun weightedPick(
        statistics: LotteryStatistics?,
        random: Random,
        weightOf: (NumberFrequency, Int) -> Double
    ): Pair<List<Int>, List<Int>> {
        if (statistics == null) return randomPick(random)

        val main = weightedSample(
            statistics.mainNumberFrequencies, LotteryRules.MAIN_NUMBERS_COUNT, statistics.drawsAnalyzed, random, weightOf
        )
        val euro = weightedSample(
            statistics.euroNumberFrequencies, LotteryRules.EURO_NUMBERS_COUNT, statistics.drawsAnalyzed, random, weightOf
        )
        return main to euro
    }

    private fun hotColdPick(
        statistics: LotteryStatistics?,
        random: Random,
        intensity: WeightIntensity
    ): Pair<List<Int>, List<Int>> {
        if (statistics == null) return randomPick(random)

        val hotMain = weightedSample(statistics.mainNumberFrequencies, 3, statistics.drawsAnalyzed, random) { freq, draws ->
            hotWeight(freq, draws, intensity)
        }
        val remainingMainPool = statistics.mainNumberFrequencies.filterNot { it.number in hotMain }
        val coldMain = weightedSample(remainingMainPool, 2, statistics.drawsAnalyzed, random) { freq, draws ->
            coldWeight(freq, draws, intensity)
        }

        val hotEuro = weightedSample(statistics.euroNumberFrequencies, 1, statistics.drawsAnalyzed, random) { freq, draws ->
            hotWeight(freq, draws, intensity)
        }
        val remainingEuroPool = statistics.euroNumberFrequencies.filterNot { it.number in hotEuro }
        val coldEuro = weightedSample(remainingEuroPool, 1, statistics.drawsAnalyzed, random) { freq, draws ->
            coldWeight(freq, draws, intensity)
        }

        return (hotMain + coldMain) to (hotEuro + coldEuro)
    }

    /**
     * Avoid Popular Picks: biases main numbers toward the 32-50 range. That range can
     * never represent a calendar day, so players hand-picking "meaningful" numbers —
     * birthdays, anniversaries — draw almost entirely from 1-31 instead, which makes
     * 32-50 under-represented among the combinations other people actually choose.
     * [CombinationHeuristics.isUnlikelyPopularPick] then rejects any sampled set that
     * still reads as a hand-picked, popular combination (all-calendar-day, or a simple
     * arithmetic sequence).
     *
     * Euro numbers (1-12) are left plain random: that whole range can represent a birth
     * month, so there's no "safe", less-popular sub-range to favor there the way there
     * is for main numbers.
     *
     * This is independent of [LotteryStatistics] — draw history describes what the
     * lottery has drawn, not what other players pick — and, like every mode here, it
     * never changes any combination's actual odds of winning. It can only affect how
     * likely you are to split a jackpot with someone who picked the same "intuitive"
     * numbers (see avoid_popular_disclaimer in strings.xml).
     */
    private fun avoidPopularPick(random: Random): Pair<List<Int>, List<Int>> {
        val mainPool = LotteryRules.MAIN_NUMBER_RANGE.toList()
        val mainWeights = mainPool.map(::birthdayAvoidanceWeight)

        val maxAttempts = 40
        var main = emptyList<Int>()
        for (attempt in 1..maxAttempts) {
            main = WeightedSampler.sampleWithoutReplacement(
                items = mainPool,
                weights = mainWeights,
                count = LotteryRules.MAIN_NUMBERS_COUNT,
                random = random
            ).sorted()
            if (CombinationHeuristics.isUnlikelyPopularPick(main)) break
        }
        // If every attempt above failed (only possible with a pathologically narrow
        // range), `main` still holds the last weighted attempt — a valid combination,
        // just not guaranteed to pass the popularity check.

        val euro = randomDistinct(random, LotteryRules.EURO_NUMBER_RANGE, LotteryRules.EURO_NUMBERS_COUNT)
        return main to euro
    }

    /**
     * AI Mix: the "fuller weighted-scoring engine" the product spec calls for beyond
     * Statistical Mix (see GenerationMode's KDoc). Two things make it different from
     * plain [statisticalWeight]-based [weightedPick]:
     *
     * 1. [aiMixWeight] blends all three existing single-factor weights — hot, cold, and
     *    statistical — into one composite score, so a number that's both historically
     *    frequent AND currently "due" (long gap since it was last drawn) scores higher
     *    than any one signal alone would give it.
     * 2. The main-number set is drawn by rejection sampling against
     *    [CombinationHeuristics] — the same structural sanity check (odd/even balance,
     *    sum range, spread across the 1-50 range, no long consecutive run)
     *    [SmartCoverageGenerator] applies to its whole candidate pool — so a weighted
     *    pick that happens to land on an oddly-shaped combination gets re-rolled instead
     *    of returned as-is.
     *
     * "AI" here means rule-based multi-factor weighting, not machine learning — like
     * every other mode in this file, it does not predict outcomes or change any
     * combination's odds of winning (see ai_mix_disclaimer in strings.xml).
     */
    private fun aiMixPick(statistics: LotteryStatistics?, random: Random): Pair<List<Int>, List<Int>> {
        if (statistics == null) return randomPick(random)

        val euro = weightedSample(
            statistics.euroNumberFrequencies, LotteryRules.EURO_NUMBERS_COUNT, statistics.drawsAnalyzed, random, ::aiMixWeight
        )

        val maxAttempts = 40
        var main = emptyList<Int>()
        for (attempt in 1..maxAttempts) {
            main = weightedSample(
                statistics.mainNumberFrequencies, LotteryRules.MAIN_NUMBERS_COUNT, statistics.drawsAnalyzed, random, ::aiMixWeight
            ).sorted()
            if (CombinationHeuristics.isWellFormedMainSet(main)) break
        }
        // If every attempt above failed to land on a well-formed set (only possible with a
        // very unusual frequency distribution), `main` still holds the last weighted
        // attempt — a valid, still-weighted combination, just not guaranteed well-formed.
        return main to euro
    }

    private fun aiMixWeight(freq: NumberFrequency, drawsAnalyzed: Int): Double =
        hotWeight(freq, drawsAnalyzed, WeightIntensity.NORMAL) +
            coldWeight(freq, drawsAnalyzed, WeightIntensity.NORMAL) +
            statisticalWeight(freq, drawsAnalyzed)

    private fun weightedSample(
        frequencies: List<NumberFrequency>,
        count: Int,
        drawsAnalyzed: Int,
        random: Random,
        weightOf: (NumberFrequency, Int) -> Double
    ): List<Int> {
        val weights = frequencies.map { weightOf(it, drawsAnalyzed) }
        return WeightedSampler.sampleWithoutReplacement(
            items = frequencies.map { it.number },
            weights = weights,
            count = count,
            random = random
        )
    }

    // ---- Weight functions ---------------------------------------------------
    // All weights are strictly positive so every number always keeps a non-zero chance
    // of being picked — "hot" and "cold" bias the odds, they never eliminate a number.

    /**
     * [intensity] scales only the frequency-based bias term, never the fixed `1.0`
     * baseline — so even at [WeightIntensity.WEAK] every number keeps a meaningful
     * chance, and [WeightIntensity.NORMAL] reproduces the original fixed weighting
     * exactly (multiplier 1.0).
     */
    private fun hotWeight(freq: NumberFrequency, @Suppress("UNUSED_PARAMETER") drawsAnalyzed: Int, intensity: WeightIntensity): Double =
        1.0 + (freq.totalCount * 1.0 + freq.last30Count * 2.0) * intensity.multiplier

    private fun coldWeight(freq: NumberFrequency, drawsAnalyzed: Int, intensity: WeightIntensity): Double {
        val maxPossible = drawsAnalyzed.coerceAtLeast(1)
        return 1.0 + ((maxPossible - freq.totalCount) * 1.0 + freq.drawsSinceLastSeen * 0.5) * intensity.multiplier
    }

    private fun statisticalWeight(freq: NumberFrequency, drawsAnalyzed: Int): Double =
        1.0 + freq.last30Count * 1.5 + freq.totalCount * 0.5 + freq.drawsSinceLastSeen * 0.3

    /**
     * Numbers above 31 can't be a day-of-month, so they're favored here for
     * [avoidPopularPick] — see that function's doc for why. Still strictly positive for
     * every number, same as every other weight function in this file.
     */
    private fun birthdayAvoidanceWeight(number: Int): Double = if (number > 31) 2.5 else 1.0

    // ---- Helpers --------------------------------------------------------

    private fun randomDistinct(random: Random, range: IntRange, count: Int): List<Int> {
        val pool = range.toMutableList()
        val picked = mutableListOf<Int>()
        repeat(count) { picked += pool.removeAt(random.nextInt(pool.size)) }
        return picked
    }

    private fun deduplicateWithinRange(numbers: MutableList<Int>, range: IntRange, random: Random) {
        val seen = HashSet<Int>()
        for (i in numbers.indices) {
            var candidate = numbers[i]
            while (!seen.add(candidate)) {
                candidate = range.random(random)
            }
            numbers[i] = candidate
        }
    }

    private fun explain(numbers: List<Int>, frequencies: List<NumberFrequency>): List<NumberExplanation> =
        numbers.mapNotNull { number ->
            frequencies.find { it.number == number }?.let {
                NumberExplanation(
                    number = number,
                    overallFrequency = it.totalCount,
                    recent30Count = it.last30Count,
                    drawsSinceLastSeen = it.drawsSinceLastSeen
                )
            }
        }
}
