package dev.akexorcist.terminal.usbspp.ui.devicelist

import app.cash.turbine.test
import dev.akexorcist.terminal.usbspp.MainDispatcherRule
import dev.akexorcist.terminal.usbspp.data.SerialRepository
import dev.akexorcist.terminal.usbspp.data.UsbSerialRepositoryImpl
import dev.akexorcist.terminal.usbspp.domain.BaudRate
import dev.akexorcist.terminal.usbspp.domain.DataFraming
import dev.akexorcist.terminal.usbspp.fake.FakeUsbDataSource
import dev.akexorcist.terminal.usbspp.fake.fakeUsbConnection
import dev.akexorcist.terminal.usbspp.fake.fakeUsbDeviceConnection
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DeviceListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: FakeUsbDataSource
    private lateinit var repository: SerialRepository
    private lateinit var viewModel: DeviceListViewModel

    @Before
    fun setup() {
        dataSource = FakeUsbDataSource()
        repository = UsbSerialRepositoryImpl(dataSource)
        viewModel = DeviceListViewModel(repository)
    }

    @After
    fun tearDown() {
        repository.disconnect()
    }

    @Test
    fun `uiState reflects devices published by the repository`() = runTest {
        viewModel.uiState.test {
            awaitItem().devices shouldBe emptyList()

            val connection = fakeUsbConnection(deviceId = 1)
            dataSource.drivers = listOf(connection.driver)
            dataSource.emitDeviceChange()

            awaitItem().devices shouldBe listOf(connection.deviceInfo)
        }
    }

    @Test
    fun `updateBaudRate and updateFraming update the selected config`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.updateBaudRate(BaudRate.BAUD_115200)
            awaitItem().config.baudRate shouldBe BaudRate.BAUD_115200

            viewModel.updateFraming(DataFraming.SEVEN_E_1)
            awaitItem().config.framing shouldBe DataFraming.SEVEN_E_1
        }
    }

    @Test
    fun `connect success emits NavigateToTerminal and clears connecting state`() = runTest {
        val connection = fakeUsbConnection(deviceId = 1)
        dataSource.deviceConnection = fakeUsbDeviceConnection()

        viewModel.uiState.test {
            awaitItem().connectingDeviceId shouldBe null

            viewModel.events.test {
                viewModel.connect(connection.deviceInfo)

                awaitItem() shouldBe DeviceListEvent.NavigateToTerminal
            }

            awaitItem().connectingDeviceId shouldBe 1
            awaitItem().connectingDeviceId shouldBe null
        }
    }

    @Test
    fun `connect failure via denied permission emits ShowError and clears connecting state`() = runTest {
        dataSource.permissionGranted = false
        val connection = fakeUsbConnection(deviceId = 1)

        viewModel.uiState.test {
            awaitItem()

            viewModel.events.test {
                viewModel.connect(connection.deviceInfo)

                awaitItem() shouldBe DeviceListEvent.ShowError("USB permission denied")
            }

            awaitItem().connectingDeviceId shouldBe 1
            awaitItem().connectingDeviceId shouldBe null
        }
    }

    @Test
    fun `connect failure when the device connection cannot be opened emits ShowError and clears connecting state`() = runTest {
        dataSource.deviceConnection = null
        val connection = fakeUsbConnection(deviceId = 1)

        viewModel.uiState.test {
            awaitItem()

            viewModel.events.test {
                viewModel.connect(connection.deviceInfo)

                awaitItem() shouldBe DeviceListEvent.ShowError("Unable to open device connection")
            }

            awaitItem().connectingDeviceId shouldBe 1
            awaitItem().connectingDeviceId shouldBe null
        }
    }

    @Test
    fun `connect failure when the port cannot be opened emits ShowError and clears connecting state`() = runTest {
        dataSource.deviceConnection = fakeUsbDeviceConnection()
        val connection = fakeUsbConnection(deviceId = 1)
        connection.port.openException = IOException("Port busy")

        viewModel.uiState.test {
            awaitItem()

            viewModel.events.test {
                viewModel.connect(connection.deviceInfo)

                awaitItem() shouldBe DeviceListEvent.ShowError("Port busy")
            }

            awaitItem().connectingDeviceId shouldBe 1
            awaitItem().connectingDeviceId shouldBe null
        }
    }

    @Test
    fun `a second connect while one is already in flight is a no-op`() = runTest {
        val inFlight = CompletableDeferred<Boolean>()
        dataSource.permissionGranted = false
        dataSource.requestPermissionDelegate = { inFlight.await() }

        val first = fakeUsbConnection(deviceId = 1)
        val second = fakeUsbConnection(deviceId = 2)

        viewModel.uiState.test {
            awaitItem()

            viewModel.connect(first.deviceInfo)
            awaitItem().connectingDeviceId shouldBe 1

            viewModel.connect(second.deviceInfo)
            viewModel.uiState.value.connectingDeviceId shouldBe 1

            inFlight.complete(false)
            awaitItem().connectingDeviceId shouldBe null
        }
    }
}
