package com.eurojackpotai.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Persists a reference photo of a physical lottery ticket bought outside the app (a
 * different retailer, a printed slip, etc.) alongside a saved number. This is a photo
 * attachment only — there is no OCR/auto-recognition here. The user still types the
 * numbers in by hand via [com.eurojackpotai.app.presentation.mynumbers.AddNumberDialog];
 * the photo is only ever shown back to the user as-is, so they can recognize which
 * physical ticket a saved combination came from.
 *
 * Images are copied into the app's private files directory (no runtime permission
 * needed to read them back, never shared outside the app) and downsized to a small
 * JPEG, so a handful of ticket photos never meaningfully add to app storage.
 */
object TicketPhotoStore {
    private const val DIR_NAME = "ticket_photos"
    private const val CAPTURE_CACHE_DIR_NAME = "ticket_scan_captures"
    private const val MAX_DIMENSION = 1280
    // Ticket scanning (OCR) needs noticeably more detail than the reference-photo
    // thumbnail above to read small printed digits reliably — see
    // decodeForRecognition's doc comment for why this exists as a separate constant.
    private const val MAX_RECOGNITION_DIMENSION = 2048
    private const val JPEG_QUALITY = 85

    /** Saves the bitmap handed back directly by the camera app via
     *  `ActivityResultContracts.TakePicturePreview` — already a small preview-sized
     *  bitmap, so no FileProvider/output-Uri plumbing is needed for capture. Returns the
     *  absolute path to the saved file, or null if writing failed. Only used for the
     *  plain manual "attach a reference photo" flow (AddNumberDialog) — ticket *scanning*
     *  (MyNumbersScreen's camera FAB) uses [createScanCaptureUri] instead, which captures
     *  at full resolution; see that function's doc comment for why. */
    fun saveBitmap(context: Context, bitmap: Bitmap): String? =
        runCatching { writeBitmap(context, downscale(bitmap)) }.getOrNull()

    /** Creates an empty file in the app's cache dir and returns a `content://` Uri for it
     *  via FileProvider (see res/xml/file_paths.xml and the `<provider>` entry in
     *  AndroidManifest.xml), suitable for `ActivityResultContracts.TakePicture()`.
     *
     *  Ticket scanning specifically needs this — NOT `TakePicturePreview()` — because
     *  `TakePicturePreview()` only ever returns a tiny preview-quality Bitmap (historically
     *  as small as a few hundred px on a side, regardless of the phone's actual camera
     *  resolution), which is fine for a reference photo a person just glances at, but is
     *  nowhere near enough detail for ML Kit to reliably read the small printed numbers on
     *  a lottery ticket (reported directly: "사진으로 종이 인식이 잘 안되" — OCR from the
     *  photo wasn't working well). `TakePicture()` with a real output Uri captures at the
     *  camera's full resolution instead. The returned Uri's backing file should be deleted
     *  with [deleteCaptureFile] once it's no longer needed (after OCR + saving a resized
     *  copy via [saveFromUri]), since it's a full-resolution, uncompressed-by-this-app copy
     *  that would otherwise sit in the cache dir. */
    fun createScanCaptureUri(context: Context): Uri {
        val dir = File(context.cacheDir, CAPTURE_CACHE_DIR_NAME).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    /** Deletes the temporary full-resolution capture file behind a Uri from
     *  [createScanCaptureUri]. Best-effort, like [delete] — a failed cleanup just leaves
     *  one orphaned cache file behind, not a crash, and the cache dir is something Android
     *  can reclaim on its own under storage pressure regardless. */
    fun deleteCaptureFile(context: Context, uri: Uri) {
        runCatching { context.contentResolver.delete(uri, null, null) }
    }

    /** Decodes a higher-detail (but still bounded, to stay OOM-safe on a 12+ MP camera
     *  photo) Bitmap from [uri] for [com.eurojackpotai.app.data.ocr.TicketTextRecognizer]
     *  to read — deliberately less downscaled than [saveFromUri]'s stored copy (see
     *  MAX_RECOGNITION_DIMENSION vs. MAX_DIMENSION), since recognition accuracy on small
     *  printed digits benefits from extra resolution in a way a photo a person just looks
     *  at does not. Uses `inSampleSize` (power-of-two downsampling during decode itself,
     *  not after) so a full-resolution camera photo is never fully decoded into memory
     *  needlessly. Returns null if reading/decoding failed. */
    fun decodeForRecognition(context: Context, uri: Uri): Bitmap? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // BUG THIS FIXED: BitmapFactory.decodeStream() ALWAYS returns null when
        // inJustDecodeBounds is true (by Android API contract — the whole point of this
        // pass is only the side effect of populating bounds.outWidth/outHeight, never a
        // Bitmap). An earlier version of this function wrote
        // `...openInputStream(uri)?.use { decodeStream(...) } ?: return null` directly on
        // this bounds-only call, which meant that always-null return value was
        // misread as "the stream failed to open" and this function returned null
        // unconditionally, on every single call, for every photo — OCR scanning (camera
        // AND gallery alike) silently never ran at all. Checking openInputStream's own
        // nullability separately (the only thing that genuinely indicates a failed-to-open
        // stream) fixes this; the actual bounds-populating decode call below is now
        // unconditional.
        val boundsStream = context.contentResolver.openInputStream(uri) ?: return null
        boundsStream.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }

        val largestSide = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (largestSide / (sampleSize * 2) >= MAX_RECOGNITION_DIMENSION) {
            sampleSize *= 2
        }

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: return null

        // inSampleSize only halves repeatedly — the result can still be somewhat above
        // MAX_RECOGNITION_DIMENSION, so finish with the same exact-size scale saveBitmap
        // already uses for the stored copy, just against the higher recognition cap.
        val resultLargestSide = maxOf(decoded.width, decoded.height)
        if (resultLargestSide <= MAX_RECOGNITION_DIMENSION) {
            decoded
        } else {
            val scale = MAX_RECOGNITION_DIMENSION.toFloat() / resultLargestSide
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        }
    }.getOrNull()

    /** Saves an image the user picked from the system photo picker. Reads and downsizes
     *  it immediately, since the picker's read grant on the returned Uri is not
     *  guaranteed to outlive this call. Returns the absolute path to the saved file, or
     *  null if reading/decoding/writing failed. */
    fun saveFromUri(context: Context, uri: Uri): String? = runCatching {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: return null
        writeBitmap(context, downscale(bitmap))
    }.getOrNull()

    /** Best-effort delete — called when a saved number (and its photo) is removed, or
     *  when a photo attached mid-add is discarded by canceling the add dialog. Never
     *  throws: a failed cleanup just leaves an orphaned file behind, not a crash. */
    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /** Makes an independent on-disk copy of an already-saved ticket photo (see
     *  [saveFromUri]/[saveBitmap]), right next to it, under a new random file name — no
     *  Context needed, since [path] already lives inside this app's own ticket-photos
     *  directory and copying is a plain file operation.
     *
     *  Used when a multi-board scan is saved as more than one
     *  [com.eurojackpotai.app.domain.model.SavedNumber] — one per board — from the same
     *  physical ticket photo (see
     *  [com.eurojackpotai.app.presentation.mynumbers.MyNumbersViewModel.save]). Every
     *  one of those saved numbers "shows the same picture", but each needs its OWN file
     *  on disk: [delete] permanently removes the underlying file, and if two saved
     *  numbers pointed at the literal same path, deleting one would silently break the
     *  other's photo too. Returns null if the copy failed (including when [path] is
     *  null/blank or no longer exists). */
    fun duplicate(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return runCatching {
            val source = File(path)
            if (!source.isFile) return null
            val copy = File(source.parentFile, "${UUID.randomUUID()}.jpg")
            source.copyTo(copy, overwrite = false)
            copy.absolutePath
        }.getOrNull()
    }

    private fun downscale(bitmap: Bitmap): Bitmap {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide <= MAX_DIMENSION) return bitmap
        val scale = MAX_DIMENSION.toFloat() / largestSide
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun writeBitmap(context: Context, bitmap: Bitmap): String {
        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        }
        return file.absolutePath
    }
}
