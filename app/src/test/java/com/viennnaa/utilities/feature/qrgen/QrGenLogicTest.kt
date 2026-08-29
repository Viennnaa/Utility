package com.viennnaa.utilities.feature.qrgen

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.LuminanceSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Reads a BitMatrix back as brightness, so a generated code can be decoded. */
private class MatrixLuminance(private val matrix: BitMatrix) :
    LuminanceSource(matrix.width, matrix.height) {

    override fun getRow(y: Int, row: ByteArray?): ByteArray {
        val out = row?.takeIf { it.size >= width } ?: ByteArray(width)
        for (x in 0 until width) out[x] = if (matrix.get(x, y)) 0 else 255.toByte()
        return out
    }

    override fun getMatrix(): ByteArray {
        val out = ByteArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                out[y * width + x] = if (matrix.get(x, y)) 0 else 255.toByte()
            }
        }
        return out
    }
}

private fun decode(matrix: BitMatrix): String? = try {
    MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(MatrixLuminance(matrix))))?.text
} catch (e: Exception) {
    null
}

class QrGenLogicTest {

    @Test
    fun `an encoded code reads back as the same text`() {
        val text = "https://example.com/hello"
        val matrix = encodeQr(text)!!
        assertEquals(text, decode(matrix))
    }

    @Test
    fun `non ascii text survives the round trip`() {
        // Without the UTF-8 hint ZXing falls back to ISO-8859-1 and mangles these.
        for (text in listOf("café", "naïve résumé", "中文测试", "emoji 🎲 here")) {
            val matrix = encodeQr(text)!!
            assertEquals(text, decode(matrix))
        }
    }

    @Test
    fun `every correction level round trips`() {
        val text = "correction level check"
        for (correction in Correction.entries) {
            val matrix = encodeQr(text, correction = correction)!!
            assertEquals("$correction", text, decode(matrix))
        }
    }

    @Test
    fun `higher correction makes a denser code for the same text`() {
        val text = "the same text at both levels"
        // Compared at the module level rather than the rendered size, which is
        // fixed by the caller.
        val low = encodeQr(text, size = 1, correction = Correction.LOW)!!
        val high = encodeQr(text, size = 1, correction = Correction.HIGH)!!
        assertTrue("low=${low.width} high=${high.width}", high.width >= low.width)
    }

    @Test
    fun `the matrix is square`() {
        val matrix = encodeQr("square", size = 256)!!
        assertEquals(matrix.width, matrix.height)
    }

    @Test
    fun `empty text has nothing to encode`() {
        assertNull(encodeQr(""))
    }

    @Test
    fun `text past the cap is refused`() {
        assertNull(encodeQr("x".repeat(MAX_CONTENT_LENGTH + 1)))
    }

    @Test
    fun `a nonsense size is refused rather than throwing`() {
        assertNull(encodeQr("hello", size = 0))
        assertNull(encodeQr("hello", size = -10))
    }

    @Test
    fun `text too dense for the chosen correction returns null rather than throwing`() {
        // Comfortably inside the length cap but beyond what fits at H.
        assertNull(encodeQr("x".repeat(1_500), correction = Correction.HIGH))
    }

    @Test
    fun `a long but encodable payload still works`() {
        val text = "u".repeat(300)
        assertNotNull(encodeQr(text, correction = Correction.LOW))
    }

    @Test
    fun `pixels come back one per module in row order`() {
        val matrix = encodeQr("pixels", size = 64)!!
        val pixels = matrixToPixels(matrix, on = 0xFF000000.toInt(), off = 0xFFFFFFFF.toInt())
        assertEquals(matrix.width * matrix.height, pixels.size)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                val expected = if (matrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                assertEquals(expected, pixels[y * matrix.width + x])
            }
        }
    }
}
