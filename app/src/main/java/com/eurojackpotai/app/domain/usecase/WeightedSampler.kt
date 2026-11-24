package com.eurojackpotai.app.domain.usecase

import kotlin.math.ln
import kotlin.random.Random

/**
 * Weighted sampling without replacement using the Efraimidis-Spirakis algorithm: each
 * item gets a random key of `u^(1/weight)` (computed in log space for stability), and
 * the top-N keys are the sample. Falls back gracefully to uniform sampling for a
 * zero/negative weight by flooring it to a tiny positive value, since a generation mode
 * should never crash just because one number currently has zero recorded appearances.
 */
object WeightedSampler {

    fun <T> sampleWithoutReplacement(
        items: List<T>,
        weights: List<Double>,
        count: Int,
        random: Random
    ): List<T> {
        require(items.size == weights.size)
        require(count <= items.size)

        val keyed = items.indices.map { i ->
            val weight = weights[i].coerceAtLeast(1e-6)
            val u = random.nextDouble().coerceIn(1e-9, 1.0)
            val key = ln(u) / weight // higher weight -> key closer to 0 -> ranks higher
            key to items[i]
        }
        return keyed.sortedByDescending { it.first }.take(count).map { it.second }
    }
}
