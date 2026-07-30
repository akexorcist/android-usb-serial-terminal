package dev.akexorcist.terminal.usbspp.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class SerialConnectionException(message: String) : Exception(message)

class SerialIoException(message: String) : Exception(message)

interface SerialRepository {
  val connectionState: StateFlow<ConnectionState>
  val incomingLines: Flow<SerialLine>
  val errors: Flow<String>

  fun observeDevices(): Flow<List<UsbDeviceInfo>>

  suspend fun connect(device: UsbDeviceInfo, config: SerialConfig)

  fun disconnect()

  suspend fun send(text: String, lineEnding: LineEnding): SerialLine
}
