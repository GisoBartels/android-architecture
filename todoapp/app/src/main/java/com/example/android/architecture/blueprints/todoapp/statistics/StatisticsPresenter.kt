/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.android.architecture.blueprints.todoapp.statistics


import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import wtf.mvi.MviPresenter
import kotlin.properties.Delegates.observable

/**
 * Listens to user actions from the UI ([StatisticsFragment]), retrieves the data and updates
 * the UI as required.
 */
class StatisticsPresenter(private val tasksRepository: TasksRepository) : MviPresenter<StatisticsView> {

    private var view: StatisticsView? = null

    private var viewState by observable(
        StatisticsView.State(false, 0, 0, false)
    ) { _, _, newValue ->
        view?.render(newValue)
    }

    override fun attachView(view: StatisticsView) {
        this.view = view
        loadStatistics()
    }

    override fun detachView() {
        this.view = null
    }

    private fun loadStatistics() {
        viewState = viewState.copy(showProgressIndicator = true)

        // The network request might be handled in a different thread so make sure Espresso knows
        // that the app is busy until the response is handled.
        EspressoIdlingResource.increment() // App is busy until further notice

        tasksRepository.getTasks(object : TasksDataSource.LoadTasksCallback {
            override fun onTasksLoaded(tasks: List<Task>) {
                // We calculate number of active and completed tasks
                val completedTasks = tasks.filter { it.isCompleted }.size
                val activeTasks = tasks.size - completedTasks

                // This callback may be called twice, once for the cache and once for loading
                // the data from the server API, so we check before decrementing, otherwise
                // it throws "Counter has been corrupted!" exception.
                if (!EspressoIdlingResource.countingIdlingResource.isIdleNow) {
                    EspressoIdlingResource.decrement() // Set app as idle.
                }
                viewState = viewState.copy(
                    showProgressIndicator = false,
                    numberOfIncompleteTasks = activeTasks,
                    numberOfCompletedTasks = completedTasks
                )
            }

            override fun onDataNotAvailable() {
                viewState = viewState.copy(
                    showProgressIndicator = false,
                    showLoadingStatisticsError = true
                )
            }
        })
    }
}
