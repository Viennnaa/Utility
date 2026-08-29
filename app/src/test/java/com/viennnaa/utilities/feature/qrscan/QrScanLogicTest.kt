package com.viennnaa.utilities.feature.qrscan

import com.viennnaa.utilities.core.qr.encodeQr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrScanLogicTest {

    /** Renders a generated code into a luminance plane, as a camera would. */
    private fun luminanceOf(text: String, rowPadding: Int = 0): Triple<ByteArray, Int, Int> {
        val matrix = encodeQr(text, size = 300)!!
        val width = matrix.width
        val height = matrix.height
        val stride = width + rowPadding
        val data = ByteArray(stride * height) { 255.toByte() }
        for (y in 0 until height) {
            for (x in 0 until width) {
                data[y * stride + x] = if (matrix.get(x, y)) 0 else 255.toByte()
            }
        }
        return Triple(data, width, height)
    }

    @Test
    fun `a generated code decodes back to its text`() {
        val text = "https://example.com/scan"
        val (data, width, height) = luminanceOf(text)
        assertEquals(text, decodeLuminance(newReader(), data, width, height))
    }

    @Test
    fun `decoding works when the camera pads each row`() {
        // Camera frames often have a row stride wider than the image itself.
        val text = "padded rows"
        val (data, width, height) = luminanceOf(text, rowPadding = 24)
        assertEquals(text, decodeLuminance(newReader(), data, width, height, rowStride = width + 24))
    }

    @Test
    fun `a reader can be used for more than one frame`() {
        val reader = newReader()
        for (text in listOf("first", "second", "third")) {
            val (data, width, height) = luminanceOf(text)
            assertEquals(text, decodeLuminance(reader, data, width, height))
        }
    }

    @Test
    fun `a blank frame decodes to nothing rather than failing`() {
        val data = ByteArray(100 * 100) { 255.toByte() }
        assertNull(decodeLuminance(newReader(), data, 100, 100))
    }

    @Test
    fun `a truncated frame is skipped rather than crashing`() {
        assertNull(decodeLuminance(newReader(), ByteArray(10), 100, 100))
    }

    @Test
    fun `nonsense dimensions are refused`() {
        val data = ByteArray(100)
        assertNull(decodeLuminance(newReader(), data, 0, 10))
        assertNull(decodeLuminance(newReader(), data, 10, 0))
        // A stride narrower than the image is impossible.
        assertNull(decodeLuminance(newReader(), data, 10, 10, rowStride = 5))
    }

    @Test
    fun `scanned text is classified by what it starts with`() {
        assertEquals(ScanKind.URL, kindOf("https://example.com"))
        assertEquals(ScanKind.URL, kindOf("HTTP://example.com"))
        assertEquals(ScanKind.EMAIL, kindOf("mailto:someone@example.com"))
        assertEquals(ScanKind.PHONE, kindOf("tel:+441234567890"))
        assertEquals(ScanKind.TEXT, kindOf("just some words"))
        assertEquals(ScanKind.TEXT, kindOf(""))
    }

    @Test
    fun `classification ignores surrounding whitespace`() {
        assertEquals(ScanKind.URL, kindOf("  https://example.com  "))
    }
}
