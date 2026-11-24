package com.eurojackpotai.app.domain.model

/**
 * The accumulated ("rollover") jackpot on offer for the *next*, not-yet-drawn
 * EuroJackpot draw — the number shown on Home as "누적 당첨금". This grows every time a
 * draw passes with no Tier-1 winner, which is why it's fetched live from
 * [com.eurojackpotai.app.data.remote.RemoteLotteryDataSource.fetchNextJackpot] rather
 * than stored alongside past [DrawResult]s: it changes even between draws, and once a
 * draw happens it stops being "next" and becomes that draw's (historical) jackpot.
 *
 * All fields are nullable because the underlying magayo call can fail (no API key
 * configured, offline, rate limited) — [amount] is null exactly when there's nothing
 * honest to show, never a placeholder like 0.
 */
data class NextJackpot(
    val amount: Long?,
    val currency: String?,
    val nextDrawDate: String?
)
