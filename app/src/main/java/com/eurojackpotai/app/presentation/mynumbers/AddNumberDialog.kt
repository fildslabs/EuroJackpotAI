package com.eurojackpotai.app.presentation.mynumbers

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.eurojackpotai.app.R
import com.eurojackpotai.app.core.util.DateFormatters
import com.eurojackpotai.app.core.util.DrawSchedule
import com.eurojackpotai.app.core.util.NumberSelectionError
import com.eurojackpotai.app.core.util.TicketPhotoStore
import com.eurojackpotai.app.domain.model.LotteryRules
import java.time.LocalDate

/** Next few upcoming draw dates offered as quick-pick chips — computed fresh each time
 *  the dialog opens, not stored anywhere, since it's just "today plus the next few
 *  Tuesdays/Fridays" and never needs to be looked up again. */
private fun upcomingDrawDates(count: Int = 4): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var candidate = DrawSchedule.nextDrawDateTime()
    repeat(count) {
        dates += candidate.toLocalDate()
        candidate = DrawSchedule.nextDrawDateTime(candidate.plusMinutes(1))
    }
    return dates
}

@Composable
fun AddNumberDialog(state: MyNumbersUiState, viewModel: MyNumbersViewModel) {
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            TicketPhotoStore.saveBitmap(context, bitmap)?.let { viewModel.setTicketPhotoPath(it) }
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            TicketPhotoStore.saveFromUri(context, uri)?.let { viewModel.setTicketPhotoPath(it) }
        }
    }

    Dialog(
        onDismissRequest = { viewModel.closeAddDialog() },
        // Default width is a fixed platform value that leaves this dialog's content
        // (name, both number pads, note, draw date, round number, photo, errors, save/
        // cancel) taller than most phone screens; without a scrollable container that
        // just clips it — which is why the photo section was invisible below the fold.
        // usePlatformDefaultWidth = false + an explicit fillMaxWidth below lets the
        // dialog use the screen width, and heightIn + verticalScroll below make
        // everything reachable by scrolling the dialog as a whole.
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 640.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (state.isScanFlow) {
                        stringResource(R.string.my_numbers_scan_review_title)
                    } else {
                        stringResource(R.string.my_numbers_add)
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                if (state.isScanFlow) {
                    TicketScanStatusBanner(
                        isScanning = state.isScanningTicket,
                        status = state.ticketScanStatus,
                        recognizedDrawDate = state.selectedDrawDate,
                        currentBoardIndex = state.currentScanBoardIndex,
                        savedBoardIndices = state.savedScanBoardIndices,
                        onSelectBoard = viewModel::selectScanBoard,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Column(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        value = state.nameInput,
                        onValueChange = viewModel::updateName,
                        label = { Text(stringResource(R.string.my_numbers_name_label)) },
                        placeholder = { Text(stringResource(R.string.my_numbers_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Text(
                    text = stringResource(R.string.my_numbers_pick_main),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                // A plain (non-lazy) grid, not LazyVerticalGrid: LazyVerticalGrid needs
                // a fixed pixel height and scrolls its own content internally once that
                // height is exceeded — a second, easy-to-miss scroll area nested inside
                // this already-scrollable dialog. There are only 50 (main) / 12 (euro)
                // cells, so laying them out directly and letting them take exactly the
                // height they need is simpler and guarantees every number is visible by
                // scrolling the dialog once, not hunting for a tiny inner scrollbar.
                NumberPad(
                    range = LotteryRules.MAIN_NUMBER_RANGE,
                    columns = 10,
                    selected = state.selectedMain,
                    onToggle = viewModel::toggleMain
                )

                Text(
                    text = stringResource(R.string.my_numbers_pick_euro),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                NumberPad(
                    range = LotteryRules.EURO_NUMBER_RANGE,
                    columns = 6,
                    selected = state.selectedEuro,
                    onToggle = viewModel::toggleEuro,
                    selectedColor = MaterialTheme.colorScheme.secondary
                )

                OutlinedTextField(
                    value = state.noteInput,
                    onValueChange = viewModel::updateNote,
                    label = { Text(stringResource(R.string.my_numbers_note_label)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )

                // Which draw + official round number this combination is for — this is
                // what lets entries in the list be told apart by week (see MyNumbersScreen).
                Text(
                    text = stringResource(R.string.my_numbers_draw_date_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                val quickPickDates = remember { upcomingDrawDates() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPickDates) { date ->
                        FilterChip(
                            selected = state.selectedDrawDate == date,
                            onClick = { viewModel.selectDrawDate(date) },
                            label = { Text(DateFormatters.formatShortWithWeekday(date)) }
                        )
                    }
                }
                // A scanned ticket's draw date (see TicketScanStatusBanner above) can
                // easily fall outside the "next few upcoming draws" quick-pick chips —
                // reviewing an old ticket, for instance — so show it plainly here too
                // rather than only via chip highlighting, which would show nothing selected.
                state.selectedDrawDate?.let { date ->
                    if (date !in quickPickDates) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.my_numbers_selected_draw_date, DateFormatters.formatShortWithWeekday(date)),
                                style = MaterialTheme.typography.labelMedium
                            )
                            TextButton(onClick = { viewModel.selectDrawDate(date) }) {
                                Text(stringResource(R.string.common_close))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.drawRoundInput,
                    onValueChange = viewModel::updateDrawRoundInput,
                    label = { Text(stringResource(R.string.my_numbers_round_input_label)) },
                    placeholder = { Text(stringResource(R.string.my_numbers_round_input_placeholder)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )

                // A reference photo of a physical ticket bought elsewhere — attachment
                // only, never OCR'd; the numbers above are always typed by hand.
                Text(
                    text = stringResource(R.string.my_numbers_ticket_photo_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                if (state.ticketPhotoPath != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        val bitmap = remember(state.ticketPhotoPath) {
                            BitmapFactory.decodeFile(state.ticketPhotoPath)?.asImageBitmap()
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = stringResource(R.string.my_numbers_ticket_photo_label),
                                modifier = Modifier
                                    .size(96.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            )
                        }
                        IconButton(
                            onClick = { viewModel.setTicketPhotoPath(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(28.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.my_numbers_remove_photo),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { cameraLauncher.launch(null) }) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(stringResource(R.string.my_numbers_take_photo))
                        }
                        OutlinedButton(
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                            Text(stringResource(R.string.my_numbers_choose_photo))
                        }
                    }
                }

                if (state.validationErrors.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        state.validationErrors.distinctBy { it::class }.forEach { error ->
                            Text(
                                text = errorMessage(error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Saving while another scanned board still needs review doesn't close
                // this dialog (see MyNumbersViewModel.save, which this mirrors) — the
                // button says so, so tapping it isn't mistaken for "done".
                val hasMoreBoards = state.isScanFlow &&
                    state.scannedBoards.indices.any { it !in state.savedScanBoardIndices + state.currentScanBoardIndex }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.closeAddDialog() }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(onClick = { viewModel.save() }, modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            if (hasMoreBoards) {
                                stringResource(R.string.my_numbers_save_and_next_board)
                            } else {
                                stringResource(R.string.common_save)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Lays [range] out as a fixed grid of [columns] columns, row by row — see the call
 *  sites above for why this isn't a LazyVerticalGrid. [range]'s size must be evenly
 *  divisible by [columns] (50/10 and 12/6 both are) so every row is full and no
 *  leftover-cell padding logic is needed. */
@Composable
private fun NumberPad(
    range: IntRange,
    columns: Int,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        range.toList().chunked(columns).forEach { rowNumbers ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowNumbers.forEach { number ->
                    SelectableNumberCell(
                        number = number,
                        isSelected = number in selected,
                        onClick = { onToggle(number) },
                        selectedColor = selectedColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableNumberCell(
    number: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .background(
                if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
                CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Shows what the last ticket scan did (a spinner while it's running) and did not
 *  manage to read, field by field — the whole point being that the user reviews and,
 *  where needed, corrects the number pads / draw date below rather than trusting OCR
 *  blindly (see ParseTicketTextUseCase's doc comment on why: even the reference ticket
 *  used to build this parser was first misread by one digit). Nothing here is ever a
 *  silent success/failure — every outcome is spelled out.
 *
 *  When the ticket has more than one board (buying several combinations on one slip),
 *  also shows a row of chips to switch which board's numbers are loaded below —
 *  [onSelectBoard] is [MyNumbersViewModel.selectScanBoard]. Every board is saved one
 *  at a time (see AddNumberDialog's save button and MyNumbersViewModel.save), so this
 *  is how the user reviews/reaches any board beyond the first, before or after saving
 *  earlier ones; [savedBoardIndices] marks which ones are already saved with a
 *  checkmark, since nothing stops the user jumping back to a board (or forward, past
 *  one not yet saved) via these chips. */
@Composable
private fun TicketScanStatusBanner(
    isScanning: Boolean,
    status: TicketScanStatus?,
    recognizedDrawDate: LocalDate?,
    currentBoardIndex: Int,
    savedBoardIndices: Set<Int>,
    onSelectBoard: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isScanning) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Text(
                    text = stringResource(R.string.my_numbers_scanning),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            return@Column
        }
        if (status == null) return@Column

        if (!status.mainRecognized && !status.euroRecognized && !status.dateRecognized) {
            ScanStatusLine(recognized = false, text = stringResource(R.string.my_numbers_scan_failed))
        } else {
            ScanStatusLine(
                recognized = status.mainRecognized,
                text = stringResource(
                    if (status.mainRecognized) R.string.my_numbers_scan_main_recognized else R.string.my_numbers_scan_main_not_recognized
                )
            )
            ScanStatusLine(
                recognized = status.euroRecognized,
                text = stringResource(
                    if (status.euroRecognized) R.string.my_numbers_scan_euro_recognized else R.string.my_numbers_scan_euro_not_recognized
                )
            )
            ScanStatusLine(
                recognized = status.dateRecognized,
                text = if (status.dateRecognized && recognizedDrawDate != null) {
                    stringResource(R.string.my_numbers_scan_date_recognized_format, DateFormatters.formatShortWithWeekday(recognizedDrawDate))
                } else {
                    stringResource(R.string.my_numbers_scan_date_not_recognized)
                }
            )
        }
        if (status.boardCount > 1) {
            Text(
                text = stringResource(R.string.my_numbers_scan_multiple_boards_warning, status.boardCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items((0 until status.boardCount).toList()) { index ->
                    FilterChip(
                        selected = currentBoardIndex == index,
                        onClick = { onSelectBoard(index) },
                        label = { Text(stringResource(R.string.my_numbers_scan_board_label, index + 1)) },
                        leadingIcon = if (index in savedBoardIndices) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanStatusLine(recognized: Boolean, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Icon(
            imageVector = if (recognized) Icons.Filled.Check else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (recognized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun errorMessage(error: NumberSelectionError): String = when (error) {
    is NumberSelectionError.WrongMainCount -> stringResource(R.string.my_numbers_validation_main_count)
    is NumberSelectionError.WrongEuroCount -> stringResource(R.string.my_numbers_validation_euro_count)
    // Both unreachable in practice: the grid only offers valid, non-repeating numbers,
    // but kept for defense-in-depth if selection ever comes from another input path (e.g. OCR).
    is NumberSelectionError.OutOfRange -> "${error.value} is out of range ${error.range}."
    is NumberSelectionError.Duplicate -> "${error.value} is selected more than once."
}
