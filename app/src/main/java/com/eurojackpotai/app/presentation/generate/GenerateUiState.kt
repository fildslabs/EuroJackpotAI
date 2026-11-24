package com.eurojackpotai.app.presentation.generate

import com.eurojackpotai.app.domain.model.GeneratedCombination
import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.NumberExplanation
import com.eurojackpotai.app.domain.model.WeightIntensity

data class GenerateUiState(
    val selectedMode: GenerationMode = GenerationMode.RANDOM,
    val selectedGameCount: Int = 1,
    val selectedIntensity: WeightIntensity = WeightIntensity.NORMAL,
    val combinations: List<GeneratedCombination> = emptyList(),
    val isGenerating: Boolean = false,
    val hasEnoughHistoryForStats: Boolean = false,
    val explanationForNumber: NumberExplanation? = null,
    val savedConfirmation: Boolean = false
) {
    companion object {
        val GAME_COUNT_OPTIONS = listOf(1, 5, 10, 20, 50, 100)
    }
}
