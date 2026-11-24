package com.eurojackpotai.app.presentation.mynumbers

import com.eurojackpotai.app.core.util.NumberSelectionError
import com.eurojackpotai.app.domain.model.SavedNumber
import com.eurojackpotai.app.domain.usecase.ParsedBoard
import java.time.LocalDate

/** Per-field recognition results for the board currently shown in the add dialog, so
 *  the dialog can show the user exactly what was (and wasn't) read off the photo —
 *  never a silent guess-or-blank. Present only while the dialog is in "scan" mode (see
 *  MyNumbersUiState.isScanFlow); null in the plain manual-add flow.
 *
 *  [boardIndex]/[boardCount] describe where the currently-shown board sits among every
 *  board the scan found (see MyNumbersUiState.scannedBoards) — a multi-board ticket
 *  (buying several combinations on one slip) used to only ever extract board 1, with
 *  the rest merely flagged as present; every board is now parsed, and the dialog lets
 *  the user switch between them (see MyNumbersViewModel.selectScanBoard) to review and
 *  save each one in turn. */
data class TicketScanStatus(
    val mainRecognized: Boolean,
    val euroRecognized: Boolean,
    val dateRecognized: Boolean,
    val boardIndex: Int = 0,
    val boardCount: Int = 1
) {
    val additionalBoardsDetected: Boolean get() = boardCount > 1
}

data class MyNumbersUiState(
    val savedNumbers: List<SavedNumber> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val nameInput: String = "",
    val noteInput: String = "",
    val selectedMain: Set<Int> = emptySet(),
    val selectedEuro: Set<Int> = emptySet(),
    val validationErrors: List<NumberSelectionError> = emptyList(),
    val deleteConfirmId: Long? = null,
    // Which upcoming draw this combination is being saved for — lets multiple saved
    // combinations across different weeks be told apart in the list. Null is a valid
    // choice (not tagging a draw), so the add dialog defaults it to the next draw but
    // lets the user clear it.
    val selectedDrawDate: LocalDate? = null,
    // Free-text official round number as printed on a physical ticket (e.g. "26-038").
    val drawRoundInput: String = "",
    // Absolute path of a ticket photo attached while the add dialog is open (see
    // TicketPhotoStore) — staged here until save() persists it onto the SavedNumber, or
    // discarded (file deleted) if the dialog is canceled instead.
    val ticketPhotoPath: String? = null,
    // Multi-select "bulk delete" mode — separate from deleteConfirmId (single-item
    // delete keeps working the same way while this is off).
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet(),
    val isBulkDeleteConfirmOpen: Boolean = false,
    // Ticket-scan flow (see MyNumbersViewModel.startScan) reuses this same add dialog
    // rather than a separate screen, distinguished by these three fields.
    // isScanFlow decides save()'s NumberSource (OCR_SCAN vs MANUAL) even after the
    // user has edited every field by hand.
    val isScanFlow: Boolean = false,
    val isScanningTicket: Boolean = false,
    val ticketScanStatus: TicketScanStatus? = null,
    // Every board the last ticket scan found (see ParseTicketTextUseCase) — usually
    // just one, but a multi-board ticket has more than one. currentScanBoardIndex is
    // which of these is currently loaded into selectedMain/selectedEuro above; save()
    // advances it to the next not-yet-saved board instead of closing the dialog when
    // more remain (see MyNumbersViewModel.save/selectScanBoard). Both reset to empty/0
    // once the scan flow ends (dialog closed or every board saved).
    val scannedBoards: List<ParsedBoard> = emptyList(),
    val currentScanBoardIndex: Int = 0,
    // Indices into scannedBoards already persisted as their own SavedNumber this scan
    // — lets save() find the next board still needing a save (not just
    // currentScanBoardIndex + 1) even if the user jumped between boards out of order
    // via the chips in AddNumberDialog before saving any of them.
    val savedScanBoardIndices: Set<Int> = emptySet()
)
