package dev.akexorcist.terminal.usbspp.di

import android.content.Context
import android.hardware.usb.UsbManager
import dev.akexorcist.terminal.usbspp.data.SerialRepository
import dev.akexorcist.terminal.usbspp.data.UsbDataSource
import dev.akexorcist.terminal.usbspp.data.UsbManagerDataSource
import dev.akexorcist.terminal.usbspp.data.UsbSerialRepositoryImpl
import dev.akexorcist.terminal.usbspp.ui.devicelist.DeviceListViewModel
import dev.akexorcist.terminal.usbspp.ui.terminal.TerminalViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single<UsbManager> { get<Context>().getSystemService(Context.USB_SERVICE) as UsbManager }
    singleOf(::UsbManagerDataSource) { bind<UsbDataSource>() }
    singleOf(::UsbSerialRepositoryImpl) { bind<SerialRepository>() }
    viewModelOf(::DeviceListViewModel)
    viewModelOf(::TerminalViewModel)
}
