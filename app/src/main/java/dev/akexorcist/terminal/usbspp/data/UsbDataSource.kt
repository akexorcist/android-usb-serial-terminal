package dev.akexorcist.terminal.usbspp.data

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import com.hoho.android.usbserial.driver.UsbSerialDriver
import kotlinx.coroutines.flow.Flow

interface UsbDataSource {

    fun scanDrivers(): List<UsbSerialDriver>

    fun observeDeviceChanges(): Flow<Unit>

    fun hasPermission(device: UsbDevice): Boolean

    suspend fun requestPermission(device: UsbDevice): Boolean

    fun openDevice(device: UsbDevice): UsbDeviceConnection?
}
