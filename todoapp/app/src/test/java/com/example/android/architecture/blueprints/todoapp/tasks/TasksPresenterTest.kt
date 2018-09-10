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
import com.example.android.architecture.blueprints.todoapp.any
import com.example.android.architecture.blueprints.todoapp.argThat
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TaskDisplay.ShowTasks
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TasksMessage.*
import com.google.common.collect.Lists
import kotlinx.coroutines.experimental.Unconfined
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.Spy
import wtf.mvi.MviIntent
import wtf.mvi.post

/**
 * Unit tests for the implementation of [TasksPresenter]
 */
class TasksPresenterTest {

    @Mock
    private lateinit var tasksRepository: TasksRepository

    @Spy
    private lateinit var tasksView: FakeTasksView

    @Mock
    private lateinit var navigator: Navigator

    private lateinit var tasksPresenter: TasksPresenter

    private lateinit var tasks: MutableList<Task>

    @Before
    fun setupTasksPresenter() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)

        // Get a reference to the class under test
        tasksPresenter = TasksPresenter(tasksRepository, navigator)

        // We start the tasks to 3, with one active and two completed
        tasks = Lists.newArrayList(Task("Title1", "Description1"),
            Task("Title2", "Description2").apply { isCompleted = true },
            Task("Title3", "Description3").apply { isCompleted = true })
        configureTasksRepository(tasks)
    }

    private fun configureTasksRepository(tasks: List<Task>) {
        `when`(tasksRepository.getTasks(any())).thenAnswer {
            val callback: TasksDataSource.LoadTasksCallback? = it.getArgument(0)
            callback?.onTasksLoaded(tasks)
        }
    }

    private fun configureTasksRepositoryWithUnavailableData() {
        `when`(tasksRepository.getTasks(any())).thenAnswer {
            val callback: TasksDataSource.LoadTasksCallback? = it.getArgument(0)
            callback?.onDataNotAvailable()
        }
    }

    @Test
    fun loadAllTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksPresenter with initialized tasks
        // When loading of Tasks is requested
        tasksPresenter.attachView(tasksView)

        // Then progress indicator is shown
        val inOrder = inOrder(tasksView)
        inOrder.verify(tasksView).render(argThat { showLoadingIndicator })
        // Then progress indicator is hidden and all tasks are shown in UI
        inOrder.verify(tasksView).render(argThat {
            !showLoadingIndicator &&
                    activeFilter == TasksFilterType.ALL_TASKS &&
                    taskDisplay == ShowTasks &&
                    taskList.size == 3
        })
    }

    @Test
    fun loadActiveTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksPresenter with initialized tasks
        tasksPresenter.attachView(tasksView)

        // When loading of Tasks is requested
        tasksView.filterTasksIntent.post(TasksFilterType.ACTIVE_TASKS)

        // Then progress indicator is hidden and active tasks are shown in UI
        verify(tasksView).render(argThat {
            !showLoadingIndicator &&
                    activeFilter == TasksFilterType.ACTIVE_TASKS &&
                    taskDisplay == ShowTasks &&
                    taskList.size == 1
        })
    }

    @Test
    fun loadCompletedTasksFromRepositoryAndLoadIntoView() {
        // Given an initialized TasksPresenter with initialized tasks
        tasksPresenter.attachView(tasksView)

        // When loading of Tasks is requested
        tasksView.filterTasksIntent.post(TasksFilterType.COMPLETED_TASKS)

        // Then progress indicator is hidden and completed tasks are shown in UI
        verify(tasksView).render(argThat {
            !showLoadingIndicator &&
                    activeFilter == TasksFilterType.COMPLETED_TASKS &&
                    taskDisplay == ShowTasks &&
                    taskList.size == 2
        })
    }

    @Test
    fun clickOnFab_ShowsAddTaskUi() {
        // Given an initialized TasksPresenter
        tasksPresenter.attachView(tasksView)

        // When adding a new task
        tasksView.addNewTaskIntent.post()

        // Then add task UI is shown
        verify(navigator).navToAddTask()
    }

    @Test
    fun clickOnTask_ShowsDetailUi() {
        // Given a stubbed active task
        val requestedTask = Task("Details Requested", "For this task")
        configureTasksRepository(listOf(requestedTask))
        // Given an initialized TasksPresenter
        tasksPresenter.attachView(tasksView)


        // When open task details is requested
        tasksView.openTaskDetailsIntent.post(requestedTask)

        // Then task detail UI is shown
        verify(navigator).navToTaskDetails(requestedTask.id)
    }

    @Test
    fun completeTask_ShowsTaskMarkedComplete() {
        // Given a stubbed task
        val task = Task("Details Requested", "For this task")
        // Given an initialized TasksPresenter with initialized tasks
        configureTasksRepository(listOf(task))
        tasksPresenter.attachView(tasksView)

        // When task is marked as complete
        tasksView.completeTaskIntent.post(task)

        // Then repository is called and task marked complete UI is shown
        verify(tasksRepository).completeTask(task)
        verify(tasksView).render(argThat { showMessage == TaskMarkedCompleted })
    }

    @Test
    fun activateTask_ShowsTaskMarkedActive() {
        // Given a stubbed completed task
        val task = Task("Details Requested", "For this task").apply { isCompleted = true }
        // Given an initialized TasksPresenter with initialized tasks
        configureTasksRepository(listOf(task))
        tasksPresenter.attachView(tasksView)

            // When task is marked as activated
            tasksView.activateTaskIntent.post(task)

        // Then repository is called and task marked active UI is shown
        verify(tasksRepository).activateTask(task)
        verify(tasksView).render(argThat { showMessage == TaskMarkedActive })
    }

    @Test
    fun unavailableTasks_ShowsError() {
        // Given a tasks repository with unavailable data
        configureTasksRepositoryWithUnavailableData()

        // When the data is loaded
        tasksPresenter.attachView(tasksView)

        // Then an error message is shown
        verify(tasksView).render(argThat { showMessage == LoadingTasksError })
    }

    abstract class FakeTasksView : TasksView {
        override val filterTasksIntent = MviIntent<TasksFilterType>(Unconfined)
        override val refreshTasksIntent = MviIntent<Unit>(Unconfined)
        override val addNewTaskIntent = MviIntent<Unit>(Unconfined)
        override val openTaskDetailsIntent = MviIntent<Task>(Unconfined)
        override val completeTaskIntent = MviIntent<Task>(Unconfined)
        override val activateTaskIntent = MviIntent<Task>(Unconfined)
        override val clearCompletedTasksIntent = MviIntent<Unit>(Unconfined)
        override val taskSuccessfullySavedIntent = MviIntent<Unit>(Unconfined)
    }

}
