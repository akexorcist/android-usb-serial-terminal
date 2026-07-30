package dev.akexorcist.terminal.usbspp.ui.terminal

import kotlin.test.assertEquals
import org.junit.Test

class TerminalViewModelKtTest {

  @Test
  fun `appendCapped keeps every item while under the cap`() {
    val result = listOf(1, 2).appendCapped(3, maxSize = 5)

    assertEquals(listOf(1, 2, 3), result)
  }

  @Test
  fun `appendCapped evicts the oldest item once the cap is reached`() {
    val result = listOf(1, 2, 3).appendCapped(4, maxSize = 3)

    assertEquals(listOf(2, 3, 4), result)
  }

  @Test
  fun `prependDistinctCapped puts the new item first while under the cap`() {
    val result = listOf("a", "b").prependDistinctCapped("c", maxSize = 5)

    assertEquals(listOf("c", "a", "b"), result)
  }

  @Test
  fun `prependDistinctCapped evicts the oldest item once the cap is reached`() {
    val result = listOf("a", "b", "c").prependDistinctCapped("d", maxSize = 3)

    assertEquals(listOf("d", "a", "b"), result)
  }

  @Test
  fun `prependDistinctCapped moves a repeated item to the front instead of duplicating it`() {
    val result = listOf("a", "b", "c").prependDistinctCapped("b", maxSize = 5)

    assertEquals(listOf("b", "a", "c"), result)
  }
}
