package dev.akexorcist.terminal.usbspp

import android.app.Application
import dev.akexorcist.terminal.usbspp.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class UsbSerialTerminalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@UsbSerialTerminalApp)
            modules(appModule)
        }
    }
}
