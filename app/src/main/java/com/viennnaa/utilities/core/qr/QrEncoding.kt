package com.viennnaa.utilities.core.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR encoding, shared by the mini apps that produce codes: the QR generator and
 * the WiFi one.
 *
 * ZXing is a plain JVM library, so all of this is testable without a device —
 * including encoding a code and reading it straight back.
 */

/**
 * A QR code tops out well below this, but the writer reports that by throwing,
 * which is slower and noisier than refusing obvious nonsense up front.
 */
const val MAX_CONTENT_LENGTH = 2000

/** Module count of the smallest code, used as the default render size. */
const val DEFAULT_SIZE = 512

/**
 * How much of the code can be damaged and still read. More correction means a
 * denser code for the same text, which is the trade the user is making.
 */
enum class Correction(val level: ErrorCorrectionLevel) {
    LOW(ErrorCorrectionLevel.L),
    MEDIUM(ErrorCorrectionLevel.M),
    QUARTILE(ErrorCorrectionLevel.Q),
    HIGH(ErrorCorrectionLevel.H),
}

/**
 * Encodes [text] as a square QR matrix, or null when there is nothing to encode
 * or the text will not fit.
 *
 * Content is encoded as UTF-8 so accented characters and emoji survive; without
 * the hint ZXing falls back to ISO-8859-1 and mangles them.
 */
fun encodeQr(
    text: String,
    size: Int = DEFAULT_SIZE,
    correction: Correction = Correction.MEDIUM,
): BitMatrix? {
    if (text.isEmpty() || text.length > MAX_CONTENT_LENGTH || size <= 0) return null
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to correction.level,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
    )
    return try {
        QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
    } catch (e: WriterException) {
        // Thrown when the text cannot fit at this correction level.
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Flattens a matrix into ARGB pixels, row by row, for handing to an image.
 *
 * @param on colour of a dark module, @param off colour of a light one.
 */
fun matrixToPixels(matrix: BitMatrix, on: Int, off: Int): IntArray {
    val pixels = IntArray(matrix.width * matrix.height)
    for (y in 0 until matrix.height) {
        val row = y * matrix.width
        for (x in 0 until matrix.width) {
            pixels[row + x] = if (matrix.get(x, y)) on else off
        }
    }
    return pixels
}
