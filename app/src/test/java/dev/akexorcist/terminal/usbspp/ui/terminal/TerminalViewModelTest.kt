package dev.akexorcist.terminal.usbspp.ui.terminal

import app.cash.turbine.test
import dev.akexorcist.terminal.usbspp.MainDispatcherRule
import dev.akexorcist.terminal.usbspp.data.SerialRepository
import dev.akexorcist.terminal.usbspp.data.UsbSerialRepositoryImpl
import dev.akexorcist.terminal.usbspp.domain.ConnectionState
import dev.akexorcist.terminal.usbspp.domain.LineDirection
import dev.akexorcist.terminal.usbspp.domain.LineEnding
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.fake.FakeUsbDataSource
import dev.akexorcist.terminal.usbspp.fake.FakeUsbSerialPort
import dev.akexorcist.terminal.usbspp.fake.fakeUsbConnection
import dev.akexorcist.terminal.usbspp.fake.fakeUsbDeviceConnection
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TerminalViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var dataSource: FakeUsbDataSource
    private lateinit var repository: SerialRepository
    private lateinit var viewModel: TerminalViewModel
    private lateinit var port: FakeUsbSerialPort

    @Before
    fun setup() {
        dataSource = FakeUsbDataSource()
        dataSource.deviceConnection = fakeUsbDeviceConnection()
        repository = UsbSerialRepositoryImpl(dataSource)

        val connection = fakeUsbConnection(deviceId = 1)
        port = connection.port
        runBlocking { repository.connect(connection.deviceInfo, SerialConfig()) }

        viewModel = TerminalViewModel(repository)
    }

    @After
    fun tearDown() {
        repository.disconnect()
    }

    @Test
    fun `incoming lines from the repository are appended to uiState`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            port.emitIncoming("hello\n".toByteArray())

            val state = awaitItem()
            state.lines.size shouldBe 1
            state.lines.first().text shouldBe "hello"
            state.lines.first().direction shouldBe LineDirection.RECEIVED
        }
    }

    @Test
    fun `sendText success appends a sent line with the line ending applied and records history`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.sendText("AT")

            val state = expectMostRecentItem()
            state.lines.size shouldBe 1
            state.lines.first().direction shouldBe LineDirection.SENT
            state.lines.first().text shouldBe "AT"
            state.lines.first().bytes shouldBe "AT\n".toByteArray().toList()
            state.sendHistory shouldBe listOf("AT")
        }
    }

    @Test
    fun `sendText appends the selected line ending to the outgoing payload`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.setLineEnding(LineEnding.CRLF)
            awaitItem().lineEnding shouldBe LineEnding.CRLF

            viewModel.sendText("AT")

            val state = expectMostRecentItem()
            state.lines.first().text shouldBe "AT"
            state.lines.first().bytes shouldBe "AT\r\n".toByteArray().toList()
        }
    }

    @Test
    fun `sendText success with a repeated command moves it to the front of history instead of duplicating it`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.sendText("AT")
            expectMostRecentItem().sendHistory shouldBe listOf("AT")

            viewModel.sendText("LED_ON")
            expectMostRecentItem().sendHistory shouldBe listOf("LED_ON", "AT")

            viewModel.sendText("AT")
            val state = expectMostRecentItem()
            state.lines.map { it.text } shouldBe listOf("AT", "LED_ON", "AT")
            state.sendHistory shouldBe listOf("AT", "LED_ON")
        }
    }

    @Test
    fun `sendText blank input is ignored`() = runTest {
        viewModel.sendText("   ")

        port.writtenPayloads.shouldBeEmpty()
    }

    @Test
    fun `sendText failure emits ShowError and does not append a line`() = runTest {
        repository.disconnect()

        viewModel.uiState.test {
            awaitItem()

            viewModel.events.test {
                viewModel.sendText("AT")

                awaitItem() shouldBe TerminalEvent.ShowError("Not connected")
            }

            expectNoEvents()
        }
    }

    @Test
    fun `a partial line with no trailing newline is flushed as its own line when disconnected`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            port.emitIncoming("partial".toByteArray())
            port.awaitIncomingDrained()
            viewModel.disconnect()

            val state = expectMostRecentItem()
            state.lines.size shouldBe 1
            state.lines.first().text shouldBe "partial"
            state.lines.first().direction shouldBe LineDirection.RECEIVED
        }
    }

    @Test
    fun `mid-session repository errors are surfaced as ShowError events and reset connection state`() = runTest {
        viewModel.events.test {
            port.readException = IOException("Device disconnected")

            awaitItem() shouldBe TerminalEvent.ShowError("Device disconnected")
        }
        repository.connectionState.value shouldBe ConnectionState.Disconnected
    }

    @Test
    fun `toggleHexMode flips hex display state`() = runTest {
        viewModel.uiState.test {
            awaitItem().hexMode shouldBe false

            viewModel.toggleHexMode()

            awaitItem().hexMode shouldBe true
        }
    }

    @Test
    fun `setLineEnding updates the selected line ending`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.setLineEnding(LineEnding.CRLF)

            awaitItem().lineEnding shouldBe LineEnding.CRLF
        }
    }

    @Test
    fun `clearConsole empties the line buffer`() = runTest {
        viewModel.uiState.test {
            awaitItem()

            viewModel.sendText("AT")
            expectMostRecentItem().lines.shouldNotBeEmpty()

            viewModel.clearConsole()

            awaitItem().lines shouldBe emptyList()
        }
    }

    @Test
    fun `disconnect calls repository and emits NavigateBack`() = runTest {
        viewModel.events.test {
            viewModel.disconnect()

            awaitItem() shouldBe TerminalEvent.NavigateBack
        }
        repository.connectionState.value shouldBe ConnectionState.Disconnected
    }

    @Test
    fun `appendCapped keeps every item while under the cap`() {
        listOf(1, 2).appendCapped(3, maxSize = 5) shouldBe listOf(1, 2, 3)
    }

    @Test
    fun `appendCapped evicts the oldest item once the cap is reached`() {
        listOf(1, 2, 3).appendCapped(4, maxSize = 3) shouldBe listOf(2, 3, 4)
    }

    @Test
    fun `prependDistinctCapped puts the new item first while under the cap`() {
        listOf("a", "b").prependDistinctCapped("c", maxSize = 5) shouldBe listOf("c", "a", "b")
    }

    @Test
    fun `prependDistinctCapped evicts the oldest item once the cap is reached`() {
        listOf("a", "b", "c").prependDistinctCapped("d", maxSize = 3) shouldBe listOf("d", "a", "b")
    }

    @Test
    fun `prependDistinctCapped moves a repeated item to the front instead of duplicating it`() {
        listOf("a", "b", "c").prependDistinctCapped("b", maxSize = 5) shouldBe listOf("b", "a", "c")
    }
}
