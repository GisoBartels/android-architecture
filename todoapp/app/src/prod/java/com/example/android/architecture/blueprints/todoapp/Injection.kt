/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.architecture.blueprints.todoapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskFragment
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.TasksRepository
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksLocalDataSource
import com.example.android.architecture.blueprints.todoapp.data.source.local.ToDoDatabase
import com.example.android.architecture.blueprints.todoapp.data.source.remote.TasksRemoteDataSource
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailActivity
import com.example.android.architecture.blueprints.todoapp.util.AppExecutors

/**
 * Enables injection of production implementations for
 * [TasksDataSource] at compile time.
 */
object Injection {
    fun provideTasksRepository(context: Context): TasksRepository {
        val database = ToDoDatabase.getInstance(context)
        return TasksRepository.getInstance(
            TasksRemoteDataSource,
            TasksLocalDataSource.getInstance(AppExecutors(), database.taskDao())
        )
    }

    fun provideNavigator(fragment: Fragment) = object : Navigator {
        override fun navToAddTask() {
            fragment.startActivityForResult(
                Intent(fragment.context, AddEditTaskActivity::class.java),
                AddEditTaskActivity.REQUEST_ADD_TASK
            )
        }

        override fun navToEditTask(taskId: String) {
            fragment.startActivityForResult(
                Intent(fragment.context, AddEditTaskActivity::class.java)
                    .putExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID, taskId),
                AddEditTaskActivity.REQUEST_EDIT_TASK
            )
        }

        override fun navToTaskDetails(taskId: String) {
            fragment.startActivity(
                Intent(fragment.context, TaskDetailActivity::class.java)
                    .putExtra(TaskDetailActivity.EXTRA_TASK_ID, taskId)
            )
        }

        override fun goBack() {
            fragment.activity?.finish()
        }

        override fun returnResultOk() {
            fragment.activity?.run {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }
}
