package dev.akexorcist.terminal.usbspp.data

private const val NEWLINE: Byte = '\n'.code.toByte()

class SerialLineBuffer {

    private val lock = Any()
    private val buffer = mutableListOf<Byte>()

    fun append(data: ByteArray): List<List<Byte>> {
        val completedLines = mutableListOf<List<Byte>>()
        synchronized(lock) {
            for (byte in data) {
                buffer.add(byte)
                if (byte == NEWLINE) {
                    completedLines.add(buffer.toList())
                    buffer.clear()
                }
            }
        }
        return completedLines
    }

    fun flush(): List<Byte>? =
        synchronized(lock) {
            if (buffer.isEmpty()) {
                null
            } else {
                buffer.toList().also { buffer.clear() }
            }
        }
}
