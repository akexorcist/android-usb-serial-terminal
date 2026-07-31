package dev.akexorcist.terminal.usbspp.domain

data class UsbDeviceInfo(
    val driverHandle: Any,
    val deviceId: Int,
    val productName: String,
    val manufacturerName: String,
    val vendorId: Int,
    val productId: Int,
    val serialNumber: String?,
    val driverName: String,
    val portCount: Int,
)
