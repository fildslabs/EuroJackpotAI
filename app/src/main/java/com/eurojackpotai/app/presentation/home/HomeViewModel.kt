package com.eurojackpotai.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eurojackpotai.app.core.util.DrawSchedule
import com.eurojackpotai.app.data.repository.CurrencyRepositoryImpl
import com.eurojackpotai.app.data.repository.LotteryRepositoryImpl
import com.eurojackpotai.app.domain.model.SupportedCurrency
import com.eurojackpotai.app.domain.repository.SavedNumberRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime

class HomeViewModel(
    private val lotteryRepository: LotteryRepositoryImpl,
    private val savedNumberRepository: SavedNumberRepository,
    private val currencyRepository: CurrencyRepositoryImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeData()
        observeSelectedCurrency()
        startCountdownTicker()
        refreshSampleDataFlag()
        refreshLastSyncTime() // picks up a previous session's sync time immediately,
        // even before this session's own (possibly throttled/failed) attempt resolves.
        syncInBackground()
        loadNextJackpot()
        loadCurrencyRates()
    }

    private fun observeSelectedCurrency() {
        viewModelScope.launch {
            currencyRepository.observeSelectedCurrency().collect { currency ->
                _uiState.update { it.copy(selectedCurrency = currency) }
            }
        }
    }

    private fun loadCurrencyRates(force: Boolean = false) {
        viewModelScope.launch {
            val rates = currencyRepository.getRatesPerPln(force = force)
            _uiState.update { it.copy(currencyRatesPerPln = rates) }
        }
    }

    /** Called from the currency picker chips on Home. Persists immediately (see
     *  CurrencyRepositoryImpl) so it's remembered on the next launch too. */
    fun selectCurrency(currency: SupportedCurrency) {
        currencyRepository.setSelectedCurrency(currency)
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                lotteryRepository.observeLatestDraw(),
                savedNumberRepository.observeAll()
            ) { latest, saved -> latest to saved.size }
                .collect { (latest, savedCount) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            latestDraw = latest,
                            savedNumbersCount = savedCount
                        )
                    }
                }
        }
    }

    private fun loadNextJackpot(force: Boolean = false) {
        viewModelScope.launch {
            val nextJackpot = lotteryRepository.getNextJackpot(force = force)
            _uiState.update {
                it.copy(
                    nextJackpotAmount = nextJackpot?.amount,
                    nextJackpotCurrency = nextJackpot?.currency
                )
            }
        }
    }

    private fun startCountdownTicker() {
        viewModelScope.launch {
            while (isActive) {
                val duration = Duration.between(LocalDateTime.now(), DrawSchedule.nextDrawDateTime())
                    .let { if (it.isNegative) Duration.ZERO else it }
                _uiState.update {
                    it.copy(
                        countdownDays = duration.toDays(),
                        countdownHours = duration.toHours() % 24,
                        countdownMinutes = duration.toMinutes() % 60
                    )
                }
                delay(30_000)
            }
        }
    }

    private fun refreshSampleDataFlag() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSampleData = lotteryRepository.isShowingSampleData()) }
        }
    }

    private fun refreshLastSyncTime() {
        viewModelScope.launch {
            _uiState.update { it.copy(lastSyncTime = lotteryRepository.getLastSyncTime()) }
        }
    }

    private fun syncInBackground() {
        viewModelScope.launch {
            val result = lotteryRepository.syncLatestResults()
            _uiState.update { it.copy(isOffline = !result.success) }
            refreshSampleDataFlag()
            refreshLastSyncTime()
        }
    }

    /** Manually triggered by the refresh button next to "최근 추첨 결과" — lets someone
     *  who left the app open through a draw see the new result without restarting it,
     *  instead of waiting for the next app launch's automatic [syncInBackground]. Reuses
     *  the exact same repository call, so a manual refresh and the startup sync can never
     *  disagree about what counts as "already synced". */
    fun refreshLatestResult() {
        if (_uiState.value.isRefreshingResult) return // ignore taps while one is already in flight
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingResult = true) }
            // force = true: this is the one place a user explicitly asked for a fresh
            // check, so it bypasses SyncThrottle's automatic-sync cooldown on purpose.
            val result = lotteryRepository.syncLatestResults(force = true)
            _uiState.update { it.copy(isRefreshingResult = false, isOffline = !result.success) }
            refreshSampleDataFlag()
            refreshLastSyncTime()
            loadNextJackpot(force = true) // the next-draw jackpot resets once that draw has happened
        }
    }
}
