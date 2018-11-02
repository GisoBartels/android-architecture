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

import com.example.android.architecture.blueprints.todoapp.*
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import org.mockito.*
import org.mockito.Mockito.verify
import wtf.mvi.MviIntent
import wtf.mvi.post

/**
 * Unit tests for the implementation of [AddEditTaskPresenter].
 */
class AddEditTaskPresenterTest {

    @Mock
    private lateinit var tasksRepository: TasksRepository

    @Spy
    private lateinit var addEditTaskView: FakeAddEditTaskView

    @Mock
    private lateinit var navigator: Navigator

    /**
     * [ArgumentCaptor] is a powerful Mockito API to capture argument values and use them to
     * perform further actions or assertions on them.
     */
    @Captor
    private lateinit var getTaskCallbackCaptor: ArgumentCaptor<TasksDataSource.GetTaskCallback>

    private lateinit var addEditTaskPresenter: AddEditTaskPresenter

    @Before
    fun setupMocksAndView() {
        // Mockito has a very convenient way to inject mocks by using the @Mock annotation. To
        // inject the mocks in the test the initMocks method needs to be called.
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun saveNewTaskToRepository_showsSuccessMessageUi() {
        // Get a reference to the class under test
        addEditTaskPresenter = AddEditTaskPresenter(null, tasksRepository, navigator, Dispatchers.Unconfined)
        addEditTaskPresenter.attachView(addEditTaskView)

        // When the presenter is asked to save a task
        addEditTaskView.titleChangedIntent.post("New Task Title")
        addEditTaskView.descriptionChangedIntent.post("Some Task Description")
        addEditTaskView.saveTaskIntent.post()

        // Then a task is saved in the repository and the view updated
        verify(tasksRepository).saveTask(any()) // saved to the model
        verify(navigator).returnResultOk() // shown in the UI
    }

    @Test
    fun saveTask_emptyTaskShowsErrorUi() {
        // Get a reference to the class under test
        addEditTaskPresenter = AddEditTaskPresenter(null, tasksRepository, navigator, Dispatchers.Unconfined)
        addEditTaskPresenter.attachView(addEditTaskView)

        // When the presenter is asked to save an empty task
        addEditTaskView.saveTaskIntent.post()

        // Then an empty not error is shown in the UI
        verify(addEditTaskView).render(argThat { showEmptyTaskError })
    }

    @Test
    fun saveExistingTaskToRepository_showsSuccessMessageUi() {
        // Get a reference to the class under test
        addEditTaskPresenter = AddEditTaskPresenter("1", tasksRepository, navigator, Dispatchers.Unconfined)
        addEditTaskPresenter.attachView(addEditTaskView)

        // When the presenter is asked to save an existing task
        addEditTaskView.saveTaskIntent.post()

        // Then a task is saved in the repository and the view updated
        verify(tasksRepository).saveTask(any()) // saved to the model
        verify(navigator).returnResultOk() // shown in the UI
    }

    @Test
    fun attachView_showsCurrentTaskData_whenLoaded() {
        val testTask = Task("TITLE", "DESCRIPTION")
        // Get a reference to the class under test
        addEditTaskPresenter = AddEditTaskPresenter(testTask.id, tasksRepository, navigator, Dispatchers.Unconfined)
        // When the presenter is asked to populate an existing task
        addEditTaskPresenter.attachView(addEditTaskView)

        // Then the task repository is queried and the view updated
        verify(tasksRepository).getTask(eq(testTask.id), capture(getTaskCallbackCaptor))

        // Simulate callback
        getTaskCallbackCaptor.value.onTaskLoaded(testTask)

        verify(addEditTaskView).render(argThat {
            !showEmptyTaskError &&
                    title == testTask.title &&
                    description == testTask.description
        })
    }

    abstract class FakeAddEditTaskView : AddEditTaskView {
        override val saveTaskIntent = MviIntent<Unit>(Dispatchers.Unconfined)
        override val titleChangedIntent = MviIntent<String>(Dispatchers.Unconfined)
        override val descriptionChangedIntent = MviIntent<String>(Dispatchers.Unconfined)
    }
}
