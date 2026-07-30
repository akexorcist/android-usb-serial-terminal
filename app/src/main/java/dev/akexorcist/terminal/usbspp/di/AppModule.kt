package dev.akexorcist.terminal.usbspp.di

import dev.akexorcist.terminal.usbspp.data.UsbSerialRepositoryImpl
import dev.akexorcist.terminal.usbspp.domain.SerialRepository
import dev.akexorcist.terminal.usbspp.ui.devicelist.DeviceListViewModel
import dev.akexorcist.terminal.usbspp.ui.terminal.TerminalViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
  singleOf(::UsbSerialRepositoryImpl) { bind<SerialRepository>() }
  viewModelOf(::DeviceListViewModel)
  viewModelOf(::TerminalViewModel)
}
