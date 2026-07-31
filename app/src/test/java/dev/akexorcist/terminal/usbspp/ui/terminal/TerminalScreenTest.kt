package dev.akexorcist.terminal.usbspp.ui.terminal

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test

class TerminalScreenTest {

    @Test
    fun `last visible item is the final item`() {
        isScrolledToBottom(lastVisibleIndex = 9, totalItemsCount = 10) shouldBe true
    }

    @Test
    fun `last visible item is before the final item`() {
        isScrolledToBottom(lastVisibleIndex = 8, totalItemsCount = 10) shouldBe false
    }

    @Test
    fun `empty list with no visible item reports at bottom`() {
        isScrolledToBottom(lastVisibleIndex = -1, totalItemsCount = 0) shouldBe true
    }

    @Test
    fun `single item list with no visible item reports not at bottom`() {
        isScrolledToBottom(lastVisibleIndex = -1, totalItemsCount = 1) shouldBe false
    }

    @Test
    fun `toTimeLabel formats date and time with millisecond precision`() {
        val dateTime = LocalDateTime(2026, Month.MARCH, 5, 9, 6, 3, nanosecond = 7_000_000)
        val millis = dateTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        millis.toTimeLabel() shouldBe "5 Mar 26, 09:06:03.007"
    }

    @Test
    fun `toHexString formats each byte as two uppercase hex digits separated by spaces`() {
        val bytes = listOf(0x00.toByte(), 0xFF.toByte(), 0x0A.toByte())

        bytes.toHexString() shouldBe "00 FF 0A"
    }

    @Test
    fun `toHexString of an empty list is an empty string`() {
        emptyList<Byte>().toHexString() shouldBe ""
    }
}
