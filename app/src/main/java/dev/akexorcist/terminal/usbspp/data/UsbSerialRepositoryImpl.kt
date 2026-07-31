package dev.akexorcist.terminal.usbspp.data

import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.util.SerialInputOutputManager
import dev.akexorcist.terminal.usbspp.domain.ConnectionState
import dev.akexorcist.terminal.usbspp.domain.LineDirection
import dev.akexorcist.terminal.usbspp.domain.LineEnding
import dev.akexorcist.terminal.usbspp.domain.Parity
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.domain.SerialLine
import dev.akexorcist.terminal.usbspp.domain.UsbDeviceInfo
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

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

class UsbSerialRepositoryImpl(private val dataSource: UsbDataSource) : SerialRepository {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val incomingLinesChannel = Channel<SerialLine>(Channel.BUFFERED)
    override val incomingLines: Flow<SerialLine> = incomingLinesChannel.receiveAsFlow()

    private val errorsChannel = Channel<String>(Channel.BUFFERED)
    override val errors: Flow<String> = errorsChannel.receiveAsFlow()

    @Volatile
    private var currentPort: UsbSerialPort? = null

    @Volatile
    private var currentIoManager: SerialInputOutputManager? = null
    private val lineBuffer = SerialLineBuffer()
    private val lineIdCounter = AtomicLong(0)

    override fun observeDevices(): Flow<List<UsbDeviceInfo>> =
        dataSource.observeDeviceChanges().onStart { emit(Unit) }.map { scanDevices() }

    private fun scanDevices(): List<UsbDeviceInfo> = dataSource.scanDrivers().map { it.toUsbDeviceInfo() }

    private fun UsbSerialDriver.toUsbDeviceInfo(): UsbDeviceInfo {
        val serialNumber = runCatching { device.serialNumber }.getOrNull()
        return UsbDeviceInfo(
            driverHandle = this,
            deviceId = device.deviceId,
            productName = device.productName ?: "Unknown device",
            manufacturerName = device.manufacturerName ?: "Unknown manufacturer",
            vendorId = device.vendorId,
            productId = device.productId,
            serialNumber = serialNumber,
            driverName = this::class.java.simpleName,
            portCount = ports.size,
        )
    }

    override suspend fun connect(device: UsbDeviceInfo, config: SerialConfig) {
        cleanupPort()
        val driver = device.driverHandle as UsbSerialDriver
        _connectionState.update { ConnectionState.Connecting(device) }

        val granted = dataSource.hasPermission(driver.device) || dataSource.requestPermission(driver.device)
        if (!granted) {
            _connectionState.update { ConnectionState.Disconnected }
            throw SerialConnectionException("USB permission denied")
        }

        try {
            withContext(Dispatchers.IO) {
                val connection =
                    dataSource.openDevice(driver.device) ?: throw SerialConnectionException("Unable to open device connection")
                val port = driver.ports.first()
                port.open(connection)
                currentPort = port
                port.setParameters(
                    config.baudRate.value,
                    config.framing.dataBits,
                    config.framing.stopBits.toUsbStopBits(),
                    config.framing.parity.toUsbParity(),
                )
                runCatching { port.dtr = true }
                runCatching { port.rts = true }

                val listener =
                    object : SerialInputOutputManager.Listener {
                        override fun onNewData(data: ByteArray) = handleIncoming(data)

                        override fun onRunError(e: Exception) = handleRunError(e)
                    }
                val ioManager = SerialInputOutputManager(port, listener)
                currentIoManager = ioManager
                ioManager.start()
            }
            _connectionState.update { ConnectionState.Connected(device, config) }
        } catch (e: CancellationException) {
            cleanupPort()
            _connectionState.update { ConnectionState.Disconnected }
            throw e
        } catch (e: Exception) {
            cleanupPort()
            _connectionState.update { ConnectionState.Disconnected }
            throw e as? SerialConnectionException ?: SerialConnectionException(e.message ?: "Unable to open port")
        }
    }

    override fun disconnect() {
        cleanupPort()
        flushLineBuffer()
        _connectionState.update { ConnectionState.Disconnected }
    }

    override suspend fun send(text: String, lineEnding: LineEnding): SerialLine {
        val manager = currentIoManager ?: throw SerialIoException("Not connected")
        val payload = (text + lineEnding.value).toByteArray(Charsets.UTF_8)
        manager.writeAsync(payload)
        return SerialLine(
            id = lineIdCounter.incrementAndGet(),
            text = text,
            bytes = payload.toList(),
            direction = LineDirection.SENT,
            timestampMillis = System.currentTimeMillis(),
        )
    }

    private fun handleIncoming(data: ByteArray) {
        lineBuffer.append(data).forEach { emitLine(it) }
    }

    private fun emitLine(bytes: List<Byte>) {
        val text = String(bytes.toByteArray(), Charsets.UTF_8).trimEnd('\r', '\n')
        incomingLinesChannel.trySend(
            SerialLine(
                id = lineIdCounter.incrementAndGet(),
                text = text,
                bytes = bytes,
                direction = LineDirection.RECEIVED,
                timestampMillis = System.currentTimeMillis(),
            )
        )
    }

    private fun flushLineBuffer() {
        lineBuffer.flush()?.let { emitLine(it) }
    }

    private fun handleRunError(e: Exception) {
        cleanupPort()
        flushLineBuffer()
        _connectionState.update { ConnectionState.Disconnected }
        errorsChannel.trySend(e.message ?: "Connection lost")
    }

    private fun cleanupPort() {
        currentIoManager?.stop()
        currentIoManager = null
        runCatching { currentPort?.close() }
        currentPort = null
    }
}

internal fun Parity.toUsbParity(): Int =
    when (this) {
        Parity.NONE -> UsbSerialPort.PARITY_NONE
        Parity.EVEN -> UsbSerialPort.PARITY_EVEN
        Parity.ODD -> UsbSerialPort.PARITY_ODD
    }

internal fun Int.toUsbStopBits(): Int =
    when (this) {
        2 -> UsbSerialPort.STOPBITS_2
        else -> UsbSerialPort.STOPBITS_1
    }
