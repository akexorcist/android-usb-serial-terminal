package dev.akexorcist.terminal.usbspp.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.akexorcist.terminal.usbspp.theme.UsbSerialTerminalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SelectionBottomSheet(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T,
    onSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        SelectionBottomSheetContent(
            title = title,
            options = options,
            optionLabel = optionLabel,
            selected = selected,
            onSelected = {
                onSelected(it)
                onDismissRequest()
            },
        )
    }
}

@Composable
fun <T> SelectionBottomSheetContent(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    selected: T,
    onSelected: (T) -> Unit,
) {
    Column(
        modifier = Modifier
          .padding(bottom = 16.dp)
          .padding(horizontal = 16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        options.forEach { option ->
            val isSelected = option == selected
            val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { onSelected(option) }
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = optionLabel(option), style = MaterialTheme.typography.bodyLarge, color = contentColor)
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = contentColor)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SelectionBottomSheetContentPreview() {
    UsbSerialTerminalTheme {
        SelectionBottomSheetContent(
            title = "Select an option",
            options = listOf("First", "Second", "Third"),
            optionLabel = { it },
            selected = "Second",
            onSelected = {},
        )
    }
}
