package com.example.android.architecture.blueprints.todoapp.taskdetail

import kotlinx.serialization.Serializable
import wtf.mvi.MviIntent
import wtf.mvi.MviView

interface TaskDetailsView : MviView<TaskDetailsView.State> {

    val editTaskIntent: MviIntent<Unit>
    val deleteTaskIntent: MviIntent<Unit>
    val completeTaskIntent: MviIntent<Unit>
    val activateTaskIntent: MviIntent<Unit>

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
