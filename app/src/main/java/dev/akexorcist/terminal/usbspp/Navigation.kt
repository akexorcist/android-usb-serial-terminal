package dev.akexorcist.terminal.usbspp

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dev.akexorcist.terminal.usbspp.data.SerialRepository
import dev.akexorcist.terminal.usbspp.ui.devicelist.DeviceListScreen
import dev.akexorcist.terminal.usbspp.ui.license.LicenseScreen
import dev.akexorcist.terminal.usbspp.ui.terminal.TerminalScreen
import org.koin.compose.koinInject

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(DeviceList)
    val repository = koinInject<SerialRepository>()

    NavDisplay(
        backStack = backStack,
        onBack = {
            // The in-app back arrow on Terminal disconnects before popping (see TerminalViewModel.disconnect()).
            // System back/predictive-back bypasses that screen's own logic entirely, so mirror it here.
            if (backStack.lastOrNull() is Terminal) {
                repository.disconnect()
            }
            backStack.removeLastOrNull()
        },
        entryProvider =
            entryProvider {
                entry<DeviceList> {
                    DeviceListScreen(
                        onNavigateToTerminal = { backStack.add(Terminal) },
                        onNavigateToLicense = { backStack.add(License) },
                    )
                }
                entry<Terminal> { TerminalScreen(onNavigateBack = { backStack.removeLastOrNull() }) }
                entry<License> { LicenseScreen(onNavigateBack = { backStack.removeLastOrNull() }) }
            },
    )
}
