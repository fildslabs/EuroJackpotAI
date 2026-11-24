package com.eurojackpotai.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * GET https://api.frankfurter.dev/v1/latest — a free, no-API-key exchange-rate API
 * backed by the European Central Bank's daily reference rates. Verified live
 * (2026-09-28): `GET /v1/latest?base=PLN&symbols=EUR,USD,KRW,GBP,JPY` returned
 * `{"amount":1.0,"base":"PLN","date":"2026-09-28","rates":{"EUR":0.22868,...}}` — no
 * auth header, no key required, matching its own docs ("You don't need an API key").
 *
 * [rates] maps a currency code (e.g. "EUR") to how much of it 1 unit of [base] buys —
 * used here as PLN→target rates so LOTTO OpenAPI's PLN-denominated jackpot/prize
 * amounts (see LottoOpenApiRemoteDataSource) can be shown in a user-selected currency.
 */
@Serializable
data class FrankfurterRatesDto(
    val amount: Double? = null,
    val base: String? = null,
    val date: String? = null,
    val rates: Map<String, Double>? = null
)
