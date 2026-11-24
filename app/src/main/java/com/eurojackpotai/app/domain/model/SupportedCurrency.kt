package com.eurojackpotai.app.domain.model

/**
 * Currencies the jackpot/prize amounts (natively in PLN — see
 * [com.eurojackpotai.app.data.remote.LottoOpenApiRemoteDataSource]) can be converted
 * to and displayed in, via the free Frankfurter exchange-rate API
 * (see [com.eurojackpotai.app.data.remote.CurrencyRateRemoteDataSource]). A small,
 * fixed list rather than all ~30 currencies Frankfurter supports — this is a picker
 * meant to fit inline on the Home screen (no Settings screen exists yet), not a full
 * currency search.
 */
enum class SupportedCurrency(val code: String, val symbol: String) {
    EUR("EUR", "€"),
    USD("USD", "$"),
    KRW("KRW", "₩"),
    GBP("GBP", "£"),
    JPY("JPY", "¥"),
    PLN("PLN", "zł");

    companion object {
        val DEFAULT = EUR

        fun fromCode(code: String?): SupportedCurrency? {
            val normalized = code?.trim()?.uppercase() ?: return null
            return entries.find { it.code == normalized }
        }
    }
}
