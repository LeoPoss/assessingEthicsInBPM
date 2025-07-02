package de.ur.apert

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "APERT",
        state = WindowState(size = DpSize(500.dp, 1000.dp)),
    ) {
        App()
    }
}