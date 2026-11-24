package com.eurojackpotai.app.domain.model

/** One line of the "why these numbers?" explanation shown when a generated number is tapped. */
data class NumberExplanation(
    val number: Int,
    val overallFrequency: Int,
    val recent30Count: Int,
    val drawsSinceLastSeen: Int
)

data class GeneratedCombination(
    val mainNumbers: List<Int>,
    val euroNumbers: List<Int>,
    val mode: GenerationMode,
    val explanations: List<NumberExplanation>
)
