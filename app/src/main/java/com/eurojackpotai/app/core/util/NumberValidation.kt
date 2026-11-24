package com.eurojackpotai.app.core.util

import com.eurojackpotai.app.domain.model.LotteryRules

sealed class NumberSelectionError {
    data class WrongMainCount(val selected: Int) : NumberSelectionError()
    data class WrongEuroCount(val selected: Int) : NumberSelectionError()
    data class OutOfRange(val value: Int, val range: IntRange) : NumberSelectionError()
    data class Duplicate(val value: Int) : NumberSelectionError()
}

/** Validates a manually-entered combination before it can be saved. Used by both the
 * manual entry screen and the (future) OCR pipeline, so both paths get the same rules. */
object NumberValidation {

    fun validateMain(numbers: List<Int>): List<NumberSelectionError> =
        validate(numbers, LotteryRules.MAIN_NUMBERS_COUNT, LotteryRules.MAIN_NUMBER_RANGE) {
            NumberSelectionError.WrongMainCount(numbers.size)
        }

    fun validateEuro(numbers: List<Int>): List<NumberSelectionError> =
        validate(numbers, LotteryRules.EURO_NUMBERS_COUNT, LotteryRules.EURO_NUMBER_RANGE) {
            NumberSelectionError.WrongEuroCount(numbers.size)
        }

    private inline fun validate(
        numbers: List<Int>,
        expectedCount: Int,
        range: IntRange,
        countError: () -> NumberSelectionError
    ): List<NumberSelectionError> {
        val errors = mutableListOf<NumberSelectionError>()
        if (numbers.size != expectedCount) errors += countError()
        numbers.forEach { if (it !in range) errors += NumberSelectionError.OutOfRange(it, range) }
        numbers.groupingBy { it }.eachCount().filter { it.value > 1 }.keys.forEach {
            errors += NumberSelectionError.Duplicate(it)
        }
        return errors
    }

    fun isValidCombination(main: List<Int>, euro: List<Int>): Boolean =
        validateMain(main).isEmpty() && validateEuro(euro).isEmpty()
}
