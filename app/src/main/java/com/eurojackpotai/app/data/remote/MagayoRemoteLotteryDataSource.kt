package com.eurojackpotai.app.data.remote

import android.util.Log
import com.eurojackpotai.app.data.remote.api.MagayoApiService
import com.eurojackpotai.app.domain.model.DrawResult
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MagayoRemoteLotteryDataSource(
    private val api: MagayoApiService,
    private val apiKey: String
) : RemoteLotteryDataSource {

    override fun isConfigured(): Boolean = apiKey.isNotBlank()

    override suspend fun fetchDraw(date: String?): DrawResult? {
        if (!isConfigured()) {
            Log.w(TAG, "fetchDraw: no MAGAYO_API_KEY configured (BuildConfig value is blank) — skipping network call")
            return null
        }
        return try {
            val response = api.getDrawResults(apiKey = apiKey, draw = date)
            if (response.error != 0) {
                Log.w(TAG, "fetchDraw: magayo returned error code ${response.error} (see https://www.magayo.com/lottery-docs/api/get-draw-results/ for the meaning of each code)")
                return null
            }

            val drawDate = response.draw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (drawDate == null) {
                Log.w(TAG, "fetchDraw: could not parse draw date from response.draw='${response.draw}'")
                return null
            }
            val numbers = response.results
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
            if (numbers == null) {
                Log.w(TAG, "fetchDraw: response.results was null")
                return null
            }

            if (numbers.size != 7) { // 5 main + 2 euro for this game
                Log.w(TAG, "fetchDraw: expected 7 numbers (5 main + 2 euro), got ${numbers.size} from '${response.results}'")
                return null
            }

            Log.i(TAG, "fetchDraw: success — draw=$drawDate, numbers=$numbers")
            DrawResult(
                date = drawDate,
                mainNumbers = numbers.take(5),
                euroNumbers = numbers.takeLast(2),
                jackpotEur = null,
                isSampleData = false
            )
        } catch (e: Exception) {
            // Network/parse errors are expected (offline, rate limit, key not yet
            // provisioned) and must never crash the app — the repository falls back
            // to local data and surfaces a "last updated" timestamp instead. Logged
            // (not swallowed silently) so a sync failure is diagnosable from Logcat.
            Log.e(TAG, "fetchDraw: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    override suspend fun fetchNextJackpot(): RemoteJackpotInfo? {
        if (!isConfigured()) {
            Log.w(TAG, "fetchNextJackpot: no MAGAYO_API_KEY configured (BuildConfig value is blank) — skipping network call")
            return null
        }
        return try {
            val response = api.getNextJackpot(apiKey = apiKey)
            if (response.error != 0) {
                Log.w(TAG, "fetchNextJackpot: magayo returned error code ${response.error} (see https://www.magayo.com/lottery-docs/api/get-next-jackpot/ for the meaning of each code)")
                return null
            }
            Log.i(TAG, "fetchNextJackpot: success — jackpot=${response.jackpot} ${response.currency}, next_draw=${response.next_draw}")
            RemoteJackpotInfo(
                amount = response.jackpot?.toLongOrNull(),
                currency = response.currency,
                nextDrawDate = response.next_draw
            )
        } catch (e: Exception) {
            Log.e(TAG, "fetchNextJackpot: failed with ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    companion object {
        val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        // A dedicated tag (rather than relying on OkHttp's own interceptor output,
        // which can be easy to mis-filter) so a sync failure is a one-line Logcat
        // search away: filter by "EuroJackpotSync" to see exactly why a call did or
        // didn't go out, instead of guessing from the UI alone.
        private const val TAG = "EuroJackpotSync"
    }
}
