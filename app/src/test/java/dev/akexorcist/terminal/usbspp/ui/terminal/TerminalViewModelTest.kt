package dev.akexorcist.terminal.usbspp.ui.terminal

import dev.akexorcist.terminal.usbspp.MainDispatcherRule
import dev.akexorcist.terminal.usbspp.domain.LineDirection
import dev.akexorcist.terminal.usbspp.domain.LineEnding
import dev.akexorcist.terminal.usbspp.domain.SerialIoException
import dev.akexorcist.terminal.usbspp.domain.SerialLine
import dev.akexorcist.terminal.usbspp.fake.FakeSerialRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var repository: FakeSerialRepository
  private lateinit var viewModel: TerminalViewModel

  @Before
  fun setup() {
    repository = FakeSerialRepository()
    viewModel = TerminalViewModel(repository)
  }

  @Test
  fun `incoming lines from the repository are appended to uiState`() = runTest {
    val collected = mutableListOf<TerminalUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    val line =
      SerialLine(id = 1L, text = "hello", bytes = "hello".toByteArray().toList(), direction = LineDirection.RECEIVED, timestampMillis = 1L)
    repository.emitIncomingLine(line)

    assertEquals(listOf(line), viewModel.uiState.value.lines)
  }

  @Test
  fun `sendText success appends a sent line and records history`() = runTest {
    val collected = mutableListOf<TerminalUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    viewModel.sendText("AT")

    assertEquals(1, viewModel.uiState.value.lines.size)
    assertEquals(LineDirection.SENT, viewModel.uiState.value.lines.first().direction)
    assertEquals(listOf("AT"), viewModel.uiState.value.sendHistory)
    assertEquals(listOf("AT" to LineEnding.LF), repository.sentPayloads)
  }

  @Test
  fun `sendText blank input is ignored`() = runTest {
    viewModel.sendText("   ")

    assertTrue(viewModel.uiState.value.lines.isEmpty())
    assertTrue(repository.sentPayloads.isEmpty())
  }

  @Test
  fun `sendText failure emits ShowError and does not append a line`() = runTest {
    repository.sendException = SerialIoException("Not connected")
    val events = mutableListOf<TerminalEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events.add(it) } }

    viewModel.sendText("AT")

    assertEquals(listOf<TerminalEvent>(TerminalEvent.ShowError("Not connected")), events)
    assertTrue(viewModel.uiState.value.lines.isEmpty())
  }

  @Test
  fun `mid-session repository errors are surfaced as ShowError events`() = runTest {
    val events = mutableListOf<TerminalEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events.add(it) } }

    repository.emitError("Device disconnected")

    assertEquals(listOf<TerminalEvent>(TerminalEvent.ShowError("Device disconnected")), events)
  }

  @Test
  fun `toggleHexMode flips hex display state`() = runTest {
    val collected = mutableListOf<TerminalUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    assertEquals(false, viewModel.uiState.value.hexMode)
    viewModel.toggleHexMode()
    assertEquals(true, viewModel.uiState.value.hexMode)
  }

  @Test
  fun `setLineEnding updates the selected line ending`() = runTest {
    val collected = mutableListOf<TerminalUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    viewModel.setLineEnding(LineEnding.CRLF)

    assertEquals(LineEnding.CRLF, viewModel.uiState.value.lineEnding)
  }

  @Test
  fun `clearConsole empties the line buffer`() = runTest {
    val collected = mutableListOf<TerminalUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    viewModel.sendText("AT")
    assertTrue(viewModel.uiState.value.lines.isNotEmpty())

    viewModel.clearConsole()

    assertTrue(viewModel.uiState.value.lines.isEmpty())
  }

  @Test
  fun `disconnect calls repository and emits NavigateBack`() = runTest {
    val events = mutableListOf<TerminalEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events.add(it) } }

    viewModel.disconnect()

    assertEquals(1, repository.disconnectCallCount)
    assertEquals(listOf<TerminalEvent>(TerminalEvent.NavigateBack), events)
  }
}
