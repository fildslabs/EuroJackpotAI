package com.eurojackpotai.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class NumberSource {
    MANUAL,
    GENERATED,
    // Numbers/draw date came from ParseTicketTextUseCase reading a scanned ticket
    // photo (see MyNumbersViewModel.startScan) — the user still reviewed/could edit
    // every field before saving, same as MANUAL, but the label makes clear where the
    // starting values came from.
    OCR_SCAN
}

/**
 * A combination the user has chosen to keep, either typed in by hand or produced by
 * one of the [GenerationMode]s. Kept separate from [DrawResult] because a saved number
 * is user data (editable, deletable) while a draw result is an immutable historical fact.
 */
data class SavedNumber(
    val id: Long = 0,
    val name: String,
    val mainNumbers: List<Int>,
    val euroNumbers: List<Int>,
    val source: NumberSource,
    val generationMode: GenerationMode? = null,
    val note: String = "",
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    // Which draw this combination is meant for, so multiple saved combinations across
    // different weeks can be told apart in the list. Set by the user at save time —
    // null means "not tagged to a draw", never a guessed/computed date, since a saved
    // number can predate or outlive any one draw.
    val targetDrawDate: LocalDate? = null,
    // The official draw/round number as printed on the user's physical ticket (e.g.
    // "26-038"). Always free text: a real Totalizator Sportowy EuroJackpot ticket
    // doesn't actually print a round number at all (it prints a draw date/time —
    // see targetDrawDate and ParseTicketTextUseCase's doc comment), and this app has
    // no verified source for official EuroJackpot round numbering, so this field
    // stays whatever the user typed by hand and is never auto-filled by the scanner.
    val drawRoundLabel: String = "",
    // Absolute path to a locally-stored reference photo of the physical ticket this
    // combination came from (see TicketPhotoStore). Attached either as a plain memory
    // aid (manual entry) or automatically when [source] is [NumberSource.OCR_SCAN] —
    // either way it's just the photo shown back to the user; it is never re-read or
    // re-parsed after save. mainNumbers/euroNumbers/targetDrawDate above reflect
    // whatever the user confirmed in the add dialog, whether they typed it or a scan
    // suggested it and they accepted/edited it.
    val ticketPhotoPath: String? = null
) {
    init {
        require(mainNumbers.size == LotteryRules.MAIN_NUMBERS_COUNT)
        require(mainNumbers.toSet().size == mainNumbers.size)
        require(mainNumbers.all { it in LotteryRules.MAIN_NUMBER_RANGE })
        require(euroNumbers.size == LotteryRules.EURO_NUMBERS_COUNT)
        require(euroNumbers.toSet().size == euroNumbers.size)
        require(euroNumbers.all { it in LotteryRules.EURO_NUMBER_RANGE })
    }

    val sortedMain: List<Int> get() = mainNumbers.sorted()
    val sortedEuro: List<Int> get() = euroNumbers.sorted()
}
