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
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TasksMessage.*
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import wtf.mvi.MviPresenter
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates.observable

/**
 * Listens to user actions from the UI ([TasksFragment]), retrieves the data and updates the
 * UI as required.
 */
class TasksPresenter(
    private val tasksRepository: TasksRepository,
    private val navigator: Navigator,
    override val coroutineContext: CoroutineContext
) : MviPresenter<TasksView>, CoroutineScope {

    override val intentActions = intentActions(
        { filterTasksIntent.subscribe { filterTasks(it) } },
        { refreshTasksIntent.subscribe { loadTasks(true) } },
        { addNewTaskIntent.subscribe { navigator.navToAddTask() } },
        { openTaskDetailsIntent.subscribe { navigator.navToTaskDetails(it.id) } },
        { completeTaskIntent.subscribe { completeTask(it) } },
        { activateTaskIntent.subscribe { activateTask(it) } },
        { clearCompletedTasksIntent.subscribe { clearCompletedTasks() } },
        { taskSuccessfullySavedIntent.subscribe { showMessage(SuccessfullySaved) } }
    )

    var viewState by observable(
        TasksView.State(false, ShowNoTasks, emptyList(), TasksFilterType.ALL_TASKS, NoMessage)
    ) { _, _, newValue ->
        launch { view?.render(newValue) }
    }

    private var firstLoad = true

    private var view: TasksView? = null

    private var dismissMessageTimerJob: Job? = null

    override fun attachView(view: TasksView) {
        super.attachView(view)
        this.view = view
        launch { view.render(viewState) }

        loadTasks(false)
    }

    override fun detachView() {
        super.detachView()
        this.view = null
    }

    private fun filterTasks(filterType: TasksFilterType) {
        viewState = viewState.copy(activeFilter = filterType)
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
            viewState = viewState.copy(showLoadingIndicator = true)
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
                    viewState = viewState.copy(showLoadingIndicator = false)
                }

                processTasks(tasksToShow)
            }

            override fun onDataNotAvailable() {
                viewState = viewState.copy(showMessage = LoadingTasksError)
            }
        })
    }

    private fun processTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            // Show a message indicating there are no tasks for that filter type.
            processEmptyTasks()
        } else {
            // Show the list of tasks
            viewState = viewState.copy(taskDisplay = ShowTasks, taskList = tasks)
        }
    }

    private fun processEmptyTasks() {
        viewState = viewState.copy(
            taskDisplay = when (viewState.activeFilter) {
                TasksFilterType.ACTIVE_TASKS -> ShowNoActiveTasks
                TasksFilterType.COMPLETED_TASKS -> ShowNoCompletedTasks
                else -> ShowNoTasks
            }
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
        viewState = viewState.copy(showMessage = message)
        dismissMessageTimerJob = launch {
            delay(2750)
            viewState = viewState.copy(showMessage = NoMessage)
            dismissMessageTimerJob = null
        }
    }

}
