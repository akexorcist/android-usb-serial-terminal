package dev.akexorcist.terminal.usbspp.fake

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import dev.akexorcist.terminal.usbspp.domain.UsbDeviceInfo
import io.mockk.every
import io.mockk.mockk

fun fakeUsbDevice(
    deviceId: Int = 1,
    productName: String = "Arduino Micro",
    manufacturerName: String = "Arduino LLC",
    vendorId: Int = 0x2341,
    productId: Int = 0x8037,
    serialNumber: String? = "TEST-$deviceId",
): UsbDevice {
    val device = mockk<UsbDevice>()
    every { device.deviceId } returns deviceId
    every { device.productName } returns productName
    every { device.manufacturerName } returns manufacturerName
    every { device.vendorId } returns vendorId
    every { device.productId } returns productId
    every { device.serialNumber } returns serialNumber
    return device
}

fun fakeUsbEndpoint(maxPacketSize: Int = 64): UsbEndpoint {
    val endpoint = mockk<UsbEndpoint>()
    every { endpoint.maxPacketSize } returns maxPacketSize
    return endpoint
}

fun fakeUsbDeviceConnection(): UsbDeviceConnection = mockk(relaxed = true)

data class FakeUsbConnection(
    val deviceInfo: UsbDeviceInfo,
    val driver: FakeUsbSerialDriver,
    val port: FakeUsbSerialPort,
)

fun fakeUsbConnection(
    deviceId: Int = 1,
    productName: String = "Arduino Micro",
    manufacturerName: String = "Arduino LLC",
    vendorId: Int = 0x2341,
    productId: Int = 0x8037,
    serialNumber: String? = "TEST-$deviceId",
): FakeUsbConnection {
    val device = fakeUsbDevice(deviceId, productName, manufacturerName, vendorId, productId, serialNumber)
    val port = FakeUsbSerialPort(device, fakeUsbEndpoint())
    val driver = FakeUsbSerialDriver(device, port)
    val deviceInfo =
        UsbDeviceInfo(
            driverHandle = driver,
            deviceId = deviceId,
            productName = productName,
            manufacturerName = manufacturerName,
            vendorId = vendorId,
            productId = productId,
            serialNumber = serialNumber,
            driverName = "FakeUsbSerialDriver",
            portCount = 1,
        )
    return FakeUsbConnection(deviceInfo, driver, port)
}
