package com.eurojackpotai.app.data.local

import android.content.Context

/**
 * Persists just enough state to avoid calling the remote API on every single app cold
 * start: when a sync last actually ran, and the last jackpot values it returned. Backed
 * by SharedPreferences (not Room) since it's tiny, non-relational key/value state
 * unrelated to the draw-history table.
 *
 * Why this exists: the original remote source (magayo) had a request quota — its API
 * returned error 303, "API limit reached", once exceeded — and every app launch used to
 * fire two fresh network calls (results + jackpot) with zero user intent behind it, so a
 * handful of rebuild/relaunch cycles during development could exhaust a day's quota on
 * their own. The app has since switched to Poland's official LOTTO OpenAPI
 * (see LottoOpenApiRemoteDataSource), which has no documented per-month cap, but the
 * throttle stays — there's still no reason to hit the network on every cold start when
 * the data is only ever refreshed on a weekly draw schedule. The cooldown here only
 * throttles that *automatic* background sync (HomeViewModel.init); the manual refresh
 * button on Home (HomeViewModel.refreshLatestResult) always passes force = true and
 * bypasses it, since checking on demand is the whole point of that button.
 */
class SyncThrottle(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var lastSyncEpochMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var cachedJackpotAmount: Long?
        get() = prefs.getLong(KEY_JACKPOT_AMOUNT, NO_VALUE).takeIf { it != NO_VALUE }
        set(value) = prefs.edit().putLong(KEY_JACKPOT_AMOUNT, value ?: NO_VALUE).apply()

    var cachedJackpotCurrency: String?
        get() = prefs.getString(KEY_JACKPOT_CURRENCY, null)
        set(value) = prefs.edit().putString(KEY_JACKPOT_CURRENCY, value).apply()

    var cachedJackpotNextDraw: String?
        get() = prefs.getString(KEY_JACKPOT_NEXT_DRAW, null)
        set(value) = prefs.edit().putString(KEY_JACKPOT_NEXT_DRAW, value).apply()

    fun isCooldownActive(now: Long = System.currentTimeMillis()): Boolean =
        now - lastSyncEpochMillis < COOLDOWN_MILLIS

    fun markSynced(now: Long = System.currentTimeMillis()) {
        lastSyncEpochMillis = now
    }

    companion object {
        private const val PREFS_NAME = "sync_throttle"
        private const val KEY_LAST_SYNC = "last_sync_epoch_millis"
        private const val KEY_JACKPOT_AMOUNT = "jackpot_amount"
        private const val KEY_JACKPOT_CURRENCY = "jackpot_currency"
        private const val KEY_JACKPOT_NEXT_DRAW = "jackpot_next_draw"
        private const val NO_VALUE = -1L

        // 1 hour — was 24h while the remote source was magayo, whose free plan capped
        // out at just 10 requests/month, but that constraint no longer applies now that
        // the source is LOTTO OpenAPI (no documented per-month cap). Shortened back down
        // so automatic background syncs (app cold start) don't sit stale for a whole day;
        // the manual refresh button still bypasses this (force = true) for checking on
        // demand at any time.
        private const val COOLDOWN_MILLIS = 60 * 60 * 1000L
    }
}
