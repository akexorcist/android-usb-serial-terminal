package dev.akexorcist.terminal.usbspp.data

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

private fun String.bytes(): ByteArray = toByteArray(Charsets.UTF_8)

private fun List<Byte>.text(): String = String(toByteArray(), Charsets.UTF_8)

class SerialLineBufferTest {

    @Test
    fun `a single complete line in one chunk is returned whole`() {
        val buffer = SerialLineBuffer()

        val lines = buffer.append("hello\n".bytes())

        lines.size shouldBe 1
        lines.first().text() shouldBe "hello\n"
    }

    @Test
    fun `multiple lines in a single chunk are all split out`() {
        val buffer = SerialLineBuffer()

        val lines = buffer.append("one\ntwo\nthree\n".bytes())

        lines.map { it.text() } shouldBe listOf("one\n", "two\n", "three\n")
    }

    @Test
    fun `a line split across two chunks is only completed on the second chunk`() {
        val buffer = SerialLineBuffer()

        val firstChunk = buffer.append("hel".bytes())
        firstChunk shouldBe emptyList()

        val secondChunk = buffer.append("lo\n".bytes())
        secondChunk.map { it.text() } shouldBe listOf("hello\n")
    }

    @Test
    fun `an unterminated line is not returned until flushed`() {
        val buffer = SerialLineBuffer()

        val lines = buffer.append("partial".bytes())
        lines shouldBe emptyList()

        val flushed = buffer.flush()
        flushed?.text() shouldBe "partial"
    }

    @Test
    fun `flush on an empty buffer returns null`() {
        val buffer = SerialLineBuffer()

        buffer.flush().shouldBeNull()
    }

    @Test
    fun `flush clears the buffer so it does not resurface old bytes`() {
        val buffer = SerialLineBuffer()
        buffer.append("partial".bytes())

        buffer.flush()

        buffer.flush().shouldBeNull()
    }

    @Test
    fun `bytes appended after a flush start a fresh line`() {
        val buffer = SerialLineBuffer()
        buffer.append("first".bytes())
        buffer.flush()

        val lines = buffer.append("second\n".bytes())

        lines.map { it.text() } shouldBe listOf("second\n")
    }

    @Test
    fun `concurrent appends from multiple threads never lose or duplicate bytes`() {
        val buffer = SerialLineBuffer()
        val threadCount = 8
        val linesPerThread = 200
        val collectedLines = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val threads =
            (0 until threadCount).map { threadIndex ->
                Thread {
                    repeat(linesPerThread) { buffer.append("t$threadIndex\n".bytes()).forEach { collectedLines.add(it.text()) } }
                }
            }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        collectedLines.size shouldBe threadCount * linesPerThread
        buffer.flush().shouldBeNull()
    }
}
