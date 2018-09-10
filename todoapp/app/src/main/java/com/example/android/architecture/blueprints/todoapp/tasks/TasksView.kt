package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import kotlinx.serialization.Serializable
import wtf.mvi.MviIntent
import wtf.mvi.MviView

interface TasksView : MviView<TasksView.State> {

    val filterTasksIntent: MviIntent<TasksFilterType>
    val refreshTasksIntent: MviIntent<Unit>
    val addNewTaskIntent: MviIntent<Unit>
    val openTaskDetailsIntent: MviIntent<Task>
    val completeTaskIntent: MviIntent<Task>
    val activateTaskIntent: MviIntent<Task>
    val clearCompletedTasksIntent: MviIntent<Unit>
    val taskSuccessfullySavedIntent: MviIntent<Unit>

    @Serializable
    data class State(
        val showLoadingIndicator: Boolean,
        val taskDisplay: TaskDisplay,
        val taskList: List<Task>,
        val activeFilter: TasksFilterType,
        val showMessage: TasksMessage
    ) : MviView.State

    enum class TaskDisplay {
        ShowTasks,
        ShowNoActiveTasks,
        ShowNoTasks,
        ShowNoCompletedTasks
    }

    enum class TasksMessage {
        NoMessage,
        TaskMarkedCompleted,
        TaskMarkedActive,
        CompletedTasksCleared,
        SuccessfullySaved,
        LoadingTasksError
    }

}
