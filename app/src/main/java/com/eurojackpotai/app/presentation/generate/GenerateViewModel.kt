package com.eurojackpotai.app.presentation.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eurojackpotai.app.data.repository.LotteryRepositoryImpl
import com.eurojackpotai.app.domain.model.GenerationMode
import com.eurojackpotai.app.domain.model.LotteryStatistics
import com.eurojackpotai.app.domain.model.NumberExplanation
import com.eurojackpotai.app.domain.model.NumberSource
import com.eurojackpotai.app.domain.model.SavedNumber
import com.eurojackpotai.app.domain.model.WeightIntensity
import com.eurojackpotai.app.domain.repository.SavedNumberRepository
import com.eurojackpotai.app.domain.usecase.ComputeStatisticsUseCase
import com.eurojackpotai.app.domain.usecase.GenerateNumbersUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GenerateViewModel(
    private val lotteryRepository: LotteryRepositoryImpl,
    private val savedNumberRepository: SavedNumberRepository,
    private val computeStatisticsUseCase: ComputeStatisticsUseCase,
    private val generateNumbersUseCase: GenerateNumbersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenerateUiState())
    val uiState: StateFlow<GenerateUiState> = _uiState.asStateFlow()

    private var cachedStatistics: LotteryStatistics? = null

    init {
        viewModelScope.launch {
            val draws = lotteryRepository.observeAllDraws().first()
            cachedStatistics = if (draws.isNotEmpty()) computeStatisticsUseCase(draws) else null
            _uiState.update { it.copy(hasEnoughHistoryForStats = draws.size >= 10) }
        }
    }

    fun selectMode(mode: GenerationMode) {
        if (!mode.isAvailable) return
        _uiState.update { it.copy(selectedMode = mode) }
    }

    fun selectGameCount(count: Int) {
        _uiState.update { it.copy(selectedGameCount = count) }
    }

    fun selectIntensity(intensity: WeightIntensity) {
        _uiState.update { it.copy(selectedIntensity = intensity) }
    }

    fun generate() {
        val mode = _uiState.value.selectedMode
        val count = _uiState.value.selectedGameCount
        val intensity = _uiState.value.selectedIntensity
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            val combinations = withContext(Dispatchers.Default) {
                generateNumbersUseCase(
                    mode = mode,
                    statistics = cachedStatistics,
                    count = count,
                    hotColdIntensity = intensity
                )
            }
            _uiState.update { it.copy(isGenerating = false, combinations = combinations) }
        }
    }

    fun showExplanation(explanation: NumberExplanation) {
        _uiState.update { it.copy(explanationForNumber = explanation) }
    }

    fun dismissExplanation() {
        _uiState.update { it.copy(explanationForNumber = null) }
    }

    fun saveAllGenerated() {
        val combinations = _uiState.value.combinations
        val mode = _uiState.value.selectedMode
        if (combinations.isEmpty()) return
        viewModelScope.launch {
            combinations.forEachIndexed { index, combination ->
                savedNumberRepository.save(
                    SavedNumber(
                        name = "${mode.name.lowercase().replaceFirstChar { it.uppercase() }} #${index + 1}",
                        mainNumbers = combination.mainNumbers,
                        euroNumbers = combination.euroNumbers,
                        source = NumberSource.GENERATED,
                        generationMode = mode
                    )
                )
            }
            _uiState.update { it.copy(savedConfirmation = true) }
        }
    }

    fun consumeSavedConfirmation() {
        _uiState.update { it.copy(savedConfirmation = false) }
    }
}
