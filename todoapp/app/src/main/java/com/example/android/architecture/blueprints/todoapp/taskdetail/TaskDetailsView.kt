package com.example.android.architecture.blueprints.todoapp.taskdetail

import kotlinx.serialization.Serializable
import wtf.mvi.MviIntent
import wtf.mvi.MviView

interface TaskDetailsView : MviView<TaskDetailsView.State> {

    sealed class TaskDetailsIntent : MviIntent {
        object EditTask : TaskDetailsIntent()
        object DeleteTask : TaskDetailsIntent()
        object CompleteTask : TaskDetailsIntent()
        object ActivateTask : TaskDetailsIntent()
    }

    @Serializable
    data class State(
        val showLoadingIndicator: Boolean,
        val taskMissing: Boolean,
        val title: String,
        val description: String,
        val completionStatus: Boolean,
        val showMessage: TaskDetailMessage
    ) : MviView.State

    enum class TaskDetailMessage {
        NoMessage,
        TaskMarkedCompleted,
        TaskMarkedActive,
    }

}
