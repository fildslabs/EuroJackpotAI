package com.eurojackpotai.app.core.util

import com.eurojackpotai.app.domain.model.SupportedCurrency
import java.util.Locale

/** Shared amount-formatting helpers used by Home (jackpot) and Results (prize
 *  breakdown) — kept in one place so the two screens can't drift into inconsistent
 *  formatting for the same kind of number. */
object CurrencyFormat {

    /** Formats an amount in its *native* currency as returned by the remote data
     *  source (PLN from LOTTO OpenAPI) — "€1,234" for EUR specifically, "1,234 CODE"
     *  for anything else, matching this app's original display convention. */
    fun formatNative(amount: Long, currencyCode: String?): String {
        val code = currencyCode?.trim()?.uppercase(Locale.US)
        return if (code == null || code == "EUR") {
            String.format(Locale.US, "€%,d", amount)
        } else {
            String.format(Locale.US, "%,d %s", amount, code)
        }
    }

    /** Formats an amount already converted into [currency] (see CurrencyRepository) —
     *  rounded to whole units. A EuroJackpot jackpot/prize is always in the thousands
     *  to hundreds-of-millions range in every currency this app offers, so a minor
     *  unit (cents) would only add noise. */
    fun formatConverted(amount: Double, currency: SupportedCurrency): String =
        String.format(Locale.US, "%s%,d", currency.symbol, Math.round(amount))

    /** [amountPln] converted using [ratesPerPln] (see CurrencyRepository.getRatesPerPln)
     *  and formatted with [formatConverted]. Null when [currency] is PLN itself (no
     *  conversion needed — callers should just show the native amount instead) or no
     *  rate is available. */
    fun convertAndFormat(amountPln: Double, currency: SupportedCurrency, ratesPerPln: Map<String, Double>?): String? {
        if (currency == SupportedCurrency.PLN) return null
        val rate = ratesPerPln?.get(currency.code) ?: return null
        return formatConverted(amountPln * rate, currency)
    }

    /** Same idea as [convertAndFormat], but for an amount that's natively in EUR rather
     *  than PLN — specifically [com.eurojackpotai.app.domain.model.CountryPrize.prizeValueEur]
     *  (see its doc comment for why that field is EUR, not PLN, despite living on the
     *  same LOTTO OpenAPI response as several genuinely-PLN fields). [ratesPerPln] is
     *  still PLN-based (see CurrencyRepository.getRatesPerPln) — there's no separate
     *  EUR-based rate table — so this first converts [amountEur] to its PLN equivalent
     *  via `ratesPerPln["EUR"]` (EUR per 1 PLN, so PLN-per-EUR is its reciprocal), then
     *  applies the normal PLN→[currency] rate from the same table. Null when [currency]
     *  is EUR itself (no conversion needed — the native amount already is the one to
     *  show) or no EUR/target rate is available. */
    fun convertAndFormatFromEur(amountEur: Double, currency: SupportedCurrency, ratesPerPln: Map<String, Double>?): String? {
        if (currency == SupportedCurrency.EUR) return null
        val eurPerPln = ratesPerPln?.get(SupportedCurrency.EUR.code) ?: return null
        if (eurPerPln <= 0.0) return null
        val amountPln = amountEur / eurPerPln
        if (currency == SupportedCurrency.PLN) return formatConverted(amountPln, currency)
        val rate = ratesPerPln[currency.code] ?: return null
        return formatConverted(amountPln * rate, currency)
    }
}
