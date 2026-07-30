package dev.akexorcist.terminal.usbspp.ui.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.akexorcist.terminal.usbspp.domain.ConnectionState
import dev.akexorcist.terminal.usbspp.domain.LineEnding
import dev.akexorcist.terminal.usbspp.domain.SerialIoException
import dev.akexorcist.terminal.usbspp.domain.SerialLine
import dev.akexorcist.terminal.usbspp.domain.SerialRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_HISTORY = 50
private const val MAX_LINES = 2_000

data class TerminalUiState(
  val connectionState: ConnectionState = ConnectionState.Disconnected,
  val lines: List<SerialLine> = emptyList(),
  val hexMode: Boolean = false,
  val lineEnding: LineEnding = LineEnding.LF,
  val sendHistory: List<String> = emptyList(),
)

sealed interface TerminalEvent {
  data class ShowError(val message: String) : TerminalEvent

  data object NavigateBack : TerminalEvent

  data object SendSucceeded : TerminalEvent
}

class TerminalViewModel(private val repository: SerialRepository) : ViewModel() {

  private val lines = MutableStateFlow<List<SerialLine>>(emptyList())
  private val hexMode = MutableStateFlow(false)
  private val lineEnding = MutableStateFlow(LineEnding.LF)
  private val sendHistory = MutableStateFlow<List<String>>(emptyList())

  private val eventsChannel = Channel<TerminalEvent>(Channel.BUFFERED)
  val events: Flow<TerminalEvent> = eventsChannel.receiveAsFlow()

  val uiState: StateFlow<TerminalUiState> =
    combine(repository.connectionState, lines, hexMode, lineEnding, sendHistory) {
        connectionState,
        lines,
        hexMode,
        lineEnding,
        sendHistory ->
        TerminalUiState(
          connectionState = connectionState,
          lines = lines,
          hexMode = hexMode,
          lineEnding = lineEnding,
          sendHistory = sendHistory,
        )
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TerminalUiState())

  init {
    viewModelScope.launch { repository.incomingLines.collect { line -> lines.update { (it + line).takeLast(MAX_LINES) } } }
    viewModelScope.launch {
      repository.errors.collect { message -> eventsChannel.send(TerminalEvent.ShowError(message)) }
    }
  }

  fun sendText(text: String) {
    if (text.isBlank()) return
    viewModelScope.launch {
      try {
        val line = repository.send(text, lineEnding.value)
        lines.update { (it + line).takeLast(MAX_LINES) }
        sendHistory.update { (listOf(text) + it).take(MAX_HISTORY) }
        eventsChannel.send(TerminalEvent.SendSucceeded)
      } catch (e: SerialIoException) {
        eventsChannel.send(TerminalEvent.ShowError(e.message ?: "Send failed"))
      }
    }
  }

  fun toggleHexMode() {
    hexMode.update { !it }
  }

  fun setLineEnding(value: LineEnding) {
    lineEnding.value = value
  }

  fun clearConsole() {
    lines.value = emptyList()
  }

  fun disconnect() {
    repository.disconnect()
    viewModelScope.launch { eventsChannel.send(TerminalEvent.NavigateBack) }
  }
}
