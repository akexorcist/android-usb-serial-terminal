package dev.akexorcist.terminal.usbspp.fake

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.util.EnumSet
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val READ_POLL_TIMEOUT_MILLIS = 50L

class FakeUsbSerialDriver(private val device: UsbDevice, private val port: UsbSerialPort) : UsbSerialDriver {
    override fun getDevice(): UsbDevice = device

    override fun getPorts(): List<UsbSerialPort> = listOf(port)
}

class FakeUsbSerialPort(private val device: UsbDevice, private val readEndpoint: UsbEndpoint) : UsbSerialPort {

    private val incomingData = LinkedBlockingQueue<ByteArray>()
    val writtenPayloads = CopyOnWriteArrayList<ByteArray>()

    var openException: IOException? = null
    var setParametersException: IOException? = null
    var readException: IOException? = null

    @Volatile private var open = false

    fun emitIncoming(data: ByteArray) {
        incomingData.put(data)
    }

    /**
     * Blocks the calling thread until the background read loop has dequeued everything queued
     * via [emitIncoming] so far, for tests that need a deterministic point after which the
     * repository has observed the injected bytes (rather than a fixed sleep).
     */
    fun awaitIncomingDrained(timeoutMillis: Long = 2_000) {
        val deadlineNanos = System.nanoTime() + timeoutMillis * 1_000_000
        while (incomingData.isNotEmpty()) {
            check(System.nanoTime() < deadlineNanos) { "Timed out waiting for queued incoming data to be read" }
            Thread.sleep(5)
        }
    }

    override fun getDriver(): UsbSerialDriver = throw UnsupportedOperationException()

    override fun getDevice(): UsbDevice = device

    override fun getPortNumber(): Int = 0

    override fun getWriteEndpoint(): UsbEndpoint = readEndpoint

    override fun getReadEndpoint(): UsbEndpoint = readEndpoint

    override fun getSerial(): String? = null

    override fun setReadQueue(bufferCount: Int, bufferSize: Int) = Unit

    override fun getReadQueueBufferCount(): Int = 0

    override fun getReadQueueBufferSize(): Int = 0

    override fun open(connection: UsbDeviceConnection) {
        openException?.let { throw it }
        open = true
    }

    override fun close() {
        open = false
    }

    override fun read(dest: ByteArray, timeout: Int): Int = read(dest, dest.size, timeout)

    override fun read(dest: ByteArray, length: Int, timeout: Int): Int {
        readException?.let { throw it }
        val data = incomingData.poll(READ_POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS) ?: return 0
        val copyLength = minOf(data.size, length, dest.size)
        System.arraycopy(data, 0, dest, 0, copyLength)
        return copyLength
    }

    override fun write(src: ByteArray, timeout: Int) = write(src, src.size, timeout)

    override fun write(src: ByteArray, length: Int, timeout: Int) {
        writtenPayloads.add(src.copyOf(length))
    }

    override fun setParameters(baudRate: Int, dataBits: Int, stopBits: Int, parity: Int) {
        setParametersException?.let { throw it }
    }

    override fun getCD(): Boolean = false

    override fun getCTS(): Boolean = false

    override fun getDSR(): Boolean = false

    override fun getDTR(): Boolean = false

    override fun setDTR(value: Boolean) = Unit

    override fun getRI(): Boolean = false

    override fun getRTS(): Boolean = false

    override fun setRTS(value: Boolean) = Unit

    override fun getControlLines(): EnumSet<UsbSerialPort.ControlLine> = EnumSet.noneOf(UsbSerialPort.ControlLine::class.java)

    override fun getSupportedControlLines(): EnumSet<UsbSerialPort.ControlLine> = EnumSet.noneOf(UsbSerialPort.ControlLine::class.java)

    override fun setFlowControl(flowControl: UsbSerialPort.FlowControl) = Unit

    override fun getFlowControl(): UsbSerialPort.FlowControl = UsbSerialPort.FlowControl.NONE

    override fun getSupportedFlowControl(): EnumSet<UsbSerialPort.FlowControl> = EnumSet.of(UsbSerialPort.FlowControl.NONE)

    override fun getXON(): Boolean = false

    override fun purgeHwBuffers(purgeWriteBuffers: Boolean, purgeReadBuffers: Boolean) = Unit

    override fun setBreak(value: Boolean) = Unit

    override fun isOpen(): Boolean = open
}
