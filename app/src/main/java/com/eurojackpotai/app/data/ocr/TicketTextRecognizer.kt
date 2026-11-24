package com.eurojackpotai.app.data.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Thin wrapper around Google ML Kit's on-device Latin-script text recognizer — Polish
 * (the language on a Totalizator Sportowy EuroJackpot ticket) is Latin-script with
 * diacritics, which this recognizer handles adequately for the plain digits and ASCII
 * labels ([com.eurojackpotai.app.domain.usecase.ParseTicketTextUseCase] actually parses.
 *
 * Runs entirely on the phone — the ticket photo is never uploaded anywhere, there's no
 * API key and no per-scan cost. The recognition model itself is fetched once via
 * Google Play services the first time this runs on a device (needs network for that
 * one download) and is cached on-device after that.
 *
 * Returns only the raw recognized text; all ticket-specific parsing lives in
 * [com.eurojackpotai.app.domain.usecase.ParseTicketTextUseCase] so that logic can be
 * unit-tested without an on-device ML Kit model.
 */
class TicketTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Returns the recognized text (ML Kit's `Text.getText()`, newline-joined across
     *  blocks/lines) or null if recognition failed outright — a blurry/dark photo, or
     *  the on-device model isn't downloaded yet and there's no network to fetch it.
     *  Never throws. */
    suspend fun recognize(bitmap: Bitmap): String? = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result.text)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "recognize: ML Kit failed with ${e.javaClass.simpleName}: ${e.message}", e)
                if (continuation.isActive) continuation.resume(null)
            }
    }

    companion object {
        // Same tag the rest of the sync pipeline uses — filter Logcat by "EuroJackpotSync".
        private const val TAG = "EuroJackpotSync"
    }
}
