package dev.akexorcist.terminal.usbspp.data

import com.hoho.android.usbserial.driver.UsbSerialPort
import dev.akexorcist.terminal.usbspp.domain.Parity
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.fake.FakeUsbDataSource
import dev.akexorcist.terminal.usbspp.fake.fakeUsbConnection
import dev.akexorcist.terminal.usbspp.fake.fakeUsbDeviceConnection
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class UsbSerialRepositoryTest {

    @Test
    fun `parity none maps to the driver constant`() {
        Parity.NONE.toUsbParity() shouldBe UsbSerialPort.PARITY_NONE
    }

    @Test
    fun `parity even maps to the driver constant`() {
        Parity.EVEN.toUsbParity() shouldBe UsbSerialPort.PARITY_EVEN
    }

    @Test
    fun `parity odd maps to the driver constant`() {
        Parity.ODD.toUsbParity() shouldBe UsbSerialPort.PARITY_ODD
    }

    @Test
    fun `stop bits of 2 maps to the two-stop-bit driver constant`() {
        2.toUsbStopBits() shouldBe UsbSerialPort.STOPBITS_2
    }

    @Test
    fun `stop bits of 1 falls back to the one-stop-bit driver constant`() {
        1.toUsbStopBits() shouldBe UsbSerialPort.STOPBITS_1
    }

    @Test
    fun `an unrecognized stop bits value falls back to the one-stop-bit driver constant`() {
        0.toUsbStopBits() shouldBe UsbSerialPort.STOPBITS_1
    }

    @Test
    fun `connecting while already connected closes the previous session first`() = runTest {
        val dataSource = FakeUsbDataSource()
        dataSource.deviceConnection = fakeUsbDeviceConnection()
        val repository = UsbSerialRepositoryImpl(dataSource)

        val first = fakeUsbConnection(deviceId = 1)
        repository.connect(first.deviceInfo, SerialConfig())
        first.port.isOpen() shouldBe true

        val second = fakeUsbConnection(deviceId = 2)
        repository.connect(second.deviceInfo, SerialConfig())

        first.port.isOpen() shouldBe false
        second.port.isOpen() shouldBe true

        repository.disconnect()
    }
}
