package com.eurojackpotai.app.data.local

import android.content.Context
import com.eurojackpotai.app.domain.model.SupportedCurrency

/**
 * Persists the user's chosen display currency and the last fetched PLN exchange
 * rates. Backed by SharedPreferences, same pattern as [SyncThrottle] — small,
 * non-relational key/value state that doesn't belong in Room (and deliberately isn't:
 * adding it as a table/entity would mean bumping AppDatabase's version for something
 * that's really per-device UI state, not lottery data).
 *
 * Rates are stored as a simple "CODE:value,CODE:value" string rather than JSON, so
 * this class stays a plain SharedPreferences wrapper with no serialization dependency
 * (kotlinx.serialization's Json instance lives in AppContainer, one layer up).
 */
class CurrencyPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedCurrencyCode: String
        get() = prefs.getString(KEY_SELECTED_CURRENCY, SupportedCurrency.DEFAULT.code)
            ?: SupportedCurrency.DEFAULT.code
        set(value) = prefs.edit().putString(KEY_SELECTED_CURRENCY, value).apply()

    var lastRatesFetchEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_FETCH, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_FETCH, value).apply()

    var cachedRates: Map<String, Double>?
        get() = prefs.getString(KEY_CACHED_RATES, null)?.let(::decodeRates)
        set(value) = prefs.edit().putString(KEY_CACHED_RATES, value?.let(::encodeRates)).apply()

    fun isCooldownActive(now: Long = System.currentTimeMillis()): Boolean =
        now - lastRatesFetchEpochMillis < COOLDOWN_MILLIS

    private fun encodeRates(rates: Map<String, Double>): String =
        rates.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun decodeRates(raw: String): Map<String, Double> =
        raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            val code = parts.getOrNull(0)
            val value = parts.getOrNull(1)?.toDoubleOrNull()
            if (code != null && value != null) code to value else null
        }.toMap()

    companion object {
        private const val PREFS_NAME = "currency_prefs"
        private const val KEY_SELECTED_CURRENCY = "selected_currency"
        private const val KEY_LAST_FETCH = "last_fetch_epoch_millis"
        private const val KEY_CACHED_RATES = "cached_rates"

        // Frankfurter's rates are the ECB's daily reference rates (updated once per
        // banking day around 16:00 CET), so re-fetching more often than this buys
        // nothing — 6h just means a freshly-opened app picks up a new banking day's
        // rate reasonably promptly without a network call on every screen visit.
        private const val COOLDOWN_MILLIS = 6 * 60 * 60 * 1000L
    }
}
