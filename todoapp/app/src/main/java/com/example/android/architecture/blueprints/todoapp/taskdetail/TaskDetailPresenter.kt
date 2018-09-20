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
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import wtf.mvi.MviPresenter
import kotlin.properties.Delegates

/**
 * Listens to user actions from the UI ([TaskDetailFragment]), retrieves the data and updates
 * the UI as required.
 */
class TaskDetailPresenter(
    private val taskId: String,
    private val tasksRepository: TasksRepository,
    private val navigator: Navigator
) : MviPresenter<TaskDetailsView> {

    override val intentActions = intentActions(
        { editTaskIntent.subscribe { editTask() } },
        { deleteTaskIntent.subscribe { deleteTask() } },
        { completeTaskIntent.subscribe { completeTask() } },
        { activateTaskIntent.subscribe { activateTask() } }
    )

    private var view: TaskDetailsView? = null

    var viewState by Delegates.observable(TaskDetailsView.State(false, false, "", "", false, NoMessage))
    { _, _, newValue -> view?.render(newValue) }

    private var dismissMessageTimerJob: Job? = null

    override fun attachView(view: TaskDetailsView) {
        super.attachView(view)
        this.view = view
        openTask()
    }

    private fun openTask() {
        if (taskId.isEmpty()) {
            viewState = viewState.copy(taskMissing = true)
            return
        }

        viewState = viewState.copy(showLoadingIndicator = true)
        tasksRepository.getTask(taskId, object : TasksDataSource.GetTaskCallback {
            override fun onTaskLoaded(task: Task) {
                viewState = viewState.copy(
                    showLoadingIndicator = false,
                    taskMissing = false,
                    title = task.title,
                    description = task.description,
                    completionStatus = task.isCompleted
                )
            }

            override fun onDataNotAvailable() {
                viewState = viewState.copy(taskMissing = true, title = "", description = "")
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
        viewState = viewState.copy(completionStatus = true)
        showMessage(TaskMarkedCompleted)
    }

    private fun activateTask() {
        if (taskId.isEmpty()) return

        tasksRepository.activateTask(taskId)
        viewState = viewState.copy(completionStatus = false)
        showMessage(TaskMarkedActive)
    }

    private fun showMessage(message: TaskDetailsView.TaskDetailMessage) {
        dismissMessageTimerJob?.cancel()
        viewState = viewState.copy(showMessage = message)
        dismissMessageTimerJob = launch {
            delay(2750)
            viewState = viewState.copy(showMessage = NoMessage)
            dismissMessageTimerJob = null
        }
    }

}
