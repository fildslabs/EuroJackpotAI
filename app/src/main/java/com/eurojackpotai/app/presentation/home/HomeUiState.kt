package com.eurojackpotai.app.presentation.home

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.SupportedCurrency
import java.time.LocalDateTime

data class HomeUiState(
    val isLoading: Boolean = true,
    val latestDraw: DrawResult? = null,
    val isSampleData: Boolean = false,
    val savedNumbersCount: Int = 0,
    val countdownDays: Long = 0,
    val countdownHours: Long = 0,
    val countdownMinutes: Long = 0,
    // The accumulated jackpot for the *next* draw (fetched live via magayo's
    // jackpot.php) — not DrawResult.jackpotEur, which is a past draw's own amount and
    // isn't populated yet. Null means "couldn't fetch it" (no API key, offline), never
    // a placeholder amount.
    val nextJackpotAmount: Long? = null,
    val nextJackpotCurrency: String? = null,
    val isOffline: Boolean = false,
    // When the last successful sync happened — persisted (survives app restarts), so
    // it stays accurate even when this run's own sync is throttled/skipped or fails.
    // Null only means "never successfully synced on this device".
    val lastSyncTime: LocalDateTime? = null,
    // True only while a user-triggered refresh (the button next to "최근 추첨 결과") is
    // in flight — separate from isLoading, which is only ever true before the very
    // first Room read completes.
    val isRefreshingResult: Boolean = false,
    // The user's chosen display currency for the jackpot amount (see
    // CurrencyRepository) and the PLN exchange rates needed to convert into it. Rates
    // are the whole PLN->code map (not just the selected one) so switching currency in
    // the picker never needs a network round-trip — see CurrencyFormat.convertAndFormat.
    val selectedCurrency: SupportedCurrency = SupportedCurrency.DEFAULT,
    val currencyRatesPerPln: Map<String, Double>? = null
)
