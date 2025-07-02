import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import apert.composeapp.generated.resources.Res
import apert.composeapp.generated.resources.done_rafiki
import de.ur.apert.getBaseUrl
import de.ur.apert.getCamundaURL
import de.ur.apert.httpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun TaskList(
    snackbarHostState: SnackbarHostState, navController: NavController, onTaskClick: (Task) -> Unit
) {
    val client = httpClient()
    var allTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var filteredTasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var selectedFilterOption by remember { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    val filterOptions = listOf("All Tasks", "My Tasks")

    var searchQuery by remember { mutableStateOf("") }
    val searchQueryFlow = remember { MutableStateFlow("") }
    var searchBarExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQueryFlow) {
        searchQueryFlow.debounce(300).collect { query ->
            filteredTasks = filterTasks(allTasks, selectedFilterOption, query)
        }
    }

    fun updateSearchQuery(newQuery: String) {
        searchQuery = newQuery
        searchQueryFlow.value = newQuery
    }

    val loadTasks = {
        coroutineScope.launch {
            isRefreshing = true
            try {
                allTasks = client.get("${getCamundaURL()}/engine-rest/task").body()
                filteredTasks = filterTasks(allTasks, selectedFilterOption, searchQuery)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error fetching tasks: ${e.message}")
            } finally {
                isRefreshing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadTasks()
    }

    Column {
        TaskListHeader(onAddClick = {
            coroutineScope.launch {
                try {
                    client.post("${getCamundaURL()}/engine-rest/process-definition/key/Process_0ll61pn/start") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"variables": null}""")
                    }
                    loadTasks()
                    snackbarHostState.showSnackbar("Process instance started successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error starting process: ${e.message}")
                }
            }
        })

        SearchBar(inputField = {
            TextField(value = searchQuery,
                onValueChange = { updateSearchQuery(it) },
                placeholder = { Text("Search...") },
                shape = SearchBarDefaults.inputFieldShape,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search, contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier.clickable {
                                updateSearchQuery("")
                            })
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
            expanded = searchBarExpanded,
            onExpandedChange = { searchBarExpanded = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = SearchBarDefaults.colors(),
            content = {})

        FilterChips(options = filterOptions,
            selectedFilterOption = selectedFilterOption,
            onFilterSelected = { index ->
                selectedFilterOption = index
                filteredTasks = filterTasks(allTasks, selectedFilterOption, searchQuery)
            })

        PullToRefreshBox(
            state = pullToRefreshState,
            isRefreshing = isRefreshing,
            onRefresh = { loadTasks() },
        ) {
            TaskListContent(
                tasks = filteredTasks, isRefreshing = isRefreshing, onTaskClick = onTaskClick
            )
        }
    }
}

fun filterTasks(
    allTasks: List<Task>, filterOption: Int, searchQuery: String = ""
): List<Task> {
    return allTasks.filter { task ->
        val matchesFilter = when (filterOption) {
            0 -> true
            1 -> task.assignee == "admin"
            else -> true
        }

        val matchesSearch = searchQuery.isEmpty() || task.name?.contains(
            searchQuery, ignoreCase = true
        ) == true || task.assignee?.contains(
            searchQuery, ignoreCase = true
        ) == true

        matchesFilter && matchesSearch
    }
}

@Composable
fun FilterChips(
    options: List<String>, selectedFilterOption: Int, onFilterSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, label ->
            FilterChip(selected = index == selectedFilterOption,
                onClick = { onFilterSelected(index) },
                label = { Text(label) },
                shape = FilterChipDefaults.shape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun TaskListHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Current Tasks",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        FilledIconButton(onClick = onAddClick) {
            Icon(
                Icons.Outlined.Add, contentDescription = "Start new process instance"
            )
        }
    }
}

@Composable
fun TaskListContent(
    tasks: List<Task>, isRefreshing: Boolean, onTaskClick: (Task) -> Unit
) {
    if (tasks.isEmpty() && !isRefreshing) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(Res.drawable.done_rafiki),
                null,
                modifier = Modifier.size(250.dp),
                alpha = .5f
            )
            Text(
                "No current tasks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    } else {
        LazyColumn(Modifier.fillMaxSize()) {
            items(tasks) { task ->
                TaskItem(task = task, onClick = { onTaskClick(task) })
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(task.name ?: "Unnamed Task") },
        supportingContent = {
            task.assignee?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null)
                    Text(it)
                }
            }
        },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SearchTextField(
    value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier
) {
    TextField(value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search, contentDescription = "Search"
            )
        },
        singleLine = true,
        modifier = modifier,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Serializable
data class Task(
    val id: String,
    val name: String?,
    val assignee: String?,
    @Serializable(with = CamundaLocalDateTimeSerializer::class) val created: LocalDateTime,
    @Serializable(with = CamundaLocalDateTimeSerializer::class) val due: LocalDateTime? = null,
    @Serializable(with = CamundaLocalDateTimeSerializer::class) val followUp: LocalDateTime? = null,
    @Serializable(with = CamundaLocalDateTimeSerializer::class) val lastUpdated: LocalDateTime? = null,
    val delegationState: String? = null,
    val description: String? = null,
    val executionId: String,
    val owner: String? = null,
    val parentTaskId: String? = null,
    val priority: Int,
    val processDefinitionId: String,
    val processInstanceId: String,
    val taskDefinitionKey: String,
    val caseExecutionId: String? = null,
    val caseInstanceId: String? = null,
    val caseDefinitionId: String? = null,
    val suspended: Boolean,
    val formKey: String? = null,
    val camundaFormRef: String? = null,
    val tenantId: String? = null
)