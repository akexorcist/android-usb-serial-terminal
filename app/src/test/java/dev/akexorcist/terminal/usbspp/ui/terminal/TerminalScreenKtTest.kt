package dev.akexorcist.terminal.usbspp.ui.terminal

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
}
