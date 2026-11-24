package com.eurojackpotai.app.data.remote

import android.util.Log
import com.eurojackpotai.app.data.remote.api.LottoOpenApiService
import com.eurojackpotai.app.data.remote.dto.DrawResultsGroupedDto
import com.eurojackpotai.app.data.remote.dto.EuroJackpotPrizesDto
import com.eurojackpotai.app.domain.model.CountryPrize
import com.eurojackpotai.app.domain.model.DrawResult
import com.eurojackpotai.app.domain.model.EuroJackpotCountry
import com.eurojackpotai.app.domain.model.PrizeBreakdown
import com.eurojackpotai.app.domain.model.PrizeBreakdownTier
import com.eurojackpotai.app.domain.model.TierPrizeAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * [RemoteLotteryDataSource] backed by Poland's official LOTTO OpenAPI
 * (https://developers.lotto.pl), replacing the earlier magayo integration.
 *
 * Why the switch: magayo's free plan caps out at 10 requests/month (confirmed via its
 * own pricing page and error code 303 — see the now-removed MagayoRemoteLotteryDataSource
 * history), which a handful of dev rebuild/relaunch cycles could exhaust in under an
 * hour. LOTTO OpenAPI is the Polish national lottery operator's own free developer feed,
 * provisioned per-app via a manually-issued API key (see local.properties: LOTTO_API_KEY)
 * with no documented per-month cap.
 *
 * One behavior difference worth knowing: the jackpot amount this source returns is
 * denominated in PLN (Polish złoty), not EUR like magayo's — see [fetchNextJackpot].
 */
class LottoOpenApiRemoteDataSource(
    private val api: LottoOpenApiService,
    private val apiKey: String
) : RemoteLotteryDataSource {

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchDraw(date: String?): DrawResult? {
        if (!isConfigured()) {
            Log.w(TAG, "fetchDraw: no LOTTO_API_KEY configured (BuildConfig value is blank) — skipping network call")
            return null
        }
        return try {
            val grouped = if (date == null) {
                val response = api.getLastResults(apiKey = apiKey)
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchDraw: last-results-per-game returned HTTP ${response.code()}")
                    return null
                }
                response.body()?.firstOrNull()
            } else {
                val isoDate = runCatching { LocalDate.parse(date) }.getOrNull()
                if (isoDate == null) {
                    Log.w(TAG, "fetchDraw: could not parse requested date '$date' as yyyy-MM-dd")
                    return null
                }
                // The API takes one instant, not a date range, so a request for "the
                // draw on this date" is sent as midnight UTC on that date — EuroJackpot
                // draws happen in the evening, well within the same calendar day.
                val drawDateTime = isoDate.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                val response = api.getResultsByDate(apiKey = apiKey, drawDate = drawDateTime)
                if (!response.isSuccessful) {
                    Log.w(TAG, "fetchDraw: by-date-per-game returned HTTP ${response.code()} for date=$date")
                    return null
                }
                response.body()?.items?.firstOrNull()
            }

            toDrawResult(grouped)
        } catch (e: Exception) {
            // Network/parse errors are expected (offline, key not yet provisioned, no
            // draw for the requested date) and must never crash the app — the repository
            // falls back to local data and surfaces a "last updated" timestamp instead.
            Log.e(TAG, "fetchDraw: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    private fun toDrawResult(grouped: DrawResultsGroupedDto?): DrawResult? {
        if (grouped == null) {
            Log.w(TAG, "fetchDraw: no draw returned")
            return null
        }
        val item = grouped.results?.firstOrNull()
        if (item == null) {
            Log.w(TAG, "fetchDraw: draw ${grouped.drawSystemId} had no results[] entry")
            return null
        }

        val drawDate = parseDrawDate(grouped.drawDate ?: item.drawDate)
        if (drawDate == null) {
            Log.w(TAG, "fetchDraw: could not parse draw date from '${grouped.drawDate}'")
            return null
        }

        val main = item.resultsJson
        val euro = item.specialResults
        if (main == null || euro == null) {
            Log.w(TAG, "fetchDraw: missing resultsJson/specialResults for draw ${grouped.drawSystemId}")
            return null
        }
        if (main.size != 5 || euro.size != 2) {
            Log.w(TAG, "fetchDraw: expected 5 main + 2 euro numbers, got ${main.size} main + ${euro.size} euro for draw ${grouped.drawSystemId}")
            return null
        }

        Log.i(TAG, "fetchDraw: success — draw=$drawDate, main=$main, euro=$euro (drawSystemId=${grouped.drawSystemId})")
        return DrawResult(
            date = drawDate,
            mainNumbers = main,
            euroNumbers = euro,
            jackpotEur = null,
            isSampleData = false
        )
    }

    override suspend fun fetchNextJackpot(): RemoteJackpotInfo? {
        if (!isConfigured()) {
            Log.w(TAG, "fetchNextJackpot: no LOTTO_API_KEY configured (BuildConfig value is blank) — skipping network call")
            return null
        }
        return try {
            val response = api.getGameJackpot(apiKey = apiKey)
            if (!response.isSuccessful) {
                Log.w(TAG, "fetchNextJackpot: game-jackpot returned HTTP ${response.code()}")
                return null
            }
            val body = response.body()
            val amount = body?.jackpotValue?.toLong()
            if (amount == null) {
                Log.w(TAG, "fetchNextJackpot: response had no jackpotValue")
                return null
            }
            Log.i(TAG, "fetchNextJackpot: success — jackpot=$amount PLN, closestDraw=${body.closestDraw}")
            RemoteJackpotInfo(
                amount = amount,
                // LOTTO OpenAPI is the Polish operator's own feed, so the jackpot value
                // it returns is in PLN, not EUR like magayo's was. HomeScreen's
                // formatJackpot() already renders any non-EUR code as "<amount> <code>"
                // instead of assuming a "€" prefix, so this needs no UI-side change.
                currency = "PLN",
                nextDrawDate = body.closestDraw
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchNextJackpot: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    override suspend fun fetchPrizeBreakdown(date: LocalDate): PrizeBreakdown? {
        if (!isConfigured()) {
            Log.w(TAG, "fetchPrizeBreakdown: no LOTTO_API_KEY configured — skipping network call")
            return null
        }
        return try {
            // draw-prizes/eurojackpot needs LOTTO's internal drawSystemId, which this
            // app never persists (see RemoteLotteryDataSource.fetchPrizeBreakdown doc
            // comment) — resolve it first via the same by-date lookup fetchDraw(date)
            // uses, then use it for the prize-breakdown call.
            val drawDateTime = date.atStartOfDay(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            val byDateResponse = api.getResultsByDate(apiKey = apiKey, drawDate = drawDateTime)
            if (!byDateResponse.isSuccessful) {
                Log.w(TAG, "fetchPrizeBreakdown: by-date-per-game returned HTTP ${byDateResponse.code()} for date=$date")
                return null
            }
            val drawSystemId = byDateResponse.body()?.items?.firstOrNull()?.drawSystemId
            if (drawSystemId == null) {
                Log.w(TAG, "fetchPrizeBreakdown: no drawSystemId found for date=$date")
                return null
            }

            val prizesResponse = api.getEuroJackpotPrizes(apiKey = apiKey, drawSystemId = drawSystemId)
            if (!prizesResponse.isSuccessful) {
                Log.w(TAG, "fetchPrizeBreakdown: draw-prizes/eurojackpot returned HTTP ${prizesResponse.code()} for drawSystemId=$drawSystemId")
                return null
            }
            // See EuroJackpotPrizesDto's doc comment: the spec says this is a single
            // object but the live API returns a one-element array.
            val dto = prizesResponse.body()?.firstOrNull()
            if (dto == null) {
                Log.w(TAG, "fetchPrizeBreakdown: empty response body for drawSystemId=$drawSystemId")
                return null
            }
            toPrizeBreakdown(date, dto).also {
                Log.i(
                    TAG,
                    "fetchPrizeBreakdown: success — drawSystemId=$drawSystemId, " +
                        "${it.tiers.size} tier(s) with a country breakdown, " +
                        "${it.tierPrizeAmounts.size} tier(s) with a Poland payout amount"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchPrizeBreakdown: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    private fun toPrizeBreakdown(date: LocalDate, dto: EuroJackpotPrizesDto): PrizeBreakdown {
        val tiers = dto.countriesPrizes.orEmpty().mapNotNull { tierDto ->
            val degree = tierDto.degree?.toInt() ?: return@mapNotNull null
            // IMPORTANT: prizesValues lists one entry PER WINNING TICKET, not one
            // aggregated entry per country — confirmed live (a 4-winner tier split
            // 3 Germany/1 Netherlands came back as 4 entries, 3 of them "Niemcy").
            // Grouping by country and counting occurrences is what turns this into an
            // actual per-country winner count.
            val countries = tierDto.prizesValues.orEmpty()
                .mapNotNull { cp ->
                    val country = EuroJackpotCountry.fromPolishName(cp.country) ?: return@mapNotNull null
                    // See CountryPrize.prizeValueEur's doc comment: this field is a
                    // cross-border amount shared across countries with different native
                    // currencies, so it's in EUR, not PLN despite living on the same
                    // response as the genuinely-PLN `prizes` map below.
                    country to (cp.prizeValue ?: 0.0)
                }
                .groupBy({ it.first }, { it.second })
                .map { (country, prizeValues) ->
                    CountryPrize(
                        country = country,
                        winners = prizeValues.size,
                        prizeValueEur = prizeValues.firstOrNull() ?: 0.0
                    )
                }
                .sortedByDescending { it.winners }
            if (countries.isEmpty()) return@mapNotNull null
            PrizeBreakdownTier(
                degree = degree,
                totalWinners = tierDto.countWinners?.toInt() ?: countries.sumOf { it.winners },
                countries = countries
            )
        }.sortedBy { it.degree }

        // See PrizeBreakdown's doc comment for what this map is (Poland's own per-tier
        // payout table, keyed "1".."12") and its one open caveat against `tiers` above.
        val tierPrizeAmounts = dto.prizes.orEmpty()
            .mapNotNull { (key, amount) ->
                val rank = key.toIntOrNull() ?: return@mapNotNull null
                val winners = amount.prize ?: return@mapNotNull null
                val value = amount.prizeValue ?: return@mapNotNull null
                rank to TierPrizeAmount(winnersInPoland = winners, prizeValuePln = value)
            }
            .toMap()

        return PrizeBreakdown(drawDate = date, tiers = tiers, tierPrizeAmounts = tierPrizeAmounts)
    }

    private fun parseDrawDate(raw: String?): LocalDate? {
        if (raw == null) return null
        return runCatching { Instant.parse(raw).atZone(ZoneOffset.UTC).toLocalDate() }
            .getOrElse { runCatching { LocalDate.parse(raw) }.getOrNull() }
    }

    companion object {
        // Same tag the whole sync pipeline has always used (magayo included) — filter
        // Logcat by "EuroJackpotSync" to see exactly why a call did or didn't go out.
        private const val TAG = "EuroJackpotSync"
    }
}
