package dev.akexorcist.terminal.usbspp.data

import com.hoho.android.usbserial.driver.UsbSerialPort
import dev.akexorcist.terminal.usbspp.domain.Parity
import kotlin.test.assertEquals
import org.junit.Test

class UsbSerialRepositoryImplKtTest {

  @Test
  fun `parity none maps to the driver constant`() {
    assertEquals(UsbSerialPort.PARITY_NONE, Parity.NONE.toUsbParity())
  }

  @Test
  fun `parity even maps to the driver constant`() {
    assertEquals(UsbSerialPort.PARITY_EVEN, Parity.EVEN.toUsbParity())
  }

  @Test
  fun `parity odd maps to the driver constant`() {
    assertEquals(UsbSerialPort.PARITY_ODD, Parity.ODD.toUsbParity())
  }

  @Test
  fun `stop bits of 2 maps to the two-stop-bit driver constant`() {
    assertEquals(UsbSerialPort.STOPBITS_2, 2.toUsbStopBits())
  }

  @Test
  fun `stop bits of 1 falls back to the one-stop-bit driver constant`() {
    assertEquals(UsbSerialPort.STOPBITS_1, 1.toUsbStopBits())
  }

  @Test
  fun `an unrecognized stop bits value falls back to the one-stop-bit driver constant`() {
    assertEquals(UsbSerialPort.STOPBITS_1, 0.toUsbStopBits())
  }
}
