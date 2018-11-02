package com.example.android.architecture.blueprints.todoapp.addedittask

import kotlinx.serialization.Serializable
import wtf.mvi.MviIntent
import wtf.mvi.MviView

interface AddEditTaskView : MviView<AddEditTaskView.State> {

    val saveTaskIntent: MviIntent<Unit>
    val titleChangedIntent: MviIntent<String>
    val descriptionChangedIntent: MviIntent<String>

    @Serializable
    data class State(
        val showEmptyTaskError: Boolean,
        val title: String,
        val description: String
    ) : MviView.State
}
