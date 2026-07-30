package dev.akexorcist.terminal.usbspp.ui.devicelist

import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.akexorcist.terminal.usbspp.R
import dev.akexorcist.terminal.usbspp.domain.BaudRate
import dev.akexorcist.terminal.usbspp.domain.DataFraming
import dev.akexorcist.terminal.usbspp.domain.SerialConfig
import dev.akexorcist.terminal.usbspp.domain.UsbDeviceInfo
import dev.akexorcist.terminal.usbspp.theme.UsbSerialTerminalTheme
import dev.akexorcist.terminal.usbspp.ui.common.SelectionBottomSheet
import dev.akexorcist.terminal.usbspp.ui.common.SelectionBottomSheetContent
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random

@Composable
fun DeviceListScreen(
    onNavigateToTerminal: () -> Unit,
    onNavigateToLicense: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceListViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DeviceListEvent.NavigateToTerminal -> onNavigateToTerminal()
                is DeviceListEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    DeviceListScreenContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBaudRateSelected = viewModel::updateBaudRate,
        onFramingSelected = viewModel::updateFraming,
        onDeviceClick = viewModel::connect,
        onNavigateToLicense = onNavigateToLicense,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceListScreenContent(
    uiState: DeviceListUiState,
    snackbarHostState: SnackbarHostState,
    onBaudRateSelected: (BaudRate) -> Unit,
    onFramingSelected: (DataFraming) -> Unit,
    onDeviceClick: (UsbDeviceInfo) -> Unit,
    onNavigateToLicense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontWeight = FontWeight.Medium,
                    )
                },
                actions = { DeviceListOptionsMenu(onNavigateToLicense = onNavigateToLicense) },
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
                .padding(horizontal = 16.dp)
        ) {
            SerialConfigSelector(
                baudRate = uiState.config.baudRate,
                framing = uiState.config.framing,
                enabled = uiState.connectingDeviceId == null,
                onBaudRateSelected = onBaudRateSelected,
                onFramingSelected = onFramingSelected,
            )

            Text(
                text = stringResource(R.string.device_list_section_header),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(8.dp),
            )

            if (uiState.devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.device_list_empty_state),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(uiState.devices, key = { it.deviceId }) { device ->
                        DeviceListItem(
                            device = device,
                            isConnecting = uiState.connectingDeviceId == device.deviceId,
                            enabled = uiState.connectingDeviceId == null,
                            onClick = { onDeviceClick(device) },
                        )
                    }
                }
            }
        }
    }
}

private const val SOURCE_CODE_URL = "https://github.com/akexorcist/android-usb-serial-terminal"

@Composable
private fun DeviceListOptionsMenu(onNavigateToLicense: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val versionName = rememberAppVersionName()

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.device_list_action_more_options))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DeviceListOptionsMenuContent(
                versionName = versionName,
                onLicenseClick = {
                    expanded = false
                    onNavigateToLicense()
                },
                onSourceCodeClick = {
                    expanded = false
                    uriHandler.openUri(SOURCE_CODE_URL)
                },
            )
        }
    }
}

@Composable
private fun DeviceListOptionsMenuContent(
    versionName: String,
    onLicenseClick: () -> Unit,
    onSourceCodeClick: () -> Unit,
) {
    Column {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.device_list_menu_license)) },
            onClick = onLicenseClick,
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.device_list_menu_source_code)) },
            onClick = onSourceCodeClick,
        )
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).padding(top = 4.dp)) {
            Text(
                text = stringResource(R.string.device_list_menu_app_version),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = versionName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun rememberAppVersionName(): String {
    if (LocalInspectionMode.current) return "1.0.0"
    val context = LocalContext.current
    return remember {
        val packageManager = context.packageManager
        val packageName = context.packageName
        val versionName =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).versionName
            }
        versionName ?: ""
    }
}

@Composable
private fun DeviceListItem(device: UsbDeviceInfo, isConnecting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(),
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = device.productName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = device.manufacturerName,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Column {
                    LabeledValue(
                        label = stringResource(R.string.device_list_item_vendor_label),
                        value = device.vendorId.toHex(),
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    LabeledValue(
                        label = stringResource(R.string.device_list_item_product_label),
                        value = device.productId.toHex(),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row {
        Text(
            text = label,
            modifier = Modifier.width(60.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private enum class ConfigSheet {
    BAUD_RATE,
    FRAMING,
}

@Composable
private fun SerialConfigSelector(
    baudRate: BaudRate,
    framing: DataFraming,
    enabled: Boolean,
    onBaudRateSelected: (BaudRate) -> Unit,
    onFramingSelected: (DataFraming) -> Unit,
) {
    var activeSheet by remember { mutableStateOf<ConfigSheet?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(
            label = { Text(stringResource(R.string.device_list_baud_rate_label, baudRate.label)) },
            enabled = enabled,
            border = AssistChipDefaults.assistChipBorder(enabled = enabled, borderColor = MaterialTheme.colorScheme.primary),
            onClick = { activeSheet = ConfigSheet.BAUD_RATE },
        )
        AssistChip(
            label = { Text(stringResource(R.string.device_list_framing_label, framing.label)) },
            enabled = enabled,
            border = AssistChipDefaults.assistChipBorder(enabled = enabled, borderColor = MaterialTheme.colorScheme.primary),
            onClick = { activeSheet = ConfigSheet.FRAMING },
        )
    }

    when (activeSheet) {
        ConfigSheet.BAUD_RATE ->
            SelectionBottomSheet(
                title = stringResource(R.string.device_list_baud_rate_sheet_title),
                options = BaudRate.entries,
                optionLabel = { it.label },
                selected = baudRate,
                onSelected = onBaudRateSelected,
                onDismissRequest = { activeSheet = null },
            )

        ConfigSheet.FRAMING ->
            SelectionBottomSheet(
                title = stringResource(R.string.device_list_framing_sheet_title),
                options = DataFraming.entries,
                optionLabel = { it.label },
                selected = framing,
                onSelected = onFramingSelected,
                onDismissRequest = { activeSheet = null },
            )

        null -> Unit
    }
}

internal fun Int.toHex(): String = "0x" + this.toString(16).uppercase().padStart(4, '0')

private val previewDevices =
    listOf(
        UsbDeviceInfo(
            driverHandle = Any(),
            deviceId = Random.nextInt(),
            productName = "Arduino Micro",
            manufacturerName = "Arduino LLC",
            vendorId = 0x2341,
            productId = 0x8037,
            serialNumber = "858323133353519190A1",
            driverName = "CdcAcmSerialDriver",
            portCount = 1,
        ),
        UsbDeviceInfo(
            driverHandle = Any(),
            deviceId = Random.nextInt(),
            productName = "USB-SERIAL CH340",
            manufacturerName = "wch.cn",
            vendorId = 0x1A86,
            productId = 0x7523,
            serialNumber = null,
            driverName = "Ch34xSerialDriver",
            portCount = 1,
        ),
    )

@Preview(showBackground = true)
@Composable
private fun DeviceListScreenPreview() {
    UsbSerialTerminalTheme {
        DeviceListScreenContent(
            uiState = DeviceListUiState(
                devices = previewDevices,
                config = SerialConfig(),
                connectingDeviceId = null,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBaudRateSelected = {},
            onFramingSelected = {},
            onDeviceClick = {},
            onNavigateToLicense = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceListScreenConnectingPreview() {
    UsbSerialTerminalTheme {
        DeviceListScreenContent(
            uiState = DeviceListUiState(
                devices = previewDevices,
                config = SerialConfig(),
                connectingDeviceId = previewDevices.first().deviceId,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBaudRateSelected = {},
            onFramingSelected = {},
            onDeviceClick = {},
            onNavigateToLicense = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceListScreenEmptyPreview() {
    UsbSerialTerminalTheme {
        DeviceListScreenContent(
            uiState = DeviceListUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onBaudRateSelected = {},
            onFramingSelected = {},
            onDeviceClick = {},
            onNavigateToLicense = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigSelectionBottomSheetBaudRatePreview() {
    UsbSerialTerminalTheme {
        SelectionBottomSheetContent(
            title = stringResource(R.string.device_list_baud_rate_sheet_title),
            options = BaudRate.entries,
            optionLabel = { it.label },
            selected = BaudRate.BAUD_9600,
            onSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigSelectionBottomSheetFramingPreview() {
    UsbSerialTerminalTheme {
        SelectionBottomSheetContent(
            title = stringResource(R.string.device_list_framing_sheet_title),
            options = DataFraming.entries,
            optionLabel = { it.label },
            selected = DataFraming.EIGHT_N_1,
            onSelected = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DeviceListOptionsMenuContentPreview() {
    UsbSerialTerminalTheme {
        DeviceListOptionsMenuContent(
            versionName = rememberAppVersionName(),
            onLicenseClick = {},
            onSourceCodeClick = {},
        )
    }
}
