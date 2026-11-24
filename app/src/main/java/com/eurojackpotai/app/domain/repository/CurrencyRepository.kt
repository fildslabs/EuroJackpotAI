package com.eurojackpotai.app.domain.repository

import com.eurojackpotai.app.domain.model.SupportedCurrency
import kotlinx.coroutines.flow.StateFlow

/**
 * The user's chosen display currency, plus conversion of PLN amounts (LOTTO OpenAPI's
 * native currency — see LottoOpenApiRemoteDataSource) into it. Backed by the free
 * Frankfurter exchange-rate API; see CurrencyRepositoryImpl for caching/fallback
 * behavior.
 */
interface CurrencyRepository {
    /** Reactive so every screen showing a converted amount (Home, Results) updates
     *  immediately when the user changes currency, without each needing its own
     *  SharedPreferences read. */
    fun observeSelectedCurrency(): StateFlow<SupportedCurrency>

    fun setSelectedCurrency(currency: SupportedCurrency)

    /** Converts a PLN amount to [target]. Returns null only when no rate is available
     *  at all (first-ever launch, offline, and nothing cached from a previous
     *  successful fetch) — never a wrong or stale-without-explanation number. */
    suspend fun convertFromPln(amountPln: Double, target: SupportedCurrency, force: Boolean = false): Double?

    /** One unit of PLN's worth of each non-PLN [SupportedCurrency], keyed by currency
     *  code — exposed directly (not just via [convertFromPln]) so a screen converting
     *  several amounts at once (e.g. every country's prize value in a breakdown) can
     *  fetch the rate table once and do the multiplication locally instead of making
     *  one suspend call per amount. Null under the same conditions as [convertFromPln]. */
    suspend fun getRatesPerPln(force: Boolean = false): Map<String, Double>?
}
