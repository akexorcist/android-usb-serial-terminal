package dev.akexorcist.terminal.usbspp.ui.devicelist

import kotlin.test.assertEquals
import org.junit.Test

class DeviceListScreenKtTest {

  @Test
  fun `toHex pads short values to four hex digits`() {
    assertEquals("0x0007", 7.toHex())
  }

  @Test
  fun `toHex uppercases hex letters`() {
    assertEquals("0x2341", 0x2341.toHex())
  }

  @Test
  fun `toHex does not truncate values longer than four hex digits`() {
    assertEquals("0x12345", 0x12345.toHex())
  }
}
