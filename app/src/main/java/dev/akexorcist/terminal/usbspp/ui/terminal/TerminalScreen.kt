package dev.akexorcist.terminal.usbspp.ui.terminal

import android.content.ClipData
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.akexorcist.terminal.usbspp.R
import dev.akexorcist.terminal.usbspp.domain.ConnectionState
import dev.akexorcist.terminal.usbspp.domain.LineDirection
import dev.akexorcist.terminal.usbspp.domain.LineEnding
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.domain.SerialLine
import dev.akexorcist.terminal.usbspp.domain.UsbDeviceInfo
import dev.akexorcist.terminal.usbspp.theme.UsbSerialTerminalTheme
import dev.akexorcist.terminal.usbspp.ui.common.SelectionBottomSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import kotlin.time.Instant

@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TerminalEvent.ShowError -> coroutineScope.launch { snackbarHostState.showSnackbar(event.message) }
                TerminalEvent.NavigateBack -> onNavigateBack()
                TerminalEvent.SendSucceeded -> inputText = ""
            }
        }
    }

    TerminalScreenContent(
        uiState = uiState,
        inputText = inputText,
        onInputTextChange = { inputText = it },
        snackbarHostState = snackbarHostState,
        onDisconnect = viewModel::disconnect,
        onClearConsole = viewModel::clearConsole,
        onCopyConsole = {
            val fullText = uiState.lines.joinToString("\n") { "${it.timestampMillis.toTimeLabel()}  ${it.text}" }
            coroutineScope.launch {
                runCatching { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Console output", fullText))) }
            }
        },
        onToggleHexMode = viewModel::toggleHexMode,
        onLineEndingSelected = viewModel::setLineEnding,
        onSend = viewModel::sendText,
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun TerminalScreenContent(
    modifier: Modifier = Modifier,
    uiState: TerminalUiState,
    inputText: String,
    onInputTextChange: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    onDisconnect: () -> Unit,
    onClearConsole: () -> Unit,
    onCopyConsole: () -> Unit,
    onToggleHexMode: () -> Unit,
    onLineEndingSelected: (LineEnding) -> Unit,
    onSend: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val lineCopiedMessage = stringResource(R.string.terminal_line_copied_message)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.connectionState.title(),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDisconnect) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.terminal_action_disconnect),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onClearConsole) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = stringResource(R.string.terminal_action_clear_console),
                        )
                    }
                    IconButton(onClick = onCopyConsole) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.terminal_action_copy_console),
                        )
                    }
                    TerminalOptionsMenu(
                        hexMode = uiState.hexMode,
                        lineEnding = uiState.lineEnding,
                        onToggleHexMode = onToggleHexMode,
                        onLineEndingSelected = onLineEndingSelected,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ConsoleView(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                lines = uiState.lines,
                hexMode = uiState.hexMode,
                onLineCopied = { coroutineScope.launch { snackbarHostState.showSnackbar(lineCopiedMessage) } },
            )
            SendBar(
                text = inputText,
                onTextChange = onInputTextChange,
                sendHistory = uiState.sendHistory,
                enabled = uiState.connectionState is ConnectionState.Connected,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun TerminalOptionsMenu(
    hexMode: Boolean,
    lineEnding: LineEnding,
    onToggleHexMode: () -> Unit,
    onLineEndingSelected: (LineEnding) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var showLineEndingSheet by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.terminal_action_more_options))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            TerminalOptionsMenuContent(
                hexMode = hexMode,
                lineEnding = lineEnding,
                onToggleHexMode = onToggleHexMode,
                onLineEndingClick = {
                    expanded = false
                    showLineEndingSheet = true
                },
            )
        }
    }

    if (showLineEndingSheet) {
        SelectionBottomSheet(
            title = stringResource(R.string.terminal_line_ending_sheet_title),
            options = LineEnding.entries,
            optionLabel = { it.label },
            selected = lineEnding,
            onSelected = onLineEndingSelected,
            onDismissRequest = { showLineEndingSheet = false },
        )
    }
}

@Composable
private fun TerminalOptionsMenuContent(
    hexMode: Boolean,
    lineEnding: LineEnding,
    onToggleHexMode: () -> Unit,
    onLineEndingClick: () -> Unit,
) {
    Column {
        DropdownMenuItem(
            text = { MenuItemLabel(stringResource(R.string.terminal_hex_view_label)) },
            trailingIcon = {
                Switch(
                    checked = hexMode,
                    onCheckedChange = null,
                )
            },
            contentPadding = PaddingValues(16.dp),
            onClick = onToggleHexMode,
        )
        DropdownMenuItem(
            text = { MenuItemLabel(stringResource(R.string.terminal_line_ending_menu_header)) },
            trailingIcon = {
                Text(
                    text = lineEnding.label,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
            contentPadding = PaddingValues(16.dp),
            onClick = onLineEndingClick,
        )
    }
}

private val menuItemLabelSpacing = 32.dp

@Composable
private fun MenuItemLabel(text: String) {
    Row {
        Text(text)
        Spacer(modifier = Modifier.width(menuItemLabelSpacing))
    }
}

@Composable
private fun ConsoleView(
    modifier: Modifier = Modifier,
    lines: List<SerialLine>,
    hexMode: Boolean, onLineCopied: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isAtBottom by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            isScrolledToBottom(lastVisible, info.totalItemsCount)
        }.collect { atBottom -> isAtBottom = atBottom }
    }

    LaunchedEffect(lines.size) {
        if (isAtBottom && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(lines, key = { it.id }) { line ->
                ConsoleLine(line, hexMode, onCopied = onLineCopied)
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0f),
                            MaterialTheme.colorScheme.background,
                        )
                    )
                ),
        )

        if (!isAtBottom && lines.isNotEmpty()) {
            FloatingActionButton(
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 8.dp)
                    .align(Alignment.BottomEnd),
                elevation = FloatingActionButtonDefaults.loweredElevation(),
                onClick = { coroutineScope.launch { listState.animateScrollToItem(lines.lastIndex) } },
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(R.string.terminal_action_jump_to_bottom),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConsoleLine(
    line: SerialLine,
    hexMode: Boolean,
    initiallyExpanded: Boolean = false,
    onCopied: () -> Unit,
) {
    var expanded by remember(line.id) { mutableStateOf(initiallyExpanded) }
    val isSent = line.direction == LineDirection.SENT
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val content = if (hexMode) line.bytes.toHexString() else line.text

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isSent) Arrangement.End
            else Arrangement.Start,
    ) {
        Surface(
            color =
                if (isSent) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
            contentColor =
                if (isSent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            shape =
                if (isSent) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 0.dp)
                else RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 0.dp, bottomEnd = 12.dp),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .combinedClickable(
                    onClickLabel = stringResource(R.string.terminal_line_toggle_timestamp_action),
                    onClick = { expanded = !expanded },
                    onLongClickLabel = stringResource(R.string.terminal_line_copy_action),
                    onLongClick = {
                        coroutineScope.launch {
                            runCatching {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Console line", content)))
                            }.onSuccess { onCopied() }
                        }
                    },
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                )
                if (expanded) {
                    Text(
                        text = line.timestampMillis.toTimeLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SendBar(
    text: String,
    sendHistory: List<String>,
    enabled: Boolean,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
) {
    var historyExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = {
                Text(
                    text = stringResource(R.string.terminal_input_placeholder),
                )
            },
            singleLine = true,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        IconButton(
                            enabled = enabled && sendHistory.isNotEmpty(),
                            onClick = { historyExpanded = true },
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = stringResource(R.string.terminal_action_send_history),
                            )
                        }
                        DropdownMenu(
                            expanded = historyExpanded,
                            onDismissRequest = { historyExpanded = false },
                        ) {
                            sendHistory.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(entry) },
                                    onClick = {
                                        onTextChange(entry)
                                        historyExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    IconButton(
                        enabled = enabled && text.isNotBlank(),
                        onClick = { onSend(text) },
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.terminal_action_send),
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ConnectionState.title(): String =
    when (this) {
        is ConnectionState.Connected -> device.productName
        is ConnectionState.Connecting -> stringResource(R.string.terminal_connecting)
        ConnectionState.Disconnected -> stringResource(R.string.terminal_disconnected)
    }

internal fun isScrolledToBottom(lastVisibleIndex: Int, totalItemsCount: Int): Boolean =
    lastVisibleIndex >= totalItemsCount - 1

private val timeFormatter = LocalDateTime.Format {
    day(padding = Padding.NONE)
    char(' ')
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    yearTwoDigits(baseYear = 2000)
    chars(", ")
    hour()
    char(':')
    minute()
    char(':')
    second()
    char('.')
    secondFraction(3)
}

internal fun Long.toTimeLabel(): String =
    timeFormatter.format(Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()))

internal fun List<Byte>.toHexString(): String = joinToString(" ") { "%02X".format(it) }

private fun previewLine(
    id: Long,
    text: String,
    direction: LineDirection,
): SerialLine =
    SerialLine(
        id = id,
        text = text,
        bytes = text.toByteArray().toList(),
        direction = direction,
        timestampMillis = id * 1_000L,
    )

private val previewDevice =
    UsbDeviceInfo(
        driverHandle = Any(),
        deviceId = 1,
        productName = "Arduino Micro",
        manufacturerName = "Arduino LLC",
        vendorId = 0x2341,
        productId = 0x8037,
        serialNumber = "858323133353519190A1",
        driverName = "CdcAcmSerialDriver",
        portCount = 1,
    )

private val previewLines =
    listOf(
        previewLine(1, "Ready", LineDirection.RECEIVED),
        previewLine(2, "hello world", LineDirection.SENT),
        previewLine(3, "Echo: hello world", LineDirection.RECEIVED),
        previewLine(4, "Heartbeat #1", LineDirection.RECEIVED),
        previewLine(5, "LED_ON", LineDirection.SENT),
        previewLine(6, "LED ON", LineDirection.RECEIVED),
        previewLine(7, "Heartbeat #2", LineDirection.RECEIVED),
    )

private val previewOverflowLines =
    (1..40L).map { id ->
        previewLine(id, "Heartbeat #$id", if (id % 5 == 0L) LineDirection.SENT else LineDirection.RECEIVED)
    }

@Preview(showBackground = true)
@Composable
private fun TerminalScreenConnectedPreview() {
    UsbSerialTerminalTheme {
        TerminalScreenContent(
            uiState = TerminalUiState(connectionState = ConnectionState.Connected(previewDevice, SerialConfig()), lines = previewLines, sendHistory = listOf("LED_ON", "hello world")),
            inputText = "",
            onInputTextChange = {},
            snackbarHostState = remember { SnackbarHostState() },
            onDisconnect = {},
            onClearConsole = {},
            onCopyConsole = {},
            onToggleHexMode = {},
            onLineEndingSelected = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalScreenOverflowPreview() {
    UsbSerialTerminalTheme {
        TerminalScreenContent(
            uiState = TerminalUiState(connectionState = ConnectionState.Connected(previewDevice, SerialConfig()), lines = previewOverflowLines),
            inputText = "",
            onInputTextChange = {},
            snackbarHostState = remember { SnackbarHostState() },
            onDisconnect = {},
            onClearConsole = {},
            onCopyConsole = {},
            onToggleHexMode = {},
            onLineEndingSelected = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalScreenHexModePreview() {
    UsbSerialTerminalTheme {
        TerminalScreenContent(
            uiState =
                TerminalUiState(
                    connectionState = ConnectionState.Connected(previewDevice, SerialConfig()),
                    lines = previewLines,
                    hexMode = true,
                ),
            inputText = "LED_OFF",
            onInputTextChange = {},
            snackbarHostState = remember { SnackbarHostState() },
            onDisconnect = {},
            onClearConsole = {},
            onCopyConsole = {},
            onToggleHexMode = {},
            onLineEndingSelected = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalScreenDisconnectedPreview() {
    UsbSerialTerminalTheme {
        TerminalScreenContent(
            uiState = TerminalUiState(connectionState = ConnectionState.Disconnected),
            inputText = "",
            onInputTextChange = {},
            snackbarHostState = remember { SnackbarHostState() },
            onDisconnect = {},
            onClearConsole = {},
            onCopyConsole = {},
            onToggleHexMode = {},
            onLineEndingSelected = {},
            onSend = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConsoleLineExpandedTimestampPreview() {
    UsbSerialTerminalTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ConsoleLine(previewLine(1, "Echo: hello world", LineDirection.RECEIVED), hexMode = false, onCopied = {}, initiallyExpanded = true)
            ConsoleLine(previewLine(2, "LED_ON", LineDirection.SENT), hexMode = false, onCopied = {}, initiallyExpanded = true)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TerminalOptionsMenuContentPreview() {
    UsbSerialTerminalTheme {
        TerminalOptionsMenuContent(
            hexMode = false,
            lineEnding = LineEnding.LF,
            onToggleHexMode = {},
            onLineEndingClick = {},
        )
    }
}
