package com.eurojackpotai.app.domain.repository

import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.NextJackpot
import com.eurojackpotai.app.domain.model.PrizeBreakdown
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

data class SyncResult(
    val success: Boolean,
    val newDrawsAdded: Int,
    val message: String? = null
)

/**
 * Single source of truth for draw history. The implementation always serves from the
 * local database first (so the app works fully offline) and treats a remote sync as an
 * optional refresh — see LotteryRepositoryImpl for the merge/validation logic.
 */
interface LotteryRepository {
    fun observeAllDraws(): Flow<List<DrawResult>>
    fun observeLatestDraw(): Flow<DrawResult?>
    suspend fun getDrawsForYear(year: Int): List<DrawResult>
    suspend fun getAvailableYears(): List<Int>
    suspend fun getLastSyncTime(): LocalDateTime?
    suspend fun isShowingSampleData(): Boolean

    /** Pulls the latest results from the remote data source and merges them locally.
     *  Automatic calls (app startup) are throttled to protect magayo's request quota —
     *  pass [force] = true only for a user-initiated check (the Home refresh button). */
    suspend fun syncLatestResults(force: Boolean = false): SyncResult

    /** The accumulated jackpot for the next draw. Null when no API key is configured,
     *  the fetch fails, and nothing is cached from a previous fetch — never a
     *  placeholder amount. Same throttling as [syncLatestResults]. */
    suspend fun getNextJackpot(force: Boolean = false): NextJackpot?

    /** Per-country winner counts for the draw on [date] (typically the latest draw).
     *  Not cached/throttled like the above — this is fetched on the user's explicit
     *  request to view the breakdown (see ResultsViewModel), not on every app launch.
     *  Null when the data source doesn't support it or the fetch fails. */
    suspend fun getPrizeBreakdown(date: LocalDate): PrizeBreakdown?
}
