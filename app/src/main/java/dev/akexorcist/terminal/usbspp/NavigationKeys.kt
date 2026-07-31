package dev.akexorcist.terminal.usbspp

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object DeviceList : NavKey

@Serializable
data object Terminal : NavKey

@Serializable
data object License : NavKey
