package com.viennnaa.utilities.feature.qrscan

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * QR decoding from a camera frame.
 *
 * The camera hands over YUV, whose first plane is already the brightness of each
 * pixel — which is all a QR reader needs — so the frame is read as luminance
 * directly rather than being converted to RGB and back.
 */

/** A reader is stateful, so callers keep one rather than sharing across threads. */
fun newReader(): MultiFormatReader = MultiFormatReader().apply {
    setHints(
        mapOf(
            // QR only: letting it try every format makes each frame slower for
            // formats this mini app does not claim to read.
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        ),
    )
}

/**
 * Decodes the QR code in a luminance plane, or null when the frame has none —
 * which is the normal case for most frames, not an error.
 *
 * @param data the luminance plane, one byte per pixel, [rowStride] bytes per row.
 * @param rowStride bytes per row, which the camera may pad beyond [width].
 */
fun decodeLuminance(
    reader: MultiFormatReader,
    data: ByteArray,
    width: Int,
    height: Int,
    rowStride: Int = width,
): String? {
    if (width <= 0 || height <= 0 || rowStride < width) return null
    if (data.size < rowStride * height) return null
    return try {
        val source = PlanarYUVLuminanceSource(
            data,
            rowStride,
            height,
            0,
            0,
            width,
            height,
            false,
        )
        reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))?.text
    } catch (e: NotFoundException) {
        null
    } catch (e: ArrayIndexOutOfBoundsException) {
        // A truncated or oddly strided frame; skip it rather than crashing.
        null
    } finally {
        reader.reset()
    }
}

/** What a scanned string looks like it is, so the screen can offer the right action. */
enum class ScanKind { URL, EMAIL, PHONE, TEXT }

fun kindOf(text: String): ScanKind {
    val trimmed = text.trim()
    return when {
        trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> ScanKind.URL

        trimmed.startsWith("mailto:", ignoreCase = true) -> ScanKind.EMAIL
        trimmed.startsWith("tel:", ignoreCase = true) -> ScanKind.PHONE
        else -> ScanKind.TEXT
    }
}
