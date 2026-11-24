package com.eurojackpotai.app.data.remote

import android.util.Log
import com.eurojackpotai.app.data.remote.api.FrankfurterApiService
import com.eurojackpotai.app.domain.model.SupportedCurrency

/**
 * Fetches PLN→[SupportedCurrency] exchange rates from the free Frankfurter API. Unlike
 * [LottoOpenApiRemoteDataSource] this needs no API key and [isConfigured] is always
 * true — there's no "not configured" state for a keyless public API.
 */
class CurrencyRateRemoteDataSource(private val api: FrankfurterApiService) {

    /** One unit of PLN's worth of each non-PLN [SupportedCurrency], keyed by currency
     *  code. Returns null on any failure (offline, malformed response) — the caller
     *  (CurrencyRepositoryImpl) falls back to its own last-cached rates in that case,
     *  same pattern as jackpot/draw sync falling back to local data. */
    suspend fun fetchRatesPerPln(): Map<String, Double>? {
        val symbols = SupportedCurrency.entries
            .filter { it != SupportedCurrency.PLN }
            .joinToString(",") { it.code }
        return try {
            val response = api.getLatestRates(base = SupportedCurrency.PLN.code, symbols = symbols)
            if (!response.isSuccessful) {
                Log.w(TAG, "fetchRatesPerPln: Frankfurter returned HTTP ${response.code()}")
                return null
            }
            val rates = response.body()?.rates
            if (rates == null) {
                Log.w(TAG, "fetchRatesPerPln: response had no rates")
                return null
            }
            Log.i(TAG, "fetchRatesPerPln: success — $rates")
            rates
        } catch (e: Exception) {
            Log.e(TAG, "fetchRatesPerPln: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    private companion object {
        // Same tag as the rest of the sync pipeline — filter Logcat by "EuroJackpotSync".
        const val TAG = "EuroJackpotSync"
    }
}
