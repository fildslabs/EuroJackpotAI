package com.eurojackpotai.app.data.remote

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.PrizeBreakdown
import java.time.LocalDate

data class RemoteJackpotInfo(
    val amount: Long?,
    val currency: String?,
    val nextDrawDate: String?
)

/**
 * Boundary between "the internet" and the rest of the app. LotteryRepositoryImpl only
 * talks to this interface, so swapping the provider — as already happened once, from
 * magayo (too low a request quota to be practical) to Poland's official LOTTO OpenAPI —
 * never touches the DAO, the use cases or the UI. Returns null/empty rather than
 * throwing on network/parse failure; the caller decides how to present that (offline
 * banner, "last updated" timestamp, etc.).
 */
interface RemoteLotteryDataSource {
    /** Fetches a single draw. Pass null to get the most recent one. Returns null on failure. */
    suspend fun fetchDraw(date: String? = null): DrawResult?

    suspend fun fetchNextJackpot(): RemoteJackpotInfo?

    /** Per-country winner counts for the draw on [date]. Defaults to null (not
     *  supported) so a future/alternate data source doesn't have to implement this to
     *  satisfy the interface — only LottoOpenApiRemoteDataSource currently overrides
     *  it. Deliberately resolved on demand (a fresh network call), not persisted in
     *  Room: it's per-draw metadata (LOTTO OpenAPI's internal drawSystemId) that this
     *  app's schema has no column for, and adding one would mean bumping
     *  AppDatabase's version for a feature that only needs an occasional lookup. */
    suspend fun fetchPrizeBreakdown(date: LocalDate): PrizeBreakdown? = null

    /** True only when an API key has actually been configured (see local.properties). */
    fun isConfigured(): Boolean
}
