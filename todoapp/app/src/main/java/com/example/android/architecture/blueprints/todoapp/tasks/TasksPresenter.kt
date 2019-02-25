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
package com.example.android.architecture.blueprints.todoapp.tasks

import com.example.android.architecture.blueprints.todoapp.Navigator
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TaskDisplay.*
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TasksIntents.*
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TasksMessage.*
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wtf.mvi.MviIntent
import wtf.mvi.base.MviBasePresenter
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * Listens to user actions from the UI ([TasksFragment]), retrieves the data and updates the
 * UI as required.
 */
class TasksPresenter(
    private val tasksRepository: TasksRepository,
    private val navigator: Navigator,
    override val coroutineContext: CoroutineContext
) : MviBasePresenter<TasksView, TasksView.State>(
    TasksView.State(false, ShowNoTasks, emptyList(), TasksFilterType.ALL_TASKS, NoMessage)
), CoroutineScope {

    private var firstLoad = true

    private var dismissMessageTimerJob: Job? = null

    override fun onIntent(intent: MviIntent) {
        when (intent) {
            is FilterTasks -> filterTasks(intent.tasksFilterType)
            is RefreshTasks -> loadTasks(true)
            is AddNewTask -> navigator.navToAddTask()
            is OpenTaskDetails -> navigator.navToTaskDetails(intent.task.id)
            is CompleteTask -> completeTask(intent.task)
            is ActivateTask -> activateTask(intent.task)
            is ClearCompletedTasks -> clearCompletedTasks()
            is TaskSuccessfullySaved -> showMessage(SuccessfullySaved)
        }
    }

    override fun onAttachView(view: TasksView) {
        loadTasks(false)
    }

    private fun filterTasks(filterType: TasksFilterType) {
        updateViewState(viewState.copy(activeFilter = filterType))
        loadTasks(false)
    }

    private fun loadTasks(forceUpdate: Boolean) {
        // Simplification for sample: a network reload will be forced on first load.
        loadTasks(forceUpdate || firstLoad, true)
        firstLoad = false
    }

    /**
     * @param forceUpdate   Pass in true to refresh the data in the [TasksDataSource]
     * *
     * @param showLoadingUI Pass in true to display a loading icon in the UI
     */
    private fun loadTasks(forceUpdate: Boolean, showLoadingUI: Boolean) {
        if (showLoadingUI) {
            updateViewState(viewState.copy(showLoadingIndicator = true))
        }
        if (forceUpdate) {
            tasksRepository.refreshTasks()
        }

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment() // App is busy until further notice

        tasksRepository.getTasks(object : TasksDataSource.LoadTasksCallback {
            override fun onTasksLoaded(tasks: List<Task>) {
                val tasksToShow = ArrayList<Task>()

                // This callback may be called twice, once for the cache and once for loading
                // the data from the server API, so we check before decrementing, otherwise
                // it throws "Counter has been corrupted!" exception.
                if (!EspressoIdlingResource.countingIdlingResource.isIdleNow) {
                    EspressoIdlingResource.decrement() // Set app as idle.
                }

                // We filter the tasks based on the requestType
                for (task in tasks) {
                    when (viewState.activeFilter) {
                        TasksFilterType.ALL_TASKS -> tasksToShow.add(task)
                        TasksFilterType.ACTIVE_TASKS -> if (task.isActive) {
                            tasksToShow.add(task)
                        }
                        TasksFilterType.COMPLETED_TASKS -> if (task.isCompleted) {
                            tasksToShow.add(task)
                        }
                    }
                }
                if (showLoadingUI) {
                    updateViewState(viewState.copy(showLoadingIndicator = false))
                }

                processTasks(tasksToShow)
            }

            override fun onDataNotAvailable() {
                updateViewState(viewState.copy(showMessage = LoadingTasksError))
            }
        })
    }

    private fun processTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            // Show a message indicating there are no tasks for that filter type.
            processEmptyTasks()
        } else {
            // Show the list of tasks
            updateViewState(viewState.copy(taskDisplay = ShowTasks, taskList = tasks))
        }
    }

    private fun processEmptyTasks() {
        updateViewState(
            viewState.copy(
                taskDisplay = when (viewState.activeFilter) {
                    TasksFilterType.ACTIVE_TASKS -> ShowNoActiveTasks
                    TasksFilterType.COMPLETED_TASKS -> ShowNoCompletedTasks
                    else -> ShowNoTasks
                }
            )
        )
    }

    private fun completeTask(completedTask: Task) {
        tasksRepository.completeTask(completedTask)
        loadTasks(false, false)
        showMessage(TaskMarkedCompleted)
    }

    private fun activateTask(activeTask: Task) {
        tasksRepository.activateTask(activeTask)
        loadTasks(false, false)
        showMessage(TaskMarkedActive)
    }

    private fun clearCompletedTasks() {
        tasksRepository.clearCompletedTasks()
        loadTasks(false, false)
        showMessage(CompletedTasksCleared)
    }

    private fun showMessage(message: TasksView.TasksMessage) {
        dismissMessageTimerJob?.cancel()
        updateViewState(viewState.copy(showMessage = message))
        dismissMessageTimerJob = launch {
            delay(2750)
            updateViewState(viewState.copy(showMessage = NoMessage))
            dismissMessageTimerJob = null
        }
    }

}
