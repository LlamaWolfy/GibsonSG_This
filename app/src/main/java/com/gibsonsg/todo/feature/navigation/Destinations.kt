package com.gibsonsg.todo.feature.navigation

sealed interface Destination {
    val route: String

    data object TaskList : Destination {
        override val route = "task_list"
    }

    data object Kanban : Destination {
        override val route = "kanban"
    }

    data object Calendar : Destination {
        override val route = "calendar"
    }

    data object Search : Destination {
        override val route = "search"
    }

    data object TaskDetail : Destination {
        const val TASK_ID_ARG = "taskId"
        override val route = "task_detail/{$TASK_ID_ARG}"
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
}
