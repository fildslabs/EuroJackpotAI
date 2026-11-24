package com.eurojackpotai.app.presentation.results

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.PrizeBreakdown
import com.eurojackpotai.app.domain.model.SavedNumber
import com.eurojackpotai.app.domain.model.SupportedCurrency
import com.eurojackpotai.app.domain.usecase.ComparisonResult
import java.time.LocalDate
import java.time.LocalDateTime

data class SavedNumberComparison(
    val savedNumber: SavedNumber,
    val result: ComparisonResult
)

data class ResultsUiState(
    val isLoading: Boolean = true,
    val latestDraw: DrawResult? = null,
    val isSampleData: Boolean = false,
    // Same persisted timestamp Home shows (see LotteryRepository.getLastSyncTime) —
    // null means this device has never successfully synced real results.
    val lastSyncTime: LocalDateTime? = null,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val drawsForSelectedYear: List<DrawResult> = emptyList(),
    val isCheckExpanded: Boolean = false,
    val comparisons: List<SavedNumberComparison> = emptyList(),
    // Display currency (shared with Home via CurrencyRepository — see
    // ResultsViewModel) for the per-country prize amounts below.
    val selectedCurrency: SupportedCurrency = SupportedCurrency.DEFAULT,
    val currencyRatesPerPln: Map<String, Double>? = null,
    // Winners-by-country breakdown for the latest draw (LOTTO OpenAPI
    // draw-prizes/eurojackpot) — fetched on demand the first time the user expands
    // this section, not on screen load, since it's an extra network call beyond the
    // sync the app already does automatically.
    val isCountryBreakdownExpanded: Boolean = false,
    val isCountryBreakdownLoading: Boolean = false,
    val countryBreakdown: PrizeBreakdown? = null,
    val countryBreakdownLoadedForDate: LocalDate? = null,
    // True only once a fetch was actually attempted and came back empty/failed —
    // distinct from "never tried yet", so the UI doesn't flash an error before the
    // first attempt.
    val countryBreakdownUnavailable: Boolean = false
)
