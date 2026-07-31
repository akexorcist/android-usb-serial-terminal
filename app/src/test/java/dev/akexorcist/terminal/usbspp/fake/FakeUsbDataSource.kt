package dev.akexorcist.terminal.usbspp.fake

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialDriver
import dev.akexorcist.terminal.usbspp.data.UsbDataSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

class FakeUsbDataSource : UsbDataSource {

    var drivers: List<UsbSerialDriver> = emptyList()
    var permissionGranted = true
    var deviceConnection: UsbDeviceConnection? = null
    var requestPermissionDelegate: (suspend () -> Boolean)? = null

    private val deviceChangesChannel = Channel<Unit>(Channel.BUFFERED)

    fun emitDeviceChange() {
        deviceChangesChannel.trySend(Unit)
    }

    override fun scanDrivers(): List<UsbSerialDriver> = drivers

    override fun observeDeviceChanges(): Flow<Unit> = deviceChangesChannel.receiveAsFlow()

    override fun hasPermission(device: UsbDevice): Boolean = permissionGranted

    override suspend fun requestPermission(device: UsbDevice): Boolean =
        requestPermissionDelegate?.invoke() ?: permissionGranted

    override fun openDevice(device: UsbDevice): UsbDeviceConnection? = deviceConnection
}
