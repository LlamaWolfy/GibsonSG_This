package com.gibsonsg.todo.feature.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gibsonsg.todo.feature.calendar.CalendarRoute
import com.gibsonsg.todo.feature.kanban.KanbanRoute
import com.gibsonsg.todo.feature.search.SearchRoute
import com.gibsonsg.todo.feature.taskdetail.TaskDetailRoute
import com.gibsonsg.todo.feature.tasklist.TaskListRoute

private data class TopLevelTab(
    val destination: Destination,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val topLevelTabs = listOf(
    TopLevelTab(Destination.TaskList, "List", Icons.Default.CheckCircle),
    TopLevelTab(Destination.Kanban, "Board", Icons.Default.ViewKanban),
    TopLevelTab(Destination.Calendar, "Calendar", Icons.Default.CalendarMonth),
    TopLevelTab(Destination.Search, "Search", Icons.Default.Search)
)

@Composable
fun TodoNavHost(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            if (topLevelTabs.any { it.destination.route == currentRoute?.route }) {
                NavigationBar {
                    topLevelTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute?.hierarchy?.any { it.route == tab.destination.route } == true,
                            onClick = {
                                navController.navigate(tab.destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.TaskList.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.TaskList.route) {
                TaskListRoute(
                    onOpenTask = { taskId ->
                        navController.navigate(Destination.TaskDetail.createRoute(taskId))
                    }
                )
            }
            composable(Destination.Kanban.route) {
                KanbanRoute()
            }
            composable(Destination.Calendar.route) {
                CalendarRoute()
            }
            composable(Destination.Search.route) {
                SearchRoute(
                    onOpenTask = { taskId ->
                        navController.navigate(Destination.TaskDetail.createRoute(taskId))
                    }
                )
            }
            composable(Destination.TaskDetail.route) {
                TaskDetailRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
