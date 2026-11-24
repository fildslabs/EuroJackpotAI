package com.eurojackpotai.app.data.repository

import com.eurojackpotai.app.data.local.CurrencyPreferences
import com.eurojackpotai.app.data.remote.CurrencyRateRemoteDataSource
import com.eurojackpotai.app.domain.model.SupportedCurrency
import com.eurojackpotai.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrencyRepositoryImpl(
    private val remote: CurrencyRateRemoteDataSource,
    private val prefs: CurrencyPreferences
) : CurrencyRepository {

    private val _selectedCurrency = MutableStateFlow(
        SupportedCurrency.fromCode(prefs.selectedCurrencyCode) ?: SupportedCurrency.DEFAULT
    )

    override fun observeSelectedCurrency(): StateFlow<SupportedCurrency> = _selectedCurrency.asStateFlow()

    override fun setSelectedCurrency(currency: SupportedCurrency) {
        prefs.selectedCurrencyCode = currency.code
        _selectedCurrency.value = currency
    }

    override suspend fun convertFromPln(amountPln: Double, target: SupportedCurrency, force: Boolean): Double? {
        if (target == SupportedCurrency.PLN) return amountPln
        val rates = getRatesPerPln(force) ?: return null
        val rate = rates[target.code] ?: return null
        return amountPln * rate
    }

    /** Same throttle/cache pattern as SyncThrottle-backed jackpot fetches: serve the
     *  cached rates within the cooldown window, and fall back to whatever's cached
     *  (even if stale) rather than null when a fresh fetch fails — a same-day-ish rate
     *  is far more useful to show than nothing, as long as it's never silently treated
     *  as live. */
    override suspend fun getRatesPerPln(force: Boolean): Map<String, Double>? {
        if (!force && prefs.isCooldownActive()) {
            prefs.cachedRates?.let { return it }
        }
        val fresh = remote.fetchRatesPerPln()
        if (fresh != null) {
            prefs.cachedRates = fresh
            prefs.lastRatesFetchEpochMillis = System.currentTimeMillis()
            return fresh
        }
        return prefs.cachedRates
    }
}
