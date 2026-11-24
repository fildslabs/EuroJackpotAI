package com.eurojackpotai.app.presentation.mynumbers

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eurojackpotai.app.core.util.DrawSchedule
import com.eurojackpotai.app.core.util.NumberValidation
import com.eurojackpotai.app.core.util.TicketPhotoStore
import com.eurojackpotai.app.data.ocr.TicketTextRecognizer
import com.eurojackpotai.app.domain.model.LotteryRules
import com.eurojackpotai.app.domain.model.NumberSource
import com.eurojackpotai.app.domain.model.SavedNumber
import com.eurojackpotai.app.domain.repository.SavedNumberRepository
import com.eurojackpotai.app.domain.usecase.ParseTicketTextUseCase
import com.eurojackpotai.app.domain.usecase.ParsedTicket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class MyNumbersViewModel(
    private val repository: SavedNumberRepository,
    private val ticketTextRecognizer: TicketTextRecognizer
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyNumbersUiState())
    val uiState: StateFlow<MyNumbersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAll().collect { list ->
                _uiState.update { it.copy(savedNumbers = list) }
            }
        }
    }

    fun openAddDialog() {
        _uiState.update {
            it.copy(
                isAddDialogOpen = true,
                nameInput = "",
                noteInput = "",
                selectedMain = emptySet(),
                selectedEuro = emptySet(),
                validationErrors = emptyList(),
                // Defaults to the very next draw — the common case is buying for it —
                // but the user can pick a different upcoming date or clear it entirely.
                selectedDrawDate = DrawSchedule.nextDrawDateTime().toLocalDate(),
                drawRoundInput = "",
                ticketPhotoPath = null,
                isScanFlow = false,
                isScanningTicket = false,
                ticketScanStatus = null,
                scannedBoards = emptyList(),
                currentScanBoardIndex = 0,
                savedScanBoardIndices = emptySet()
            )
        }
    }

    /** Entry point for "Scan Ticket" (see MyNumbersScreen). Opens the same add dialog
     *  used for manual entry, but in a "scanning" state with every number field
     *  cleared (never a stale value carried over from a previous scan) while OCR runs
     *  in the background, then fills in whatever [ParseTicketTextUseCase] could read
     *  — see [applyParsedTicket]. [photoPath] (already saved via TicketPhotoStore by
     *  the caller, which has the Context this ViewModel deliberately doesn't hold) is
     *  attached immediately so the user has the source photo to double-check against
     *  even before OCR finishes. */
    fun startScan(photoPath: String?, bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                isAddDialogOpen = true,
                nameInput = "",
                noteInput = "",
                selectedMain = emptySet(),
                selectedEuro = emptySet(),
                validationErrors = emptyList(),
                // Left unset rather than defaulted to "next draw": the scanned ticket
                // may be for a past or already-known draw, and applyParsedTicket fills
                // this in from the ticket itself when it can.
                selectedDrawDate = null,
                drawRoundInput = "",
                ticketPhotoPath = photoPath,
                isScanFlow = true,
                isScanningTicket = true,
                ticketScanStatus = null,
                scannedBoards = emptyList(),
                currentScanBoardIndex = 0,
                savedScanBoardIndices = emptySet()
            )
        }
        viewModelScope.launch {
            val rawText = ticketTextRecognizer.recognize(bitmap)
            applyParsedTicket(rawText?.let { ParseTicketTextUseCase(it) } ?: ParsedTicket())
        }
    }

    private fun applyParsedTicket(parsed: ParsedTicket) {
        val firstBoard = parsed.boards.firstOrNull()
        _uiState.update { state ->
            state.copy(
                isScanningTicket = false,
                scannedBoards = parsed.boards,
                currentScanBoardIndex = 0,
                selectedMain = firstBoard?.mainNumbers.orEmpty().toSet(),
                selectedEuro = firstBoard?.euroNumbers.orEmpty().toSet(),
                selectedDrawDate = parsed.drawDate,
                // Never invented as a "round number" (see SavedNumber.drawRoundLabel's
                // doc comment) — surfaced as an editable note instead, so nothing the
                // scan found is lost but nothing is mislabeled either.
                noteInput = parsed.ticketReference
                    ?.let { "티켓 참조번호(단말기): $it" }
                    ?: state.noteInput,
                ticketScanStatus = TicketScanStatus(
                    mainRecognized = firstBoard?.mainNumbers?.isNotEmpty() == true,
                    euroRecognized = firstBoard?.euroNumbers?.isNotEmpty() == true,
                    dateRecognized = parsed.drawDate != null,
                    boardIndex = 0,
                    // Even a failed scan (no board at all) is shown as "board 1 of 1"
                    // rather than "of 0" — there's nothing to switch between either way.
                    boardCount = parsed.boards.size.coerceAtLeast(1)
                )
            )
        }
    }

    /** Lets the user switch which scanned board's numbers are loaded into the add
     *  dialog's editable fields, without re-scanning — e.g. to look at board 2 before
     *  deciding which to save first. See [scannedBoards][MyNumbersUiState.scannedBoards]
     *  and [save] (which advances through boards automatically as each is saved). */
    fun selectScanBoard(index: Int) {
        val board = _uiState.value.scannedBoards.getOrNull(index) ?: return
        _uiState.update { state ->
            state.copy(
                currentScanBoardIndex = index,
                selectedMain = board.mainNumbers.toSet(),
                selectedEuro = board.euroNumbers.toSet(),
                validationErrors = emptyList(),
                ticketScanStatus = state.ticketScanStatus?.copy(
                    mainRecognized = board.mainNumbers.isNotEmpty(),
                    euroRecognized = board.euroNumbers.isNotEmpty(),
                    boardIndex = index
                )
            )
        }
    }

    fun closeAddDialog() {
        // If the user attached a ticket photo and then canceled instead of saving,
        // don't leave the file orphaned on disk.
        TicketPhotoStore.delete(_uiState.value.ticketPhotoPath)
        _uiState.update {
            it.copy(
                isAddDialogOpen = false,
                ticketPhotoPath = null,
                isScanFlow = false,
                isScanningTicket = false,
                ticketScanStatus = null,
                scannedBoards = emptyList(),
                currentScanBoardIndex = 0,
                savedScanBoardIndices = emptySet()
            )
        }
    }

    fun updateName(value: String) = _uiState.update { it.copy(nameInput = value) }
    fun updateNote(value: String) = _uiState.update { it.copy(noteInput = value) }

    fun selectDrawDate(date: LocalDate) {
        _uiState.update { state ->
            // Tapping the already-selected date clears it — tagging a draw is optional.
            state.copy(selectedDrawDate = if (state.selectedDrawDate == date) null else date)
        }
    }

    fun updateDrawRoundInput(value: String) = _uiState.update { it.copy(drawRoundInput = value) }

    fun setTicketPhotoPath(path: String?) {
        // Replacing a photo (or clearing it) should not leave the previous file behind.
        val previous = _uiState.value.ticketPhotoPath
        if (previous != null && previous != path) TicketPhotoStore.delete(previous)
        _uiState.update { it.copy(ticketPhotoPath = path) }
    }

    fun toggleMain(number: Int) {
        _uiState.update { state ->
            val current = state.selectedMain
            val updated = when {
                number in current -> current - number
                current.size < LotteryRules.MAIN_NUMBERS_COUNT -> current + number
                else -> current // already full — extra taps are ignored rather than erroring
            }
            state.copy(selectedMain = updated)
        }
    }

    fun toggleEuro(number: Int) {
        _uiState.update { state ->
            val current = state.selectedEuro
            val updated = when {
                number in current -> current - number
                current.size < LotteryRules.EURO_NUMBERS_COUNT -> current + number
                else -> current
            }
            state.copy(selectedEuro = updated)
        }
    }

    /** Saves the currently-shown numbers (manual entry, or whichever scanned board is
     *  loaded — see [selectScanBoard]). For a multi-board scan with more boards still
     *  unsaved, this does NOT close the dialog: it saves the current board, then loads
     *  the next not-yet-saved board (by [MyNumbersUiState.savedScanBoardIndices], not
     *  just "currentScanBoardIndex + 1" — the user may have jumped between boards out
     *  of order via the chips before saving any of them) into the editable fields so
     *  it can be reviewed and saved in turn, same as the first — letting every board
     *  on one ticket be added without re-scanning. Only once every board is saved does
     *  the dialog close, same as before this multi-board handling existed. */
    fun save() {
        val state = _uiState.value
        val main = state.selectedMain.toList()
        val euro = state.selectedEuro.toList()
        val errors = NumberValidation.validateMain(main) + NumberValidation.validateEuro(euro)
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(validationErrors = errors) }
            return
        }
        val savedIndices = if (state.isScanFlow) {
            state.savedScanBoardIndices + state.currentScanBoardIndex
        } else {
            state.savedScanBoardIndices
        }
        val remainingBoardIndex = state.scannedBoards.indices.firstOrNull { it !in savedIndices }
        val hasMoreBoards = state.isScanFlow && remainingBoardIndex != null
        // When another board from this same scan still needs saving, give it its own
        // independent copy of the photo file right now (see TicketPhotoStore
        // .duplicate's doc comment) — never let the record about to be created below
        // and the next board's future record end up pointing at the exact same file,
        // or deleting one later would silently break the other's photo.
        val nextPhotoPath = if (hasMoreBoards) TicketPhotoStore.duplicate(state.ticketPhotoPath) else null
        viewModelScope.launch {
            repository.save(
                SavedNumber(
                    name = state.nameInput.ifBlank { "My Numbers" },
                    mainNumbers = main,
                    euroNumbers = euro,
                    source = if (state.isScanFlow) NumberSource.OCR_SCAN else NumberSource.MANUAL,
                    note = state.noteInput,
                    targetDrawDate = state.selectedDrawDate,
                    drawRoundLabel = state.drawRoundInput.trim(),
                    ticketPhotoPath = state.ticketPhotoPath
                )
            )
            if (hasMoreBoards && remainingBoardIndex != null) {
                val nextBoard = state.scannedBoards[remainingBoardIndex]
                _uiState.update {
                    it.copy(
                        savedScanBoardIndices = savedIndices,
                        currentScanBoardIndex = remainingBoardIndex,
                        selectedMain = nextBoard.mainNumbers.toSet(),
                        selectedEuro = nextBoard.euroNumbers.toSet(),
                        validationErrors = emptyList(),
                        ticketPhotoPath = nextPhotoPath,
                        ticketScanStatus = it.ticketScanStatus?.copy(
                            mainRecognized = nextBoard.mainNumbers.isNotEmpty(),
                            euroRecognized = nextBoard.euroNumbers.isNotEmpty(),
                            boardIndex = remainingBoardIndex
                        )
                        // isAddDialogOpen is left untouched (stays true) — the dialog
                        // doesn't close between boards of the same scan.
                    )
                }
            } else {
                // Every board saved (or this was plain manual entry) — close like
                // before. The photo (if any) now belongs to the just-saved record —
                // clear the staging field without deleting the file this time.
                _uiState.update {
                    it.copy(
                        isAddDialogOpen = false,
                        ticketPhotoPath = null,
                        isScanFlow = false,
                        isScanningTicket = false,
                        ticketScanStatus = null,
                        scannedBoards = emptyList(),
                        currentScanBoardIndex = 0,
                        savedScanBoardIndices = emptySet()
                    )
                }
            }
        }
    }

    fun setActive(id: Long, isActive: Boolean) {
        viewModelScope.launch { repository.setActive(id, isActive) }
    }

    fun requestDelete(id: Long) = _uiState.update { it.copy(deleteConfirmId = id) }
    fun cancelDelete() = _uiState.update { it.copy(deleteConfirmId = null) }

    fun confirmDelete() {
        val id = _uiState.value.deleteConfirmId ?: return
        val photoPath = _uiState.value.savedNumbers.find { it.id == id }?.ticketPhotoPath
        viewModelScope.launch {
            repository.delete(id)
            TicketPhotoStore.delete(photoPath)
            _uiState.update { it.copy(deleteConfirmId = null) }
        }
    }

    // --- Multi-select bulk delete ---------------------------------------------------

    fun toggleSelectionMode() {
        _uiState.update { state ->
            if (state.isSelectionMode) {
                // Leaving selection mode clears whatever was selected.
                state.copy(isSelectionMode = false, selectedIds = emptySet())
            } else {
                state.copy(isSelectionMode = true)
            }
        }
    }

    fun toggleSelected(id: Long) {
        _uiState.update { state ->
            val current = state.selectedIds
            val updated = if (id in current) current - id else current + id
            state.copy(selectedIds = updated)
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            state.copy(selectedIds = state.savedNumbers.map { it.id }.toSet())
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun requestBulkDelete() {
        if (_uiState.value.selectedIds.isEmpty()) return
        _uiState.update { it.copy(isBulkDeleteConfirmOpen = true) }
    }

    fun cancelBulkDelete() = _uiState.update { it.copy(isBulkDeleteConfirmOpen = false) }

    fun confirmBulkDelete() {
        val ids = _uiState.value.selectedIds
        if (ids.isEmpty()) {
            _uiState.update { it.copy(isBulkDeleteConfirmOpen = false) }
            return
        }
        val photoPaths = _uiState.value.savedNumbers.filter { it.id in ids }.mapNotNull { it.ticketPhotoPath }
        viewModelScope.launch {
            repository.deleteMany(ids)
            photoPaths.forEach { TicketPhotoStore.delete(it) }
            _uiState.update {
                it.copy(
                    isBulkDeleteConfirmOpen = false,
                    isSelectionMode = false,
                    selectedIds = emptySet()
                )
            }
        }
    }
}
