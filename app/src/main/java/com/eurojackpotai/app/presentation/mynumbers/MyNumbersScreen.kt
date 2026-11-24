package com.eurojackpotai.app.presentation.mynumbers

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eurojackpotai.app.EuroJackpotApp
import com.eurojackpotai.app.R
import com.eurojackpotai.app.core.GenericViewModelFactory
import com.eurojackpotai.app.core.util.DateFormatters
import com.eurojackpotai.app.core.util.TicketPhotoStore
import com.eurojackpotai.app.domain.model.NumberSource
import com.eurojackpotai.app.domain.model.SavedNumber
import com.eurojackpotai.app.presentation.components.LotteryCombinationRow
import com.eurojackpotai.app.presentation.components.SectionCard

/**
 * Plain `ActivityResultContracts.TakePicture()` builds its capture Intent as just
 * `Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri)` — it
 * does NOT grant the launched camera app permission to actually WRITE to that `content://`
 * Uri. Android's automatic URI-permission-granting for a launched activity keys off
 * `Intent.data`/`Intent.clipData`, not an arbitrary Parcelable extra like `EXTRA_OUTPUT`,
 * so without doing this ourselves, many camera apps silently fail to persist the photo:
 * the user sees the normal capture-and-confirm screen, confirms, the write throws inside
 * the camera app, and it returns `RESULT_CANCELED` — which, from this screen's side,
 * looks exactly like nothing happened at all (reported directly: "사진 확인 이후
 * 아무반응없이 넘어감" — this was reported again even *after* the `rememberSaveable` fix
 * for the separate process-death issue, which ruled that one out as the full story).
 *
 * Wrapping the stock contract to add `FLAG_GRANT_WRITE_URI_PERMISSION` (so the camera app
 * can save into it) + `FLAG_GRANT_READ_URI_PERMISSION` (so this app can read the result
 * back) plus a `ClipData` carrying the same Uri — `ClipData`, not just the flags, is what
 * Android's grant mechanism actually inspects for an extra that isn't `Intent.data` —
 * fixes this for whichever camera app ends up handling the intent, without this app having
 * to enumerate every installed camera app and call `grantUriPermission` on each one by hand.
 */
private class GrantingTakePicture : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("ticket_scan_capture", input)
        }
}

@Composable
fun MyNumbersScreen() {
    val app = LocalContext.current.applicationContext as EuroJackpotApp
    val viewModel: MyNumbersViewModel = viewModel(
        factory = GenericViewModelFactory {
            MyNumbersViewModel(app.container.savedNumberRepository, app.container.ticketTextRecognizer)
        }
    )
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // "Scan Ticket" via camera uses ActivityResultContracts.TakePicture() (a real
    // content:// output Uri, via TicketPhotoStore.createScanCaptureUri/FileProvider) —
    // NOT TakePicturePreview(), which this used to use. TakePicturePreview() only ever
    // hands back a tiny preview-quality Bitmap regardless of the phone's actual camera
    // resolution, which isn't enough detail for ML Kit to reliably read a ticket's small
    // printed numbers (reported directly: "사진으로 종이 인식이 잘 안되" — OCR wasn't
    // working well from camera photos specifically). TakePicture() captures at full
    // resolution instead; decodeForRecognition then safely downsamples that for ML Kit
    // while saveFromUri keeps the existing smaller stored copy for display.
    //
    // IMPORTANT: this Uri MUST survive process death, not just recomposition. While the
    // external camera app is in the foreground taking the photo, Android is free to kill
    // this app's process entirely to reclaim memory (common on real devices, and more
    // likely now that the camera is capturing full-resolution photos instead of a tiny
    // preview) — reported directly: "사진만 찍고 아무 동작이 없음" (takes the photo, then
    // nothing happens). The ActivityResultRegistry that delivers TakePicture()'s callback
    // is itself process-death-safe and still fires correctly after the process restarts,
    // but a plain `remember` here is NOT — it resets to null on the fresh recomposition,
    // so by the time the callback runs, `uri != null` fails silently and startScan() is
    // never called. `rememberSaveable` (Uri is Parcelable, so Compose's default Saver
    // handles it with no custom Saver needed) persists this across that process restart,
    // so the callback can still match it up with the right capture.
    var scanCaptureUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    val scanCameraLauncher = rememberLauncherForActivityResult(
        remember { GrantingTakePicture() }
    ) { captured ->
        val uri = scanCaptureUri
        if (captured && uri != null) {
            val bitmap = TicketPhotoStore.decodeForRecognition(context, uri)
            val photoPath = TicketPhotoStore.saveFromUri(context, uri)
            if (bitmap != null) {
                viewModel.startScan(photoPath, bitmap)
            }
        }
        uri?.let { TicketPhotoStore.deleteCaptureFile(context, it) }
        scanCaptureUri = null
    }
    // Gallery picks were already full/higher resolution (no TakePicturePreview-style
    // downscaling involved), so this wasn't the reported problem — switched to the same
    // decodeForRecognition/saveFromUri pair as the camera path above anyway, both for
    // consistency and because a plain, unsampled BitmapFactory.decodeStream() on a large
    // gallery photo risked an avoidable OutOfMemoryError that decodeForRecognition's
    // inSampleSize guards against.
    val scanGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bitmap = TicketPhotoStore.decodeForRecognition(context, uri)
            if (bitmap != null) {
                viewModel.startScan(TicketPhotoStore.saveFromUri(context, uri), bitmap)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (!state.isSelectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    // Camera is the primary scan entry point (the common case: photograph
                    // a ticket right after buying it) — scanning an existing gallery photo
                    // is offered as a smaller text button up in the header row instead of
                    // a third stacked FAB, which would crowd this corner of the screen.
                    FloatingActionButton(
                        onClick = {
                            val uri = TicketPhotoStore.createScanCaptureUri(context)
                            scanCaptureUri = uri
                            scanCameraLauncher.launch(uri)
                        },
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(Icons.Filled.DocumentScanner, contentDescription = stringResource(R.string.my_numbers_scan_from_camera))
                    }
                    FloatingActionButton(onClick = { viewModel.openAddDialog() }) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.my_numbers_add))
                    }
                }
            }
        }
    ) { padding ->
        if (state.savedNumbers.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.my_numbers_title), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.my_numbers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                GalleryScanButton(
                    onClick = {
                        scanGalleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.my_numbers_title), style = MaterialTheme.typography.headlineMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!state.isSelectionMode) {
                                GalleryScanButton(
                                    onClick = {
                                        scanGalleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                            TextButton(onClick = { viewModel.toggleSelectionMode() }) {
                                Text(
                                    if (state.isSelectionMode) {
                                        stringResource(R.string.my_numbers_select_cancel)
                                    } else {
                                        stringResource(R.string.my_numbers_select)
                                    }
                                )
                            }
                        }
                    }
                }
                if (state.isSelectionMode) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.my_numbers_selected_count, state.selectedIds.size),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        if (state.selectedIds.size == state.savedNumbers.size) {
                                            viewModel.clearSelection()
                                        } else {
                                            viewModel.selectAll()
                                        }
                                    }
                                ) {
                                    Text(
                                        if (state.selectedIds.size == state.savedNumbers.size) {
                                            stringResource(R.string.my_numbers_select_none)
                                        } else {
                                            stringResource(R.string.my_numbers_select_all)
                                        }
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.requestBulkDelete() },
                                    enabled = state.selectedIds.isNotEmpty()
                                ) {
                                    Text(stringResource(R.string.my_numbers_delete_selected))
                                }
                            }
                        }
                    }
                }
                items(state.savedNumbers, key = { it.id }) { saved ->
                    SavedNumberCard(
                        saved = saved,
                        onToggleActive = { viewModel.setActive(saved.id, it) },
                        onDelete = { viewModel.requestDelete(saved.id) },
                        isSelectionMode = state.isSelectionMode,
                        isSelected = saved.id in state.selectedIds,
                        onToggleSelected = { viewModel.toggleSelected(saved.id) }
                    )
                }
            }
        }
    }

    if (state.isAddDialogOpen) {
        AddNumberDialog(state = state, viewModel = viewModel)
    }

    state.deleteConfirmId?.let {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.my_numbers_delete_confirm_title)) },
            text = { Text(stringResource(R.string.my_numbers_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (state.isBulkDeleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelBulkDelete() },
            title = { Text(stringResource(R.string.my_numbers_bulk_delete_confirm_title, state.selectedIds.size)) },
            text = { Text(stringResource(R.string.my_numbers_bulk_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmBulkDelete() }) { Text(stringResource(R.string.common_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelBulkDelete() }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }
}

@Composable
private fun SavedNumberCard(
    saved: SavedNumber,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelected: () -> Unit = {}
) {
    SectionCard(
        modifier = if (isSelectionMode) Modifier.clickable { onToggleSelected() } else Modifier
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleSelected() }
                    )
                }
                Column {
                    Text(saved.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = when (saved.source) {
                            NumberSource.MANUAL -> stringResource(R.string.my_numbers_source_manual)
                            NumberSource.GENERATED -> stringResource(R.string.my_numbers_source_generated)
                            NumberSource.OCR_SCAN -> stringResource(R.string.my_numbers_source_ocr)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // The whole point of this line: which draw/round this combination
                    // was saved for, so entries from different weeks aren't confused
                    // with each other. Both are optional and only shown if the user set
                    // them — never a guessed value.
                    val drawTag = buildDrawTag(saved)
                    if (drawTag != null) {
                        Text(
                            text = drawTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (!isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = saved.isActive, onCheckedChange = onToggleActive)
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.common_delete))
                    }
                }
            }
        }
        LotteryCombinationRow(
            mainNumbers = saved.sortedMain,
            euroNumbers = saved.sortedEuro,
            ballSize = 32.dp,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (saved.note.isNotBlank()) {
            Text(
                text = saved.note,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (saved.ticketPhotoPath != null) {
            TicketPhotoThumbnail(path = saved.ticketPhotoPath, modifier = Modifier.padding(top = 8.dp))
        }
        Text(
            text = stringResource(R.string.my_numbers_created_on, DateFormatters.formatDisplay(saved.createdAt.toLocalDate())),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** "9/22 (Tue)" and/or a free-text round like "26-038", joined with " · " — or null when
 *  the user didn't tag either, in which case nothing is shown (never a fallback guess). */
@Composable
private fun buildDrawTag(saved: SavedNumber): String? {
    val parts = mutableListOf<String>()
    saved.targetDrawDate?.let {
        parts += stringResource(R.string.my_numbers_target_draw, DateFormatters.formatShortWithWeekday(it))
    }
    if (saved.drawRoundLabel.isNotBlank()) {
        parts += stringResource(R.string.my_numbers_round_label, saved.drawRoundLabel)
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** Small tappable preview of a ticket photo attached to a saved number; tapping opens
 *  it larger in a dialog. Decoded straight from disk (no image-loading library in this
 *  project) — fine for the handful of small, already-downsized ticket photos this
 *  produces (see TicketPhotoStore). */
@Composable
private fun TicketPhotoThumbnail(path: String, modifier: Modifier = Modifier) {
    var isViewerOpen by remember { mutableStateOf(false) }
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = stringResource(R.string.my_numbers_ticket_photo_label),
            modifier = modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .clickable { isViewerOpen = true }
        )
        if (isViewerOpen) {
            Dialog(onDismissRequest = { isViewerOpen = false }) {
                Image(
                    bitmap = it,
                    contentDescription = stringResource(R.string.my_numbers_ticket_photo_label),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .clickable { isViewerOpen = false }
                )
            }
        }
    }
}

/** Small text button offering "Scan Ticket" from an existing gallery photo — the
 *  camera FAB covers the common case (photograph a ticket right after buying it), so
 *  this lives in the header instead of a third stacked FAB, for the less common case
 *  of scanning a photo already on the phone. */
@Composable
private fun GalleryScanButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) {
        Icon(
            Icons.Filled.DocumentScanner,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp)
        )
        Text(stringResource(R.string.my_numbers_scan_from_gallery))
    }
}
