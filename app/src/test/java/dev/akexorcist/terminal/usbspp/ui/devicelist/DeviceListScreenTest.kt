package dev.akexorcist.terminal.usbspp.ui.devicelist

import io.kotest.matchers.shouldBe
import org.junit.Test

class DeviceListScreenTest {

    @Test
    fun `toHex pads short values to four hex digits`() {
        7.toHex() shouldBe "0x0007"
    }

    @Test
    fun `toHex uppercases hex letters`() {
        0x2341.toHex() shouldBe "0x2341"
    }

    @Test
    fun `toHex does not truncate values longer than four hex digits`() {
        0x12345.toHex() shouldBe "0x12345"
    }
}
