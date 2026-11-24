package com.eurojackpotai.app.presentation.results

import androidx.annotation.StringRes
import com.eurojackpotai.app.R
import com.eurojackpotai.app.domain.model.EuroJackpotCountry

/** Maps the domain-layer [EuroJackpotCountry] enum to its localized (EN/KO) display
 *  name — kept in `presentation` rather than on the enum itself so `domain` never
 *  depends on Android resources. */
@StringRes
fun EuroJackpotCountry.labelRes(): Int = when (this) {
    EuroJackpotCountry.AUSTRIA -> R.string.country_austria
    EuroJackpotCountry.CROATIA -> R.string.country_croatia
    EuroJackpotCountry.DENMARK -> R.string.country_denmark
    EuroJackpotCountry.ESTONIA -> R.string.country_estonia
    EuroJackpotCountry.FINLAND -> R.string.country_finland
    EuroJackpotCountry.GERMANY -> R.string.country_germany
    EuroJackpotCountry.HUNGARY -> R.string.country_hungary
    EuroJackpotCountry.ICELAND -> R.string.country_iceland
    EuroJackpotCountry.IRELAND -> R.string.country_ireland
    EuroJackpotCountry.ITALY -> R.string.country_italy
    EuroJackpotCountry.LATVIA -> R.string.country_latvia
    EuroJackpotCountry.LITHUANIA -> R.string.country_lithuania
    EuroJackpotCountry.NETHERLANDS -> R.string.country_netherlands
    EuroJackpotCountry.NORWAY -> R.string.country_norway
    EuroJackpotCountry.POLAND -> R.string.country_poland
    EuroJackpotCountry.SLOVAKIA -> R.string.country_slovakia
    EuroJackpotCountry.SLOVENIA -> R.string.country_slovenia
    EuroJackpotCountry.SPAIN -> R.string.country_spain
    EuroJackpotCountry.SWEDEN -> R.string.country_sweden
}
