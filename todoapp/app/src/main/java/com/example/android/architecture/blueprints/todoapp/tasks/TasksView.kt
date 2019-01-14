package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.data.Task
import kotlinx.serialization.Serializable
import wtf.mvi.MviIntent
import wtf.mvi.MviView

interface TasksView : MviView<TasksView.State> {

    sealed class TasksIntents : MviIntent {
        data class FilterTasks(val tasksFilterType: TasksFilterType) : TasksIntents()
        object RefreshTasks : TasksIntents()
        object AddNewTask : TasksIntents()
        data class OpenTaskDetails(val task: Task) : TasksIntents()
        data class CompleteTask(val task: Task) : TasksIntents()
        data class ActivateTask(val task: Task) : TasksIntents()
        object ClearCompletedTasks : TasksIntents()
        object TaskSuccessfullySaved : TasksIntents()
    }

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
