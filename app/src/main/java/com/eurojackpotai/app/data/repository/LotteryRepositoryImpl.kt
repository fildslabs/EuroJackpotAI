package com.eurojackpotai.app.data.repository

import com.eurojackpotai.app.data.local.SampleDrawSeeder
import com.eurojackpotai.app.data.local.SyncThrottle
import com.eurojackpotai.app.data.local.dao.DrawResultDao
import com.eurojackpotai.app.data.local.entity.toDomain
import com.eurojackpotai.app.data.local.entity.toEntity
import com.eurojackpotai.app.data.remote.RemoteLotteryDataSource
import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.NextJackpot
import com.eurojackpotai.app.domain.model.PrizeBreakdown
import com.eurojackpotai.app.domain.repository.LotteryRepository
import com.eurojackpotai.app.domain.repository.SyncResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class LotteryRepositoryImpl(
    private val dao: DrawResultDao,
    private val remote: RemoteLotteryDataSource,
    private val syncThrottle: SyncThrottle
) : LotteryRepository {

    /** Seeds the sample dataset the first time the app runs with an empty database. */
    suspend fun ensureSeeded() {
        if (dao.count() == 0) {
            dao.insertAll(SampleDrawSeeder.generate().map { it.toEntity() })
        }
    }

    override fun observeAllDraws(): Flow<List<DrawResult>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeLatestDraw(): Flow<DrawResult?> =
        dao.observeLatest().map { it?.toDomain() }

    override suspend fun getDrawsForYear(year: Int): List<DrawResult> =
        dao.getForYear(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
            .map { it.toDomain() }

    override suspend fun getAvailableYears(): List<Int> = dao.getAvailableYears()

    // Backed by SyncThrottle (SharedPreferences) rather than an in-memory field so it
    // survives process death — otherwise every app restart would forget when the last
    // real sync happened and the "last updated" timestamp shown to the user (see Home
    // and Results screens) would blank out on every cold start even though the locally
    // cached draw data is still perfectly valid.
    override suspend fun getLastSyncTime(): LocalDateTime? {
        val epochMillis = syncThrottle.lastSyncEpochMillis
        if (epochMillis == 0L) return null
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
    }

    override suspend fun isShowingSampleData(): Boolean = dao.sampleDataCount() > 0

    override suspend fun syncLatestResults(force: Boolean): SyncResult {
        if (!remote.isConfigured()) {
            return SyncResult(
                success = false,
                newDrawsAdded = 0,
                message = "No API key configured — showing local/sample data only."
            )
        }

        // Automatic syncs (every app launch) are throttled — see SyncThrottle for why.
        // The manual refresh button passes force = true and always goes through.
        if (!force && syncThrottle.isCooldownActive()) {
            return SyncResult(success = true, newDrawsAdded = 0, message = null)
        }

        val latest = remote.fetchDraw() ?: return SyncResult(
            success = false,
            newDrawsAdded = 0,
            message = "Could not reach the results server. Showing the last saved data."
        )

        // Checked against real rows only (not existsForDate): the bundled sample
        // history is generated relative to today's date (SampleDrawSeeder), so it can
        // easily already have a row on the same calendar date as the real draw —
        // existsForDate would then wrongly say "yes" and this sync would silently do
        // nothing, forever, every time the app starts.
        val alreadyExists = dao.existsRealDataForDate(latest.date)
        if (!alreadyExists) {
            // A real result has arrived: the bundled placeholder rows would otherwise
            // sit alongside genuine ones with no visual distinction other than the
            // badge, so they're cleared out the first time real data lands.
            dao.clearSampleData()
            dao.insertAll(listOf(latest.toEntity()))
        }
        syncThrottle.markSynced()

        return SyncResult(
            success = true,
            newDrawsAdded = if (alreadyExists) 0 else 1,
            message = null
        )
    }

    override suspend fun getNextJackpot(force: Boolean): NextJackpot? {
        if (!force && syncThrottle.isCooldownActive()) {
            // Still within the cooldown window — serve the last successfully fetched
            // value instead of calling the network again. Only returns null here if
            // nothing has ever been fetched yet, same as a real fetch failure would.
            val cachedAmount = syncThrottle.cachedJackpotAmount ?: return null
            return NextJackpot(
                amount = cachedAmount,
                currency = syncThrottle.cachedJackpotCurrency,
                nextDrawDate = syncThrottle.cachedJackpotNextDraw
            )
        }

        val info = remote.fetchNextJackpot() ?: return null
        syncThrottle.cachedJackpotAmount = info.amount
        syncThrottle.cachedJackpotCurrency = info.currency
        syncThrottle.cachedJackpotNextDraw = info.nextDrawDate
        return NextJackpot(
            amount = info.amount,
            currency = info.currency,
            nextDrawDate = info.nextDrawDate
        )
    }

    override suspend fun getPrizeBreakdown(date: LocalDate): PrizeBreakdown? = remote.fetchPrizeBreakdown(date)
}
