package de.ur.apert

import TaskDetail
import TaskList
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import org.jetbrains.compose.ui.tooling.preview.Preview

sealed class Screen(val route: String) {
    data object TaskList : Screen("task_list")
    data object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }

    data object Info : Screen("info")

    companion object {
        fun fromRoute(route: String?): Screen? {
            return when {
                route?.startsWith(TaskDetail.route.replace("/{taskId}", "/")) == true -> TaskDetail
                route == TaskList.route -> TaskList
                route == Info.route -> Info
                else -> null
            }
        }
    }
}

fun NavController.navigateToTaskDetail(taskId: String) {
    navigate(Screen.TaskDetail.createRoute(taskId)) {
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToInfo() {
    navigate(Screen.Info.route) {
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.navigateToTaskList() {
    navigate(Screen.TaskList.route) {
        popUpTo(Screen.TaskList.route) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

@Preview
@Composable
fun App() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)

    MaterialTheme {
        Scaffold(topBar = {
            CustomTopAppBar(
                navController = navController,
                title = when (currentScreen) {
                    is Screen.TaskDetail -> "Task Detail"
                    is Screen.Info -> "Information"
                    else -> null
                },
                showInfo = currentScreen is Screen.TaskList,
                backIcon = if (currentScreen !is Screen.TaskList) Icons.AutoMirrored.Filled.ArrowBack else null,
                onBackClick = { navController.popBackStack() },
                onInfoClick = { navController.navigateToInfo() })
        }, snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }) { padding ->
            Box(
                modifier = Modifier.padding(padding).widthIn(max = 600.dp),
                contentAlignment = Alignment.Center
            ) {
                NavHost(
                    navController = navController, startDestination = Screen.TaskList.route
                ) {
                    composable(Screen.TaskList.route) {
                        TaskList(
                            snackbarHostState = snackbarHostState,
                            navController = navController,
                            onTaskClick = { task ->
                                navController.navigateToTaskDetail(task.id)
                            })
                    }

                    composable(
                        route = Screen.TaskDetail.route, arguments = listOf(navArgument("taskId") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        val taskId = backStackEntry.savedStateHandle.get<String>("taskId")

                        if (taskId == null) {
                            LaunchedEffect(Unit) {
                                snackbarHostState.showSnackbar(
                                    message = "Error: Task ID not provided",
                                    withDismissAction = true
                                )
                                navController.popBackStack()
                            }
                            return@composable
                        }

                        TaskDetail(
                            snackbarHostState = snackbarHostState,
                            navController = navController,
                            taskId = taskId,
                            onClose = { navController.popBackStack() })
                    }

                    composable(Screen.Info.route) {
                        InfoScreen()
                    }
                }
            }
        }
    }
}