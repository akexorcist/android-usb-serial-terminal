package dev.akexorcist.terminal.usbspp.ui.terminal

import java.util.Calendar
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class TerminalScreenKtTest {
    @Test
    fun `last visible item is the final item`() {
        assertTrue(isScrolledToBottom(lastVisibleIndex = 9, totalItemsCount = 10))
    }

    @Test
    fun `last visible item is before the final item`() {
        assertFalse(isScrolledToBottom(lastVisibleIndex = 8, totalItemsCount = 10))
    }

    @Test
    fun `empty list with no visible item reports at bottom`() {
        assertTrue(isScrolledToBottom(lastVisibleIndex = -1, totalItemsCount = 0))
    }

    @Test
    fun `single item list with no visible item reports not at bottom`() {
        assertFalse(isScrolledToBottom(lastVisibleIndex = -1, totalItemsCount = 1))
    }

    @Test
    fun `toTimeLabel formats date and time with millisecond precision`() {
        val calendar = Calendar.getInstance(Locale.US).apply {
            clear()
            set(2026, Calendar.MARCH, 5, 9, 6, 3)
            set(Calendar.MILLISECOND, 7)
        }

        assertEquals("5 Mar 26, 09:06:03.007", calendar.timeInMillis.toTimeLabel())
    }

    @Test
    fun `toHexString formats each byte as two uppercase hex digits separated by spaces`() {
        val bytes = listOf(0x00.toByte(), 0xFF.toByte(), 0x0A.toByte())

        assertEquals("00 FF 0A", bytes.toHexString())
    }

    @Test
    fun `toHexString of an empty list is an empty string`() {
        assertEquals("", emptyList<Byte>().toHexString())
    }
}
