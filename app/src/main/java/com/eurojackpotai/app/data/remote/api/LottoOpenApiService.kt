package com.eurojackpotai.app.data.remote.api

import com.eurojackpotai.app.data.remote.dto.DrawResultsGroupedDto
import com.eurojackpotai.app.data.remote.dto.EuroJackpotPrizesDto
import com.eurojackpotai.app.data.remote.dto.GameJackpotDto
import com.eurojackpotai.app.data.remote.dto.PaginatedDrawResultsDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit contract for the LOTTO OpenAPI (developers.lotto.pl) endpoints this app needs.
 *
 * Auth is a "secret" HTTP header carrying the raw API key — no "Bearer" prefix, per the
 * API's own SecurityDefinitions (confirmed live: `secret: <key>`). Every method returns
 * a [Response] wrapper (rather than the body type directly) so the data source layer can
 * branch on HTTP status — 401 (bad/missing key) and 404 (nothing for this query) both
 * carry meaning here, distinct from a network failure.
 *
 * Kept as a thin, literal mirror of the documented+verified HTTP API; all
 * merging/mapping/business logic lives in
 * [com.eurojackpotai.app.data.remote.LottoOpenApiRemoteDataSource].
 */
interface LottoOpenApiService {

    @GET("api/open/v1/lotteries/info/game-jackpot")
    suspend fun getGameJackpot(
        @Header(SECRET_HEADER) apiKey: String,
        @Query("gameType") gameType: String = GAME_TYPE
    ): Response<GameJackpotDto>

    @GET("api/open/v1/lotteries/draw-results/last-results-per-game")
    suspend fun getLastResults(
        @Header(SECRET_HEADER) apiKey: String,
        @Query("gameType") gameType: String = GAME_TYPE
    ): Response<List<DrawResultsGroupedDto>>

    @GET("api/open/v1/lotteries/draw-results/by-date-per-game")
    suspend fun getResultsByDate(
        @Header(SECRET_HEADER) apiKey: String,
        @Query("gameType") gameType: String = GAME_TYPE,
        @Query("drawDate") drawDate: String,
        @Query("index") index: Int = 1,
        @Query("size") size: Int = 1,
        @Query("sort") sort: String = "drawDate",
        @Query("order") order: String = "DESC"
    ): Response<PaginatedDrawResultsDto>

    @GET("api/open/v1/lotteries/draw-prizes/eurojackpot/{drawSystemId}")
    suspend fun getEuroJackpotPrizes(
        @Header(SECRET_HEADER) apiKey: String,
        @Path("drawSystemId") drawSystemId: Long
    ): Response<List<EuroJackpotPrizesDto>>

    companion object {
        const val BASE_URL = "https://developers.lotto.pl/"
        const val SECRET_HEADER = "secret"
        const val GAME_TYPE = "EuroJackpot"
    }
}
