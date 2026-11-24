package com.eurojackpotai.app.data.remote.api

import com.eurojackpotai.app.data.remote.dto.FrankfurterRatesDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit contract for the Frankfurter exchange-rate API (https://api.frankfurter.dev).
 * Free, keyless, ECB-reference-rate-based — see [FrankfurterRatesDto] for the
 * verification note. Used to convert LOTTO OpenAPI's PLN-denominated amounts into a
 * user-selected [com.eurojackpotai.app.domain.model.SupportedCurrency].
 */
interface FrankfurterApiService {

    @GET("v1/latest")
    suspend fun getLatestRates(
        @Query("base") base: String,
        @Query("symbols") symbols: String
    ): Response<FrankfurterRatesDto>

    companion object {
        const val BASE_URL = "https://api.frankfurter.dev/"
    }
}
