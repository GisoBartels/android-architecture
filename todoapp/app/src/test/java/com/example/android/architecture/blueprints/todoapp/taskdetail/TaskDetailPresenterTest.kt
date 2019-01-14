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
import com.example.android.architecture.blueprints.todoapp.argThat
import com.example.android.architecture.blueprints.todoapp.capture
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.eq
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailMessage.TaskMarkedActive
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailMessage.TaskMarkedCompleted
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailsIntent.*
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import wtf.mvi.intents

/**
 * Unit tests for the implementation of [TaskDetailPresenter]
 */
@RunWith(MockitoJUnitRunner::class)
class TaskDetailPresenterTest {

    private val TITLE_TEST = "title"

    private val DESCRIPTION_TEST = "description"

    private val invalidTaskId = ""

    private val activeTask = Task(TITLE_TEST, DESCRIPTION_TEST)

    private val completedTask = Task(TITLE_TEST, DESCRIPTION_TEST).apply { isCompleted = true }

    @Mock
    private lateinit var tasksRepository: TasksRepository

    @Mock
    private lateinit var navigator: Navigator

    @Mock
    private lateinit var taskDetailView: TaskDetailsView

    /**
     * [ArgumentCaptor] is a powerful Mockito API to capture argument values and use them to
     * perform further actions or assertions on them.
     */
    @Captor
    private lateinit var getTaskCallbackCaptor:
            ArgumentCaptor<TasksDataSource.GetTaskCallback>

    private lateinit var taskDetailPresenter: TaskDetailPresenter

    @Test
    fun getActiveTaskFromRepositoryAndLoadIntoView() {
        // When tasks presenter is asked to open a task
        taskDetailPresenter = TaskDetailPresenter(activeTask.id, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // Then task is loaded from model, callback is captured and progress indicator is shown
        verify(tasksRepository).getTask(eq(activeTask.id), capture(getTaskCallbackCaptor))
        val inOrder = inOrder(taskDetailView)
        inOrder.verify(taskDetailView).render(argThat { showLoadingIndicator == true })

        // When task is finally loaded
        getTaskCallbackCaptor.value.onTaskLoaded(activeTask) // Trigger callback

        // Then progress indicator is hidden and title, description and completion status are shown in UI
        inOrder.verify(taskDetailView).render(argThat {
            showLoadingIndicator == false &&
                    title == TITLE_TEST &&
                    description == DESCRIPTION_TEST &&
                    completionStatus == false
        })
    }

    @Test
    fun getCompletedTaskFromRepositoryAndLoadIntoView() {
        taskDetailPresenter = TaskDetailPresenter(completedTask.id, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // Then task is loaded from model, callback is captured and progress indicator is shown
        verify(tasksRepository).getTask(eq(completedTask.id), capture(getTaskCallbackCaptor))
        val inOrder = inOrder(taskDetailView)
        inOrder.verify(taskDetailView).render(argThat { showLoadingIndicator == true })

        // When task is finally loaded
        getTaskCallbackCaptor.value.onTaskLoaded(completedTask) // Trigger callback

        // Then progress indicator is hidden and title, description and completion status are shown in UI
        inOrder.verify(taskDetailView).render(argThat {
            showLoadingIndicator == false &&
                    title == TITLE_TEST &&
                    description == DESCRIPTION_TEST &&
                    completionStatus == true
        })
    }

    @Test
    fun getUnknownTaskFromRepositoryAndLoadIntoView() {
        // When loading of a task is requested with an invalid task ID.
        taskDetailPresenter = TaskDetailPresenter(invalidTaskId, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        verify(taskDetailView).render(argThat { taskMissing == true })
    }

    @Test
    fun deleteTask() {
        // Given an initialized TaskDetailPresenter with stubbed task
        val task = Task(TITLE_TEST, DESCRIPTION_TEST)
        taskDetailPresenter = TaskDetailPresenter(task.id, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // When the deletion of a task is requested
        taskDetailView.intents.publish(DeleteTask)

        // Then the repository is notified and the detail view is closed
        verify(tasksRepository).deleteTask(task.id)
        verify(navigator).goBack()
    }

    @Test
    fun completeTask() {
        // Given an initialized presenter with an active task
        val task = Task(TITLE_TEST, DESCRIPTION_TEST)
        taskDetailPresenter = TaskDetailPresenter(task.id, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // When a task gets completed
        taskDetailView.intents.publish(CompleteTask)

        // Then a request is sent to the task repository and the UI is updated
        verify(tasksRepository).completeTask(task.id)
        verify(taskDetailView).render(argThat { showMessage == TaskMarkedCompleted })
    }

    @Test
    fun activateTask() {
        // Given an initialized presenter with a completed task
        val task = Task(TITLE_TEST, DESCRIPTION_TEST).apply { isCompleted = true }
        taskDetailPresenter = TaskDetailPresenter(task.id, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // When a task gets activated
        taskDetailView.intents.publish(ActivateTask)

        // Then a request is sent to the task repository and the UI is updated
        verify(tasksRepository).activateTask(task.id)
        verify(taskDetailView).render(argThat { showMessage == TaskMarkedActive })
    }

    @Test
    fun activeTaskIsShownWhenEditing() {
        // Given an initialized presenter with an active task
        taskDetailPresenter = TaskDetailPresenter(activeTask.id, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // When the edit of an activeTask is requested
        taskDetailView.intents.publish(EditTask)

        // Then the app navigates to the edit task screen
        verify(navigator).navToEditTask(activeTask.id)
    }

    @Test
    fun invalidTaskIsNotShownWhenEditing() {
        // Given an initialized presenter with an invalid task
        taskDetailPresenter = TaskDetailPresenter(invalidTaskId, tasksRepository, navigator, Dispatchers.Unconfined)
        taskDetailPresenter.attachView(taskDetailView)

        // When the edit of an invalid task id is requested
        taskDetailView.intents.publish(EditTask)

        // Then the app never navigates to the edit task screen
        verify(navigator, never()).navToEditTask(invalidTaskId)
        // instead, the error is shown.
        verify(taskDetailView).render(argThat { taskMissing == true })
    }

}
