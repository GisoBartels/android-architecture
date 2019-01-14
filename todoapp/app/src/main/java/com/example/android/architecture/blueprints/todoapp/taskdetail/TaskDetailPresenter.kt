/*
 * Copyright 2017, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.taskdetail

import com.example.android.architecture.blueprints.todoapp.Navigator
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailMessage.*
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailsIntent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wtf.mvi.MviIntent
import wtf.mvi.subscription.MviBasePresenter
import kotlin.coroutines.CoroutineContext

/**
 * Listens to user actions from the UI ([TaskDetailFragment]), retrieves the data and updates
 * the UI as required.
 */
class TaskDetailPresenter(
    private val taskId: String,
    private val tasksRepository: TasksRepository,
    private val navigator: Navigator,
    override val coroutineContext: CoroutineContext
) : MviBasePresenter<TaskDetailsView, TaskDetailsView.State>(
    TaskDetailsView.State(false, false, "", "", false, NoMessage)
), CoroutineScope {

    private var dismissMessageTimerJob: Job? = null

    override fun onIntent(intent: MviIntent) {
        when (intent) {
            is EditTask -> editTask()
            is DeleteTask -> deleteTask()
            is CompleteTask -> completeTask()
            is ActivateTask -> activateTask()
        }
    }

    override fun onAttachView(view: TaskDetailsView) {
        openTask()
    }

    private fun openTask() {
        if (taskId.isEmpty()) {
            updateViewState(viewState.copy(taskMissing = true))
            return
        }

        updateViewState(viewState.copy(showLoadingIndicator = true))
        tasksRepository.getTask(taskId, object : TasksDataSource.GetTaskCallback {
            override fun onTaskLoaded(task: Task) {
                updateViewState(
                    viewState.copy(
                        showLoadingIndicator = false,
                        taskMissing = false,
                        title = task.title,
                        description = task.description,
                        completionStatus = task.isCompleted
                    )
                )
            }

            override fun onDataNotAvailable() {
                updateViewState(viewState.copy(taskMissing = true, title = "", description = ""))
            }
        })
    }

    private fun editTask() {
        if (taskId.isEmpty()) {
            viewState.copy(taskMissing = true)
        } else {
            navigator.navToEditTask(taskId)
        }
    }

    private fun deleteTask() {
        if (taskId.isEmpty()) return

        tasksRepository.deleteTask(taskId)
        navigator.goBack()
    }

    private fun completeTask() {
        if (taskId.isEmpty()) return

        tasksRepository.completeTask(taskId)
        updateViewState(viewState.copy(completionStatus = true))
        showMessage(TaskMarkedCompleted)
    }

    private fun activateTask() {
        if (taskId.isEmpty()) return

        tasksRepository.activateTask(taskId)
        updateViewState(viewState.copy(completionStatus = false))
        showMessage(TaskMarkedActive)
    }

    private fun showMessage(message: TaskDetailsView.TaskDetailMessage) {
        dismissMessageTimerJob?.cancel()
        updateViewState(viewState.copy(showMessage = message))
        dismissMessageTimerJob = launch {
            delay(2750)
            updateViewState(viewState.copy(showMessage = NoMessage))
            dismissMessageTimerJob = null
        }
    }

}
