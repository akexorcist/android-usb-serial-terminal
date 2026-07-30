package dev.akexorcist.terminal.usbspp.data

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

private fun String.bytes(): ByteArray = toByteArray(Charsets.UTF_8)

private fun List<Byte>.text(): String = String(toByteArray(), Charsets.UTF_8)

class SerialLineBufferTest {

  @Test
  fun `a single complete line in one chunk is returned whole`() {
    val buffer = SerialLineBuffer()

    val lines = buffer.append("hello\n".bytes())

    assertEquals(1, lines.size)
    assertEquals("hello\n", lines.first().text())
  }

  @Test
  fun `multiple lines in a single chunk are all split out`() {
    val buffer = SerialLineBuffer()

    val lines = buffer.append("one\ntwo\nthree\n".bytes())

    assertEquals(listOf("one\n", "two\n", "three\n"), lines.map { it.text() })
  }

  @Test
  fun `a line split across two chunks is only completed on the second chunk`() {
    val buffer = SerialLineBuffer()

    val firstChunk = buffer.append("hel".bytes())
    assertEquals(emptyList(), firstChunk)

    val secondChunk = buffer.append("lo\n".bytes())
    assertEquals(listOf("hello\n"), secondChunk.map { it.text() })
  }

  @Test
  fun `an unterminated line is not returned until flushed`() {
    val buffer = SerialLineBuffer()

    val lines = buffer.append("partial".bytes())
    assertEquals(emptyList(), lines)

    val flushed = buffer.flush()
    assertEquals("partial", flushed?.text())
  }

  @Test
  fun `flush on an empty buffer returns null`() {
    val buffer = SerialLineBuffer()

    assertNull(buffer.flush())
  }

  @Test
  fun `flush clears the buffer so it does not resurface old bytes`() {
    val buffer = SerialLineBuffer()
    buffer.append("partial".bytes())

    buffer.flush()

    assertNull(buffer.flush())
  }

  @Test
  fun `bytes appended after a flush start a fresh line`() {
    val buffer = SerialLineBuffer()
    buffer.append("first".bytes())
    buffer.flush()

    val lines = buffer.append("second\n".bytes())

    assertEquals(listOf("second\n"), lines.map { it.text() })
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

    assertEquals(threadCount * linesPerThread, collectedLines.size)
    assertNull(buffer.flush())
  }
}
