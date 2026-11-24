package com.eurojackpotai.app.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eurojackpotai.app.domain.repository.LotteryRepository
import com.eurojackpotai.app.domain.usecase.ComputeStatisticsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsViewModel(
    private val lotteryRepository: LotteryRepository,
    private val computeStatisticsUseCase: ComputeStatisticsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            lotteryRepository.observeAllDraws().collectLatest { draws ->
                val stats = if (draws.isEmpty()) {
                    null
                } else {
                    withContext(Dispatchers.Default) { computeStatisticsUseCase(draws) }
                }
                _uiState.value = StatisticsUiState(isLoading = false, statistics = stats)
            }
        }
    }
}
