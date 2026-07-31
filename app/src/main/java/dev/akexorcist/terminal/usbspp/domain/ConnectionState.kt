package dev.akexorcist.terminal.usbspp.domain

sealed interface ConnectionState {
    data object Disconnected : ConnectionState

    data class Connecting(val device: UsbDeviceInfo) : ConnectionState

    data class Connected(val device: UsbDeviceInfo, val config: SerialConfig) : ConnectionState
}
