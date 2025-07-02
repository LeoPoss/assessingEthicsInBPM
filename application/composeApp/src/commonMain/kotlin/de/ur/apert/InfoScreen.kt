package de.ur.apert

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("APERT is designed to assess ethical perspectives and values throughout the Business Process Management (BPM) lifecycle. It provides a mechanism for evaluating ethical considerations during process modeling, execution, and monitoring phases.", color = MaterialTheme.colorScheme.onSurface)
        Text("2024, the authors", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 16.dp))
    }
}