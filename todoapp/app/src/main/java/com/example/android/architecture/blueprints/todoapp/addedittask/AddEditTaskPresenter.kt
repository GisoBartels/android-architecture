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

package com.example.android.architecture.blueprints.todoapp.addedittask

import com.example.android.architecture.blueprints.todoapp.Navigator
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wtf.mvi.MviPresenter
import kotlin.coroutines.CoroutineContext

/**
 * Listens to user actions from the UI ([AddEditTaskFragment]), retrieves the data and updates
 * the UI as required.
 */
class AddEditTaskPresenter(
    private val taskId: String?,
    val tasksRepository: TasksDataSource,
    private val navigator: Navigator,
    override val coroutineContext: CoroutineContext
) : MviPresenter<AddEditTaskView>, CoroutineScope, TasksDataSource.GetTaskCallback {

    override val intentActions = intentActions(
        { titleChangedIntent.subscribe { viewState = viewState.copy(title = it) } },
        { descriptionChangedIntent.subscribe { viewState = viewState.copy(description = it) } },
        { saveTaskIntent.subscribe { saveTask(viewState.title, viewState.description) } }
    )

    private var view: AddEditTaskView? = null

    var viewState = AddEditTaskView.State(false, "", "")

    private var dismissMessageTimerJob: Job? = null

    override fun attachView(view: AddEditTaskView) {
        super.attachView(view)
        this.view = view

        if (taskId != null && isDataMissing()) {
            populateTask()
        }
    }

    override fun detachView() {
        this.view = null
        super.detachView()
    }

    private fun isDataMissing() = viewState.title.isEmpty() && viewState.description.isEmpty()

    private fun saveTask(title: String, description: String) {
        if (taskId == null) {
            createTask(title, description)
        } else {
            updateTask(title, description)
        }
    }

    private fun populateTask() {
        if (taskId == null) {
            throw RuntimeException("populateTask() was called but task is new.")
        }
        tasksRepository.getTask(taskId, this)
    }

    override fun onTaskLoaded(task: Task) {
        viewState = viewState.copy(title = task.title, description = task.description)
        view?.render(viewState)
    }

    override fun onDataNotAvailable() {
        showEmptyTaskError()
    }

    private fun createTask(title: String, description: String) {
        val newTask = Task(title, description)
        if (newTask.isEmpty) {
            showEmptyTaskError()
        } else {
            tasksRepository.saveTask(newTask)
            navigator.returnResultOk()
        }
    }

    private fun updateTask(title: String, description: String) {
        if (taskId == null) {
            throw RuntimeException("updateTask() was called but task is new.")
        }
        tasksRepository.saveTask(Task(title, description, taskId))
        navigator.returnResultOk()
    }

    private fun showEmptyTaskError() {
        dismissMessageTimerJob?.cancel()
        viewState = viewState.copy(showEmptyTaskError = true)
        view?.render(viewState)
        dismissMessageTimerJob = launch {
            delay(2750)
            viewState = viewState.copy(showEmptyTaskError = false)
            view?.render(viewState)
            dismissMessageTimerJob = null
        }
    }
}
