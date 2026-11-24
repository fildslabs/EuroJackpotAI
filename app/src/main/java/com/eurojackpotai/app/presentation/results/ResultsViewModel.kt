package com.eurojackpotai.app.presentation.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eurojackpotai.app.data.repository.CurrencyRepositoryImpl
import com.eurojackpotai.app.data.repository.LotteryRepositoryImpl
import com.eurojackpotai.app.domain.model.SupportedCurrency
import com.eurojackpotai.app.domain.repository.SavedNumberRepository
import com.eurojackpotai.app.domain.usecase.CompareNumbersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class ResultsViewModel(
    private val lotteryRepository: LotteryRepositoryImpl,
    private val savedNumberRepository: SavedNumberRepository,
    private val compareNumbersUseCase: CompareNumbersUseCase,
    private val currencyRepository: CurrencyRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            currencyRepository.observeSelectedCurrency().collect { currency ->
                _uiState.update { it.copy(selectedCurrency = currency) }
            }
        }
        viewModelScope.launch {
            _uiState.update { it.copy(currencyRatesPerPln = currencyRepository.getRatesPerPln()) }
        }
        viewModelScope.launch {
            combine(
                lotteryRepository.observeLatestDraw(),
                savedNumberRepository.observeAll()
            ) { latest, saved -> latest to saved }
                .collect { (latest, saved) ->
                    val comparisons = if (latest != null) {
                        saved.filter { it.isActive }.map { number ->
                            SavedNumberComparison(number, compareNumbersUseCase(number, latest))
                        }
                    } else {
                        emptyList()
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            latestDraw = latest,
                            comparisons = comparisons
                        )
                    }
                    // At least one saved number actually won something on this draw —
                    // fetch the prize breakdown (same call "Winners by Country" uses)
                    // right away rather than waiting for the user to expand that
                    // section, since ComparisonRow needs tierPrizeAmounts from it to
                    // show the won amount (see loadPrizeBreakdown).
                    if (latest != null && comparisons.any { it.result.prizeTier != null }) {
                        loadPrizeBreakdown(latest.date)
                    }
                }
        }
        viewModelScope.launch {
            val years = lotteryRepository.getAvailableYears()
            _uiState.update { it.copy(availableYears = years) }
            _uiState.update { it.copy(isSampleData = lotteryRepository.isShowingSampleData()) }
            _uiState.update { it.copy(lastSyncTime = lotteryRepository.getLastSyncTime()) }
            years.firstOrNull()?.let { selectYear(it) }
        }
    }

    fun selectYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
        viewModelScope.launch {
            val draws = lotteryRepository.getDrawsForYear(year)
            _uiState.update { it.copy(drawsForSelectedYear = draws) }
        }
    }

    fun toggleCheckExpanded() {
        _uiState.update { it.copy(isCheckExpanded = !it.isCheckExpanded) }
    }

    fun selectCurrency(currency: SupportedCurrency) {
        currencyRepository.setSelectedCurrency(currency)
    }

    /** Expands/collapses the "Winners by Country" section on the latest draw, fetching
     *  the breakdown the first time it's opened for that draw's date — not eagerly on
     *  screen load, since draw-prizes/eurojackpot is an extra network call the app
     *  otherwise never makes automatically (see LottoOpenApiRemoteDataSource.fetchPrizeBreakdown). */
    fun toggleCountryBreakdown() {
        val expanding = !_uiState.value.isCountryBreakdownExpanded
        _uiState.update { it.copy(isCountryBreakdownExpanded = expanding) }
        if (!expanding) return

        val date = _uiState.value.latestDraw?.date ?: return
        viewModelScope.launch { loadPrizeBreakdown(date) }
    }

    /** Shared by [toggleCountryBreakdown] (user opened "Winners by Country") and the
     *  win-checking collector in [init] (a saved number actually won something) — both
     *  need the exact same draw-prizes/eurojackpot call, just for different parts of
     *  the response ([PrizeBreakdown.tiers] vs. [PrizeBreakdown.tierPrizeAmounts]), so
     *  whichever asks first fetches it once and both read the same cached result. */
    private suspend fun loadPrizeBreakdown(date: LocalDate) {
        if (_uiState.value.countryBreakdownLoadedForDate == date) return // already fetched for this draw
        _uiState.update { it.copy(isCountryBreakdownLoading = true) }
        val breakdown = lotteryRepository.getPrizeBreakdown(date)
        _uiState.update {
            it.copy(
                isCountryBreakdownLoading = false,
                countryBreakdown = breakdown,
                countryBreakdownLoadedForDate = date,
                countryBreakdownUnavailable = breakdown == null
            )
        }
    }
}
