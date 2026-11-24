package com.eurojackpotai.app.domain.model

/**
 * The 12 standard EuroJackpot prize tiers (source: official EuroJackpot game rules).
 * Deliberately carries no monetary amounts — actual prize money varies draw to draw
 * and per country/currency, and must come from an official results feed rather than
 * being hardcoded here (see section 18/35 of the product spec and the README).
 */
enum class PrizeTier(val rank: Int, val mainMatches: Int, val euroMatches: Int) {
    TIER_1(1, 5, 2),
    TIER_2(2, 5, 1),
    TIER_3(3, 5, 0),
    TIER_4(4, 4, 2),
    TIER_5(5, 4, 1),
    TIER_6(6, 3, 2),
    TIER_7(7, 4, 0),
    TIER_8(8, 2, 2),
    TIER_9(9, 3, 1),
    TIER_10(10, 3, 0),
    TIER_11(11, 1, 2),
    TIER_12(12, 2, 1);

    companion object {
        fun forMatch(mainMatches: Int, euroMatches: Int): PrizeTier? =
            entries.find { it.mainMatches == mainMatches && it.euroMatches == euroMatches }

        /** LOTTO OpenAPI's draw-prizes/eurojackpot endpoint labels each tier with a
         *  plain "degree" integer (see PrizeBreakdownTier.degree) with no match-count
         *  fields alongside it. A live-verified sample draw's degree/winner-count shape
         *  (degree 2 → 4 winners, degree 3 → 13 winners, with the jackpot tier rolled
         *  over that draw) lines up with degree == [rank] here — degree 1 is the 5+2
         *  jackpot tier, degree 2 is 5+1, and so on — so this reuses the same 1..12
         *  ranking rather than inventing a second one. */
        fun forRank(rank: Int): PrizeTier? = entries.find { it.rank == rank }
    }
}
