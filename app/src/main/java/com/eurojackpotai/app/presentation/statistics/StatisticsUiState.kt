package com.eurojackpotai.app.presentation.statistics

import com.eurojackpotai.app.domain.model.LotteryStatistics

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val statistics: LotteryStatistics? = null
)
