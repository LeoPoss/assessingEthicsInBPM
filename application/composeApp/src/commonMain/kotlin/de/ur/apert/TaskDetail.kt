import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.ur.apert.MoralitySheet
import de.ur.apert.getBaseUrl
import de.ur.apert.getCamundaURL
import de.ur.apert.httpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.abs

@Composable
fun RelativeDateText(createdDate: LocalDateTime) {
    createdDate.let {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val relativeDate = when (val daysAgo = -today.date.daysUntil(createdDate.date)) {
            0 -> "Today"
            1 -> "Yesterday"
            in 2..7 -> "$daysAgo days ago"
            in 8..30 -> "${daysAgo / 7} weeks ago"
            else -> {
                val formatter = LocalDateTime.Formats.ISO
                createdDate.format(formatter)
            }
        }

        Text(text = relativeDate)
    }
}


@Composable
fun DurationDisplay(durationInMinutes: Int) {
    val hours = durationInMinutes / 60
    val minutes = durationInMinutes % 60

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom
    ) {
        if (hours > 0) {
            Text(
                text = hours.toString(), style = TextStyle(fontSize = 24.sp)
            )
            Text(
                text = if (hours == 1) "hour" else "hours", style = TextStyle(fontSize = 16.sp)
            )
        }

        if (minutes > 0 || hours == 0) {
            Text(
                text = minutes.toString(), style = TextStyle(fontSize = 24.sp)
            )
            Text(
                text = if (minutes == 1) "minute" else "minutes",
                style = TextStyle(fontSize = 16.sp)
            )
        }
    }
}

@Serializable
data class MoralFormInfo(val perspectives: Boolean, val values: List<String>?)

@Composable
@Preview
@OptIn(ExperimentalMaterial3Api::class)
fun TaskDetail(
    snackbarHostState: SnackbarHostState,
    navController: NavController,
    taskId: String,
    onClose: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var task by remember { mutableStateOf<Task?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val client = httpClient()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var binaryResults by remember { mutableStateOf(emptyMap<String, Boolean>()) }
    var valueCards by remember { mutableStateOf(emptyList<String>()) }
    var askPerspectives by remember { mutableStateOf(false) }

    fun getTaskInfoById() {
        coroutineScope.launch {
            isLoading = true
            try {
                val response = client.get("${getCamundaURL()}/engine-rest/task/$taskId")
                task = response.body()
            } catch (e: Exception) {
                println("Error fetching task details: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(taskId) {
        getTaskInfoById()
    }
    LaunchedEffect(task) {
        task?.let {
            val formKey = it.formKey ?: return@let

            try {
                val formInfo = Json.decodeFromString<MoralFormInfo>(formKey)
                valueCards = formInfo.values ?: emptyList()
                askPerspectives = formInfo.perspectives
            } catch (e: SerializationException) {
                println("Serialization error: ${e.message}")
            } catch (e: Exception) {
                println("An unexpected error occurred: ${e.message}")
            }
        }
    }


    Scaffold(bottomBar = {
        BottomAppBar(actions = {
            IconButton(onClick = {
                coroutineScope.launch {
                    try {
                        client.post("${getCamundaURL()}/engine-rest/task/$taskId/assignee") {
                            contentType(ContentType.Application.Json)
                            if (task?.assignee == null) {
                                setBody("""{"userId": "admin"}""")
                            } else {
                                setBody("""{"userId": null}""")
                            }
                        }
                    } catch (e: Exception) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Error starting process: ${e.message}")
                        }
                    } finally {
                        getTaskInfoById()
                        snackbarHostState.showSnackbar(if (task?.assignee == null) "Claimed task" else "Returned task")

                    }
                }
            }) {
                if (task?.assignee != null) {
                    Icon(Icons.Filled.PersonRemove, "Return task")
                } else {
                    Icon(Icons.Filled.PersonAdd, "Claim task")
                }
            }
        }, floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                Icon(Icons.Filled.Check, "Finish Task")
            }
        })
    }, content = { padding ->
        Box(
            modifier = Modifier.padding(padding).widthIn(max = 600.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(8.dp, 0.dp).verticalScroll(rememberScrollState())
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                } else {
                    task?.let {

                        Column {
                            Text(
                                text = it.name ?: "Unnamed",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                RelativeDateText(it.created)
                                Text("•")
                                Text(
                                    it.created.time.format(LocalTime.Format {
                                        hour(); char(':'); minute()
                                    })
                                )
                                Text("•")
                                Text(it.assignee ?: "Unassigned")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Expected Duration",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    DurationDisplay(70)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Priority", style = MaterialTheme.typography.labelLarge
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            it.priority.toString(),
                                            style = TextStyle(fontSize = 24.sp)
                                        )
                                        Text("/")
                                        Text("100", style = TextStyle(fontSize = 24.sp))
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Due", style = MaterialTheme.typography.labelLarge)
                                    Row {
                                        Text(text = it.due?.let { dueDate ->
                                            val daysUntil = Clock.System.now()
                                                .toLocalDateTime(TimeZone.currentSystemDefault()).date.daysUntil(
                                                    dueDate.date
                                                )

                                            when {
                                                daysUntil > 0 -> "in $daysUntil days"
                                                daysUntil < 0 -> "${-daysUntil} days ago"
                                                else -> "Today"
                                            }
                                        } ?: "No due date", style = TextStyle(fontSize = 24.sp))
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(0.dp, 16.dp))


                            Text(
                                text = "Description", style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                it.description ?: "No description",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(16.dp))


//                                Text("Form", style = MaterialTheme.typography.titleMedium)
//
//                                Text(
//                                    Json.decodeFromString<MoralFormInfo>(it.formKey ?: "")
//                                        .toString(),
//                                    style = MaterialTheme.typography.bodyMedium,
//                                    fontFamily = FontFamily.Monospace
//                                )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (valueCards.isNotEmpty()) {
                                Text("Ethics", style = MaterialTheme.typography.titleMedium)
                            }

                            SwipeableCards(valueCards, onAllCardsSwipedWith = { results ->
                                binaryResults = results
                            })


                        }
                    }
                }

                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                        }, sheetState = sheetState
                    ) {
                        MoralitySheet(task!!,
                            binaryResults,
                            askPerspectives,
                            snackbarHostState,
                            sheetState,
                            scope,
                            onTaskCompleted = {
                                scope.launch {
                                    sheetState.hide()
                                }
                                showBottomSheet = false
                                navController.navigate("task_list") {
                                    popUpTo("task_list") {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            })
                    }
                }
            }
        }
    })
}

@Composable
fun SwipeableCards(
    valueCards: List<String>, onAllCardsSwipedWith: (Map<String, Boolean>) -> Unit
) {
    var cardIndex by remember { mutableStateOf(0) }
    val swipeResults = remember { mutableStateListOf<Boolean>() }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (cardIndex >= valueCards.size) {
            LaunchedEffect(Unit) {
                val resultsMap = valueCards.zip(swipeResults).toMap()
                onAllCardsSwipedWith(resultsMap)
            }
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                swipeResults.forEachIndexed { index, result ->
                    Text(
                        "${valueCards[index]}: ${if (result) "yes" else "no"}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        } else {
            (cardIndex until minOf(cardIndex + 3, valueCards.size)).reversed().forEach { index ->
                SwipeableCard(
                    cardText = valueCards[index], onSwiped = { swipedRight ->
                        coroutineScope.launch {
                            swipeResults.add(swipedRight)
                            cardIndex++
                        }
                    }, modifier = Modifier.offset(y = ((index - cardIndex) * 8).dp)
                )
            }

            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    coroutineScope.launch {
                        swipeResults.add(false)
                        cardIndex++
                    }
                }) {
                    Icon(Icons.Filled.ThumbDown, "False")
                }
                Button(onClick = {
                    coroutineScope.launch {
                        swipeResults.add(true)
                        cardIndex++
                    }
                }) {
                    Icon(Icons.Filled.ThumbUp, "True")
                }
            }
        }
    }
}

@Composable
fun SwipeableCard(
    cardText: String, onSwiped: (Boolean) -> Unit, modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX)

    ElevatedCard(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = modifier.fillMaxWidth().height(200.dp).graphicsLayer(
            translationX = animatedOffsetX, rotationZ = animatedOffsetX * 0.1f
        ).pointerInput(Unit) {
            detectDragGestures(onDragEnd = {
                if (abs(offsetX) > size.width / 3) {
                    val swipedRight = offsetX > 0
                    onSwiped(swipedRight)
                    offsetX = 0f
                } else {
                    offsetX = 0f
                }
            }, onDrag = { change, dragAmount ->
                change.consume()
                offsetX += dragAmount.x
            })
        },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            Text(
                cardText,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}