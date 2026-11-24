package com.eurojackpotai.app.domain.model

/**
 * The 19 countries that currently sell EuroJackpot tickets. Used to translate the
 * Polish country names LOTTO OpenAPI returns in `draw-prizes/eurojackpot/{id}`
 * (`countriesPrizes[].prizesValues[].country`, e.g. "Niemcy", "Holandia") into a
 * stable identity the presentation layer can map to a localized (EN/KO) label —
 * see `presentation/results/CountryLabels.kt` for that mapping.
 *
 * Kept in `domain` rather than `data` because it's a fact about EuroJackpot itself
 * (which countries participate), not about any one API's response shape.
 */
enum class EuroJackpotCountry(val polishName: String) {
    AUSTRIA("Austria"),
    CROATIA("Chorwacja"),
    DENMARK("Dania"),
    ESTONIA("Estonia"),
    FINLAND("Finlandia"),
    GERMANY("Niemcy"),
    HUNGARY("Węgry"),
    ICELAND("Islandia"),
    IRELAND("Irlandia"),
    ITALY("Włochy"),
    LATVIA("Łotwa"),
    LITHUANIA("Litwa"),
    NETHERLANDS("Holandia"),
    NORWAY("Norwegia"),
    POLAND("Polska"),
    SLOVAKIA("Słowacja"),
    SLOVENIA("Słowenia"),
    SPAIN("Hiszpania"),
    SWEDEN("Szwecja");

    companion object {
        /** Matches by the Polish name LOTTO OpenAPI returns; unknown/unmapped names
         *  (a future new member country the API starts reporting before this enum is
         *  updated) return null rather than a guess — callers skip those entries
         *  rather than showing a raw untranslated Polish string in the UI. */
        fun fromPolishName(name: String?): EuroJackpotCountry? {
            val trimmed = name?.trim() ?: return null
            return entries.find { it.polishName.equals(trimmed, ignoreCase = true) }
        }
    }
}
