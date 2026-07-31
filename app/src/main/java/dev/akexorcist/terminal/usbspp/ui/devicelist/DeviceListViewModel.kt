package dev.akexorcist.terminal.usbspp.ui.devicelist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.akexorcist.terminal.usbspp.data.SerialConnectionException
import dev.akexorcist.terminal.usbspp.data.SerialRepository
import dev.akexorcist.terminal.usbspp.domain.BaudRate
import dev.akexorcist.terminal.usbspp.domain.DataFraming
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.domain.UsbDeviceInfo
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

data class DeviceListUiState(
  val devices: List<UsbDeviceInfo> = emptyList(),
  val config: SerialConfig = SerialConfig(),
  val connectingDeviceId: Int? = null,
)

sealed interface DeviceListEvent {
  data object NavigateToTerminal : DeviceListEvent

  data class ShowError(val message: String) : DeviceListEvent
}

class DeviceListViewModel(private val repository: SerialRepository) : ViewModel() {

  private val config = MutableStateFlow(SerialConfig())
  private val connectingDeviceId = MutableStateFlow<Int?>(null)

  private val eventsChannel = Channel<DeviceListEvent>(Channel.BUFFERED)
  val events: Flow<DeviceListEvent> = eventsChannel.receiveAsFlow()

  val uiState: StateFlow<DeviceListUiState> =
    combine(repository.observeDevices(), config, connectingDeviceId) { devices, config, connectingDeviceId ->
        DeviceListUiState(devices = devices, config = config, connectingDeviceId = connectingDeviceId)
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeviceListUiState())

  fun updateBaudRate(baudRate: BaudRate) {
    config.update { it.copy(baudRate = baudRate) }
  }

  fun updateFraming(framing: DataFraming) {
    config.update { it.copy(framing = framing) }
  }

  fun connect(device: UsbDeviceInfo) {
    if (connectingDeviceId.value != null) return
    connectingDeviceId.value = device.deviceId
    viewModelScope.launch {
      try {
        repository.connect(device, config.value)
        eventsChannel.send(DeviceListEvent.NavigateToTerminal)
      } catch (e: SerialConnectionException) {
        eventsChannel.send(DeviceListEvent.ShowError(e.message ?: "Connection failed"))
      } finally {
        connectingDeviceId.value = null
      }
    }
  }
}
