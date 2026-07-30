package dev.akexorcist.terminal.usbspp.ui.devicelist

import dev.akexorcist.terminal.usbspp.MainDispatcherRule
import dev.akexorcist.terminal.usbspp.domain.BaudRate
import dev.akexorcist.terminal.usbspp.domain.DataFraming
import dev.akexorcist.terminal.usbspp.domain.SerialConnectionException
import dev.akexorcist.terminal.usbspp.fake.FakeSerialRepository
import dev.akexorcist.terminal.usbspp.fake.fakeDevice
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceListViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var repository: FakeSerialRepository
  private lateinit var viewModel: DeviceListViewModel

  @Before
  fun setup() {
    repository = FakeSerialRepository()
    viewModel = DeviceListViewModel(repository)
  }

  @Test
  fun `uiState reflects devices published by the repository`() = runTest {
    val collected = mutableListOf<DeviceListUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    val device = fakeDevice(deviceId = 1)
    repository.setDevices(listOf(device))

    assertEquals(listOf(device), viewModel.uiState.value.devices)
  }

  @Test
  fun `updateBaudRate and updateFraming update the selected config`() = runTest {
    val collected = mutableListOf<DeviceListUiState>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { collected.add(it) } }

    viewModel.updateBaudRate(BaudRate.BAUD_115200)
    viewModel.updateFraming(DataFraming.SEVEN_E_1)

    assertEquals(BaudRate.BAUD_115200, viewModel.uiState.value.config.baudRate)
    assertEquals(DataFraming.SEVEN_E_1, viewModel.uiState.value.config.framing)
  }

  @Test
  fun `connect success emits NavigateToTerminal and clears connecting state`() = runTest {
    val events = mutableListOf<DeviceListEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events.add(it) } }

    val device = fakeDevice(deviceId = 1)
    viewModel.connect(device)

    assertEquals(listOf<DeviceListEvent>(DeviceListEvent.NavigateToTerminal), events)
    assertNull(viewModel.uiState.value.connectingDeviceId)
  }

  @Test
  fun `connect failure emits ShowError and clears connecting state`() = runTest {
    repository.connectException = SerialConnectionException("USB permission denied")
    val events = mutableListOf<DeviceListEvent>()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect { events.add(it) } }

    val device = fakeDevice(deviceId = 1)
    viewModel.connect(device)

    assertEquals(listOf<DeviceListEvent>(DeviceListEvent.ShowError("USB permission denied")), events)
    assertNull(viewModel.uiState.value.connectingDeviceId)
  }
}
