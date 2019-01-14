package com.example.android.architecture.blueprints.todoapp.addedittask

import kotlinx.serialization.Serializable
import wtf.mvi.MviIntent
import wtf.mvi.MviView

interface AddEditTaskView : MviView<AddEditTaskView.State> {

    sealed class AddEditTaskIntent : MviIntent {
        object SaveTask : AddEditTaskIntent()
        data class TitleChanged(val title: String) : AddEditTaskIntent()
        data class DescriptionChanged(val description: String) : AddEditTaskIntent()
    }

    @Serializable
    data class State(
        val showEmptyTaskError: Boolean,
        val title: String,
        val description: String
    ) : MviView.State
}
