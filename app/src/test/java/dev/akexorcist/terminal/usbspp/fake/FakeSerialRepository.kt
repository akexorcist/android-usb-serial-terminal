package dev.akexorcist.terminal.usbspp.fake

import dev.akexorcist.terminal.usbspp.domain.ConnectionState
import dev.akexorcist.terminal.usbspp.domain.LineDirection
import dev.akexorcist.terminal.usbspp.domain.LineEnding
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.domain.SerialConnectionException
import dev.akexorcist.terminal.usbspp.domain.SerialIoException
import dev.akexorcist.terminal.usbspp.domain.SerialLine
import dev.akexorcist.terminal.usbspp.domain.SerialRepository
import dev.akexorcist.terminal.usbspp.domain.UsbDeviceInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.atomic.AtomicLong

fun fakeDevice(deviceId: Int, productName: String = "Arduino Micro"): UsbDeviceInfo =
  UsbDeviceInfo(
    driverHandle = Any(),
    deviceId = deviceId,
    productName = productName,
    manufacturerName = "Arduino LLC",
    vendorId = 0x2341,
    productId = 0x8037,
    serialNumber = "TEST-$deviceId",
    driverName = "CdcAcmSerialDriver",
    portCount = 1,
  )

class FakeSerialRepository : SerialRepository {

  private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
  override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

  private val incomingLinesChannel = Channel<SerialLine>(Channel.UNLIMITED)
  override val incomingLines: Flow<SerialLine> = incomingLinesChannel.receiveAsFlow()

  private val errorsChannel = Channel<String>(Channel.UNLIMITED)
  override val errors: Flow<String> = errorsChannel.receiveAsFlow()

  private val devices = MutableStateFlow<List<UsbDeviceInfo>>(emptyList())

  var connectException: SerialConnectionException? = null
  var sendException: SerialIoException? = null
  val sentPayloads = mutableListOf<Pair<String, LineEnding>>()
  var disconnectCallCount = 0
    private set

  private val lineIdCounter = AtomicLong(0)

  fun setDevices(value: List<UsbDeviceInfo>) {
    devices.value = value
  }

  fun emitIncomingLine(line: SerialLine) {
    incomingLinesChannel.trySend(line)
  }

  fun emitError(message: String) {
    errorsChannel.trySend(message)
  }

  override fun observeDevices(): Flow<List<UsbDeviceInfo>> = devices

  override suspend fun connect(device: UsbDeviceInfo, config: SerialConfig) {
    connectException?.let { throw it }
    _connectionState.value = ConnectionState.Connected(device, config)
  }

  override fun disconnect() {
    disconnectCallCount++
    _connectionState.value = ConnectionState.Disconnected
  }

  override suspend fun send(text: String, lineEnding: LineEnding): SerialLine {
    sendException?.let { throw it }
    sentPayloads.add(text to lineEnding)
    return SerialLine(
      id = lineIdCounter.incrementAndGet(),
      text = text,
      bytes = (text + lineEnding.value).toByteArray().toList(),
      direction = LineDirection.SENT,
      timestampMillis = 0L,
    )
  }
}
