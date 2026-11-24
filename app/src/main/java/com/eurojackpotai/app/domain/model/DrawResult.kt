package com.eurojackpotai.app.domain.model

import java.time.LocalDate

/**
 * One official EuroJackpot draw. [mainNumbers] always has 5 entries in 1..50,
 * [euroNumbers] always has 2 entries in 1..12 — enforced at construction so a bad
 * value can never silently reach the UI or statistics engine.
 */
data class DrawResult(
    val id: Long = 0,
    val date: LocalDate,
    val mainNumbers: List<Int>,
    val euroNumbers: List<Int>,
    val jackpotEur: Long? = null,
    val isSampleData: Boolean = false
) {
    init {
        require(mainNumbers.size == LotteryRules.MAIN_NUMBERS_COUNT) {
            "A draw must have exactly ${LotteryRules.MAIN_NUMBERS_COUNT} main numbers, got ${mainNumbers.size}"
        }
        require(mainNumbers.toSet().size == mainNumbers.size) { "Main numbers must not repeat" }
        require(mainNumbers.all { it in LotteryRules.MAIN_NUMBER_RANGE }) {
            "Main numbers must be within ${LotteryRules.MAIN_NUMBER_RANGE}"
        }
        require(euroNumbers.size == LotteryRules.EURO_NUMBERS_COUNT) {
            "A draw must have exactly ${LotteryRules.EURO_NUMBERS_COUNT} euro numbers, got ${euroNumbers.size}"
        }
        require(euroNumbers.toSet().size == euroNumbers.size) { "Euro numbers must not repeat" }
        require(euroNumbers.all { it in LotteryRules.EURO_NUMBER_RANGE }) {
            "Euro numbers must be within ${LotteryRules.EURO_NUMBER_RANGE}"
        }
    }

    val sortedMain: List<Int> get() = mainNumbers.sorted()
    val sortedEuro: List<Int> get() = euroNumbers.sorted()
    val sum: Int get() = mainNumbers.sum()
}

/** Central place for the current EuroJackpot format so a future rule change touches one file. */
object LotteryRules {
    const val MAIN_NUMBERS_COUNT = 5
    const val EURO_NUMBERS_COUNT = 2
    val MAIN_NUMBER_RANGE = 1..50
    val EURO_NUMBER_RANGE = 1..12
}
