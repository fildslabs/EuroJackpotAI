package com.eurojackpotai.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response shapes for Poland's official LOTTO OpenAPI (https://developers.lotto.pl),
 * run by Totalizator Sportowy — replaces magayo (see LottoOpenApiRemoteDataSource for
 * why: magayo's free tier caps out at 10 requests/month, which made it impractical for
 * real use).
 *
 * Field names mirror the live API responses. Verified against BOTH the published spec
 * (https://developers.lotto.pl/swagger/open-api-v1/swagger.json) AND real authenticated
 * calls — see [EuroJackpotPrizesDto] for a case where the two actually disagreed.
 * `ignoreUnknownKeys = true` (AppContainer's Json instance) means any field this app
 * doesn't need can simply be left out of these DTOs rather than fully mirrored.
 */

/** GET /api/open/v1/lotteries/info/game-jackpot */
@Serializable
data class GameJackpotDto(
    val jackpotValue: Double? = null,
    val jackpotPlusValue: Double? = null,
    // ISO-8601 instant, e.g. "2026-09-29T18:15:00Z" — also doubles as the next-draw
    // date/time, so a separate call to the next-draw endpoint isn't needed.
    val closestDraw: String? = null
)

/** One game's numbers within a [DrawResultsGroupedDto]. */
@Serializable
data class DrawResultItemDto(
    val drawDate: String? = null,
    val drawSystemId: Long? = null,
    val gameType: String? = null,
    // For EuroJackpot: the 5 main numbers (1-50).
    val resultsJson: List<Int>? = null,
    // For EuroJackpot: the 2 Euro numbers (1-12).
    val specialResults: List<Int>? = null
)

/**
 * A single draw. Despite the name (LOTTO OpenAPI "groups" every game that drew at a
 * given date/time into one of these — by-date queries can return several games at once),
 * for a gameType-filtered query like last-results-per-game this always holds exactly one
 * EuroJackpot draw, with its numbers in `results[0]`.
 */
@Serializable
data class DrawResultsGroupedDto(
    val drawSystemId: Long? = null,
    val drawDate: String? = null,
    val gameType: String? = null,
    val multiplierValue: Int? = null,
    val results: List<DrawResultItemDto>? = null,
    val showSpecialResults: Boolean = false,
    val isNewEuroJackpotDraw: Boolean = false
)

/** GET /api/open/v1/lotteries/draw-results/by-date-per-game — paginated response envelope. */
@Serializable
data class PaginatedDrawResultsDto(
    val code: Int = 0,
    val message: String? = null,
    val showMorePossible: Boolean? = null,
    val totalRows: Int = 0,
    val items: List<DrawResultsGroupedDto>? = null
)

/** One country's share of a prize tier, within [PrizeTierDto.prizesValues]. */
@Serializable
data class CountryPrizeDto(
    // Country name as returned by the API, e.g. "Polska", "Niemcy" — not an ISO code,
    // and not translated; see LottoOpenApiRemoteDataSource for the Polish→display mapping.
    val country: String? = null,
    val prizeValue: Double? = null
)

/**
 * One prize tier (e.g. "5+2 matches") for a draw: how many winners there were in total
 * and, for EuroJackpot specifically, which country each winning ticket was sold in.
 */
@Serializable
data class PrizeTierDto(
    val degree: Long? = null,
    val countWinners: Long? = null,
    val prizesValues: List<CountryPrizeDto>? = null
)

/** One tier's entry in [EuroJackpotPrizesDto.prizes] — Poland's own national payout for
 *  that tier on this specific draw. [prize] is the winner *count* despite the generic
 *  name (verified live: rises monotonically from tier 1 down to tier 12, exactly the
 *  shape you'd expect from rarest/priciest to most-common/cheapest — see
 *  [com.eurojackpotai.app.domain.model.PrizeBreakdown]'s doc comment for the full
 *  reasoning and its one open caveat). */
@Serializable
data class DrawPrizeAmountDto(
    val prize: Int? = null,
    val prizeValue: Double? = null
)

/**
 * GET /api/open/v1/lotteries/draw-prizes/eurojackpot/{drawSystemId}
 *
 * IMPORTANT: the published OpenAPI spec's 200 response schema for this endpoint says it
 * returns a single object, but a real authenticated call returns a JSON *array*
 * containing exactly one object instead — confirmed directly against the live API, not
 * just the docs (the same "don't trust the spec blindly" lesson this app already learned
 * once from a stale magayo BuildConfig value). Modeled as a list to match reality;
 * [LottoOpenApiRemoteDataSource] takes `firstOrNull()`.
 */
@Serializable
data class EuroJackpotPrizesDto(
    val drawDate: String? = null,
    val drawSystemId: Long? = null,
    val gameType: String? = null,
    val prizesEmpty: Boolean = true,
    val countriesPrizes: List<PrizeTierDto>? = null,
    // Keyed "1".."12" — Poland's own official per-tier winner count + PLN prize value
    // for this draw (the swagger spec just calls the whole thing "Wygrane"/"Winnings",
    // no further field docs). See DrawPrizeAmountDto and PrizeBreakdown's doc comment.
    val prizes: Map<String, DrawPrizeAmountDto>? = null
)
