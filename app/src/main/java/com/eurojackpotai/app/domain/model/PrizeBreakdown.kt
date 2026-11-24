package com.eurojackpotai.app.domain.model

import java.time.LocalDate

/**
 * One country's share of a [PrizeBreakdownTier]. [winners] is derived, not read
 * directly off the API: LOTTO OpenAPI's `prizesValues` array actually lists one entry
 * *per winning ticket* (confirmed against a live response — a tier with 4 winners
 * split 3/1 between two countries came back as 4 separate same-country-repeated
 * entries, not an aggregated count), so [com.eurojackpotai.app.data.remote.LottoOpenApiRemoteDataSource]
 * groups by country and counts occurrences to get a real per-country winner count.
 *
 * [prizeValueEur] — despite living on an endpoint whose other field ([TierPrizeAmount])
 * is genuinely in PLN, this one is in EUR, not PLN. Reasoning (there's no currency field
 * on the API response to just read off): `countriesPrizes` is a *cross-border* list — one
 * shared value covering winners across every participating country (Poland, Germany,
 * Denmark, etc.), each of which has its own native currency. A single unlabeled amount
 * meant to apply uniformly across countries with different currencies only makes sense
 * if it's already in EuroJackpot's common settlement currency, EUR. Confirmed directly:
 * for the 9 Oct 2026 draw, euro-jackpot.net's own per-tier EUR breakdown reported
 * €11,558,578.60 for the 5+1 tier and €180,126.60 for the 5+0 tier — the same order of
 * magnitude and, in earlier samples, the same uniform-across-countries value this app's
 * `countriesPrizes` data showed for those tiers. Treating that number as PLN produces an
 * implausibly small zloty amount for a jackpot-adjacent tier; treating it as EUR lines up
 * with the real published figures. See [TierPrizeAmount] for the field that actually is
 * PLN on this same response.
 */
data class CountryPrize(
    val country: EuroJackpotCountry,
    val winners: Int,
    val prizeValueEur: Double
)

/** One official prize tier (e.g. "5+2", the jackpot tier) for a single draw, broken
 *  down by which country each winning ticket was sold in. Only tiers LOTTO OpenAPI
 *  actually reports a country breakdown for are included — most tiers are paid at a
 *  fixed national amount and never get one (see [PrizeBreakdown] doc comment). */
data class PrizeBreakdownTier(
    val degree: Int,
    val totalWinners: Int,
    val countries: List<CountryPrize>
)

/** Poland's own official winner count + PLN prize amount for one tier of one draw (the
 *  `prizes` map on the same API response [PrizeBreakdownTier]/[CountryPrize] come from —
 *  see [PrizeBreakdown]'s doc comment for what this is and, importantly, what it is
 *  *not*). [winnersInPoland] of 0 means nobody in Poland actually won this tier that
 *  draw — [prizeValuePln] is then also 0 and must NOT be shown as "what you'd win": with
 *  no winners to split a pari-mutuel tier's pool, there is no real per-winner amount to
 *  report for that specific draw. */
data class TierPrizeAmount(
    val winnersInPoland: Int,
    val prizeValuePln: Double
)

/**
 * Per-country winner counts for one EuroJackpot draw, sourced from
 * `GET /api/open/v1/lotteries/draw-prizes/eurojackpot/{drawSystemId}`.
 *
 * Deliberately does NOT include city-level detail: verified against the full LOTTO
 * OpenAPI spec that the only endpoint carrying city/place data
 * (`/lotteries/highest-wins`) explicitly excludes EuroJackpot from its supported
 * `games` filter, and this endpoint's own schema has no place/city field at all. Per
 * this project's standing rule against showing fabricated data, the UI that renders
 * this model says so plainly instead of silently omitting or faking a "tap for city"
 * interaction (see `results_country_no_city_data` in strings.xml).
 *
 * Only some tiers carry a country breakdown at all — in a live-verified sample draw,
 * only the two pan-European jackpot-adjacent tiers (degree 2 and 3) had any
 * `countriesPrizes` entries; lower tiers are apparently paid at fixed national amounts
 * without needing a cross-border breakdown. [tiers] reflects that: it's whatever the
 * API actually returned data for, never padded out to all 12 tiers. Each [CountryPrize]
 * in it is in EUR, not PLN — see that class's doc comment for why.
 *
 * [tierPrizeAmounts] is a *separate* field from the same response, keyed by
 * [PrizeTier.rank] (1..12) via the same degree==rank inference [PrizeTier.forRank]
 * already documents — Poland's own official per-tier payout table for this draw,
 * covering all 12 tiers rather than just the two pan-European ones [tiers] covers. This
 * is genuinely useful for "did my saved number win, and how much" (My Numbers vs. the
 * latest draw), which is why it was added, but it comes with one open point worth being
 * explicit about: for a tier that *does* also appear in [tiers] (2 and 3), the two
 * amounts were checked against each other on a real draw and did **not** numerically
 * agree even after accounting for the currency difference below (tier 3's
 * `countriesPrizes` amount was uniform across every country as expected for a pooled
 * EUR tier, but [tierPrizeAmounts]'s PLN figure for the same tier/draw didn't come out to
 * a plausible EUR→PLN conversion of it either). Part of that gap is now explained — see
 * [CountryPrize.prizeValueEur]'s doc comment: [tiers] is in EUR while [tierPrizeAmounts]
 * is genuinely in PLN, so the two were never directly comparable numbers to begin with —
 * but the residual gap after converting units is still unreconciled (plausibly Poland's
 * "Multiplikator" add-on, tax handling, or a rounding/timing difference between the two
 * sub-objects of the same API response). Rather than silently pick one as "correct"
 * without being able to confirm the residual gap, this app treats [tierPrizeAmounts] as
 * Poland's own domestic-payout figure on its own terms — the right number for "what would
 * LOTTO Poland actually pay a Polish ticket for this tier" even if it isn't the same
 * concept, or even the same currency, as the cross-border reference amount in [tiers].
 */
data class PrizeBreakdown(
    val drawDate: LocalDate,
    val tiers: List<PrizeBreakdownTier>,
    val tierPrizeAmounts: Map<Int, TierPrizeAmount> = emptyMap()
)
