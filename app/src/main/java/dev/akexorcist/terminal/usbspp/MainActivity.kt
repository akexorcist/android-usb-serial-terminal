package dev.akexorcist.terminal.usbspp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.core.view.WindowCompat
import dev.akexorcist.terminal.usbspp.theme.UsbSerialTerminalTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      UsbSerialTerminalTheme {
        val colorScheme = MaterialTheme.colorScheme
        SideEffect {
          WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
            colorScheme.primary.luminance() > 0.5f
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() }
      }
    }
  }
}
