package com.eurojackpotai.app.domain.usecase

import com.eurojackpotai.app.domain.model.GeneratedCombination
import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.LotteryRules
import com.eurojackpotai.app.domain.model.LotteryStatistics
import kotlin.random.Random

/**
 * Phase 4 (product spec section 8): Smart Coverage. Instead of calling Random N
 * independent times, this:
 *
 * 1. Builds a pool of candidate combinations, keeping only "well-formed" ones — no
 *    extreme odd/even split, no long run of consecutive numbers, a sum near the middle
 *    of the theoretical range, and main numbers spread across at least 3 of the five
 *    1-10/11-20/.../41-50 buckets.
 * 2. Greedily selects [count] of those candidates so that, as a set, they reuse the
 *    same main/Euro numbers as little as possible — maximizing how many distinct
 *    numbers the whole batch of games covers, and minimizing unnecessary overlap
 *    between the user's own games.
 *
 * This does not, and must never be described as, increasing any single game's odds of
 * winning (see generation_disclaimer / mode_smart_coverage_desc in strings.xml) — every
 * candidate combination has identical winning odds to every other. What changes is only
 * which combinations, among equally-likely ones, get suggested together.
 *
 * [statistics] is accepted for a consistent call signature with the other generators
 * but intentionally unused: coverage is a property of the numbers themselves (their
 * spread, not their draw history), so weighting by past frequency here would just be
 * Statistical Mix repeated N times, not a genuinely different mode.
 */
object SmartCoverageGenerator {

    fun generate(
        count: Int,
        @Suppress("UNUSED_PARAMETER") statistics: LotteryStatistics?,
        random: Random = Random.Default
    ): List<GeneratedCombination> {
        if (count <= 0) return emptyList()

        val poolSize = (count * 15).coerceAtLeast(200)
        val candidates = buildCandidatePool(poolSize, random)
        val selected = selectDiverseSet(candidates, count, random)

        return selected.map { (main, euro) ->
            GeneratedCombination(
                mainNumbers = main.sorted(),
                euroNumbers = euro.sorted(),
                mode = GenerationMode.SMART_COVERAGE,
                // Coverage is a property of the whole batch, not of any one number, so
                // there's no single-number "why" to show — the explain-on-tap UI is
                // skipped for this mode (see GenerateScreen.CombinationCard).
                explanations = emptyList()
            )
        }
    }

    private fun buildCandidatePool(poolSize: Int, random: Random): List<Pair<List<Int>, List<Int>>> {
        val seenMainSets = HashSet<List<Int>>()
        val candidates = mutableListOf<Pair<List<Int>, List<Int>>>()
        var attempts = 0
        val maxAttempts = poolSize * 20 // generous cap so a pathological run can't spin forever

        while (candidates.size < poolSize && attempts < maxAttempts) {
            attempts++
            val main = randomDistinct(random, LotteryRules.MAIN_NUMBER_RANGE, LotteryRules.MAIN_NUMBERS_COUNT).sorted()
            if (!CombinationHeuristics.isWellFormedMainSet(main)) continue
            if (!seenMainSets.add(main)) continue // reject exact duplicate combinations outright

            val euro = randomDistinct(random, LotteryRules.EURO_NUMBER_RANGE, LotteryRules.EURO_NUMBERS_COUNT)
            candidates += main to euro
        }
        return candidates
    }

    /**
     * Greedily builds the final [count]-game set: at every step it picks whichever
     * remaining candidate would add the least overlap with numbers already used by
     * previously-selected games (tracked in [usage]), which is exactly what maximizes
     * coverage across the whole set rather than any single game.
     */
    private fun selectDiverseSet(
        candidates: List<Pair<List<Int>, List<Int>>>,
        count: Int,
        random: Random
    ): List<Pair<List<Int>, List<Int>>> {
        val pool = candidates.shuffled(random).toMutableList()
        val selected = mutableListOf<Pair<List<Int>, List<Int>>>()
        val mainUsage = HashMap<Int, Int>()
        val euroUsage = HashMap<Int, Int>()

        while (selected.size < count && pool.isNotEmpty()) {
            var bestIndex = 0
            var bestPenalty = Int.MAX_VALUE
            for (i in pool.indices) {
                val (main, euro) = pool[i]
                val penalty = main.sumOf { mainUsage[it] ?: 0 } + euro.sumOf { euroUsage[it] ?: 0 }
                if (penalty < bestPenalty) {
                    bestPenalty = penalty
                    bestIndex = i
                }
            }
            val chosen = pool.removeAt(bestIndex)
            selected += chosen
            chosen.first.forEach { mainUsage[it] = (mainUsage[it] ?: 0) + 1 }
            chosen.second.forEach { euroUsage[it] = (euroUsage[it] ?: 0) + 1 }
        }

        // Only reachable if the well-formed pool is smaller than `count` itself (a very
        // large request relative to the filter's acceptance rate) — top up with plain
        // random combinations rather than silently returning fewer games than asked for.
        while (selected.size < count) {
            val main = randomDistinct(random, LotteryRules.MAIN_NUMBER_RANGE, LotteryRules.MAIN_NUMBERS_COUNT).sorted()
            val euro = randomDistinct(random, LotteryRules.EURO_NUMBER_RANGE, LotteryRules.EURO_NUMBERS_COUNT)
            selected += main to euro
        }

        return selected
    }

    private fun randomDistinct(random: Random, range: IntRange, count: Int): List<Int> {
        val pool = range.toMutableList()
        val picked = mutableListOf<Int>()
        repeat(count) { picked += pool.removeAt(random.nextInt(pool.size)) }
        return picked
    }
}
