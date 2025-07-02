package de.ur.apert

import Task
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.jetbrains.compose.ui.tooling.preview.Preview

data class SliderData(
    val id: String, val leftLabel: String, val rightLabel: String, val category: String
)

@Serializable
data class CombinedDocument(
    val taskId: String,
    val taskName: String,
    val timestamp: Instant,
    val moralityValues: Map<String, Int>,
    val swipeValues: Map<String, Boolean>
)

@Composable
fun SliderWithLabels(
    sliderData: SliderData,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier) {
        Slider(
            value = value, onValueChange = onValueChange, colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
            ), steps = 6, valueRange = 0f..7f
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                sliderData.leftLabel,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(150.dp)
            )
            Text(
                sliderData.rightLabel,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.width(150.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun MoralitySheet(
    task: Task,
    swipeResults: Map<String, Boolean>,
    askPerspectives: Boolean,
    snackbarHostState: SnackbarHostState,
    sheetState: SheetState,
    scope: CoroutineScope,
    onTaskCompleted: () -> Unit
) {
    val client = httpClient()

    val sliders = listOf(
        SliderData("s1", "Unjust", "Just", "Broad-based Moral Equity"),
        SliderData("s2", "Unfair", "Fair", "Broad-based Moral Equity"),
        SliderData("s3", "Not Morally Right", "Morally Right", "Broad-based Moral Equity"),
        SliderData(
            "s4",
            "Not Acceptable to my Family",
            "Acceptable to my Family",
            "Broad-based Moral Equity"
        ),
        SliderData("s5", "Culturally Unacceptable", "Culturally Acceptable", "Relativist View"),
        SliderData(
            "s6", "Traditionally Unacceptable", "Traditionally Acceptable", "Relativist View"
        ),
        SliderData(
            "s7",
            "Violates an Unspoken Promise",
            "Does not Violate an Unspoken Promise",
            "Social Contract View"
        ),
        SliderData(
            "s8",
            "Violates an Unwritten Contract",
            "Does not Violate an Unwritten Contract",
            "Social Contract View"
        )
    )

    var sliderValues by remember { mutableStateOf(sliders.associate { it.id to 0f }) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (askPerspectives) {
                Text("Morality", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))

                sliders.groupBy { it.category }.forEach { (category, categorySliders) ->
                    Text(category, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    categorySliders.forEach { sliderData ->
                        SliderWithLabels(
                            sliderData = sliderData,
                            value = sliderValues[sliderData.id] ?: 0f,
                            onValueChange = {
                                sliderValues =
                                    sliderValues.toMutableMap().apply { this[sliderData.id] = it }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (category != sliders.last().category) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                }
            } else {
                Text("No perspectives needed", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            scope.launch {
                try {
                    val labels = sliders.map { it.rightLabel }
                    val sliderValuesList = sliderValues.values.map { it.toInt() }

                    val res = labels.zip(sliderValuesList).toMap()

                    val combinedDocument = CombinedDocument(
                        taskId = task.taskDefinitionKey,
                        taskName = task.name ?: "null",
                        timestamp = Clock.System.now(),
                        moralityValues = if (askPerspectives) res else emptyMap(),
                        swipeValues = swipeResults
                    )

                    println(combinedDocument)

                    client.post("${getElasticURL()}/ethics/_doc/") {
                        contentType(ContentType.Application.Json)
                        setBody(combinedDocument)
                    }

                    val responseCamunda =
                        client.post("${getCamundaURL()}/engine-rest/task/${task.id}/submit-form") {
                            contentType(ContentType.Application.Json)
                            setBody("""{"variables": null}""")
                        }

                    if (responseCamunda.status.isSuccess()) {
                        onTaskCompleted()
                        snackbarHostState.showSnackbar("Finished task")
                    } else {
                        snackbarHostState.showSnackbar("Failed to submit task")
                    }

                } catch (e: Exception) {
                    println("Exception while submitting values: ${e.message}")
                    snackbarHostState.showSnackbar("An error occurred: ${e.message}")
                }
            }
        }) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Check, null)
                Text("Submit")
            }
        }
    }
}