package com.eurojackpotai.app.data.remote.api

import com.eurojackpotai.app.data.remote.dto.DrawResultsResponse
import com.eurojackpotai.app.data.remote.dto.NextJackpotResponse
import com.eurojackpotai.app.data.remote.dto.PastJackpotResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit contract for the three magayo endpoints this app needs. Kept as a thin,
 * literal mirror of the documented HTTP API — all merging/validation/business logic
 * lives in [com.eurojackpotai.app.data.remote.MagayoRemoteLotteryDataSource] and the
 * repository layer, never here.
 */
interface MagayoApiService {

    @GET("api/results.php")
    suspend fun getDrawResults(
        @Query("api_key") apiKey: String,
        @Query("game") game: String = GAME_CODE,
        @Query("draw") draw: String? = null
    ): DrawResultsResponse

    @GET("api/jackpot.php")
    suspend fun getNextJackpot(
        @Query("api_key") apiKey: String,
        @Query("game") game: String = GAME_CODE
    ): NextJackpotResponse

    @GET("api/past_jackpot.php")
    suspend fun getPastJackpot(
        @Query("api_key") apiKey: String,
        @Query("game") game: String = GAME_CODE,
        @Query("draw") draw: String
    ): PastJackpotResponse

    companion object {
        const val BASE_URL = "https://www.magayo.com/"

        // "eurojackpot" is the generic multi-country game code. magayo also exposes
        // country-specific variants (e.g. "pl_eurojackpot" for PLN jackpot amounts) —
        // see https://www.magayo.com/lottery-docs/api/supported-jackpots/ if that's
        // ever needed instead.
        const val GAME_CODE = "eurojackpot"
    }
}
