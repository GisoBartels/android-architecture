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

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.replaceFragmentInActivity
import com.example.android.architecture.blueprints.todoapp.util.setupActionBar
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.JSON

/**
 * Displays an add or edit task screen.
 */
class AddEditTaskActivity : AppCompatActivity() {

    private lateinit var addEditTaskPresenter: AddEditTaskPresenter
    private lateinit var addEditTaskFragment: AddEditTaskFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.addtask_act)
        val taskId = intent.getStringExtra(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID)

        // Set up the toolbar.
        setupActionBar(R.id.toolbar) {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle(if (taskId == null) R.string.add_task else R.string.edit_task)
        }

        addEditTaskFragment = supportFragmentManager.findFragmentById(R.id.contentFrame) as AddEditTaskFragment?
                ?: AddEditTaskFragment.newInstance(taskId).also {
            replaceFragmentInActivity(it, R.id.contentFrame)
        }

        // Create the presenter
        addEditTaskPresenter = AddEditTaskPresenter(
            taskId,
            Injection.provideTasksRepository(applicationContext),
            Injection.provideNavigator(addEditTaskFragment),
            Dispatchers.Main
        )

        savedInstanceState?.let {
            addEditTaskPresenter.updateViewState(
                JSON.parse(AddEditTaskView.State.serializer(), it.getString(SAVED_VIEW_STATE))
            )
        }
    }

    override fun onResume() {
        super.onResume()
        addEditTaskPresenter.attachView(addEditTaskFragment)
    }

    override fun onPause() {
        addEditTaskPresenter.detachView()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save the state so that next time we know if we need to refresh data.
        super.onSaveInstanceState(outState.apply {
            putString(
                SAVED_VIEW_STATE,
                JSON.stringify(AddEditTaskView.State.serializer(), addEditTaskPresenter.viewState)
            )
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val SAVED_VIEW_STATE = "SAVED_VIEW_STATE"
        const val REQUEST_ADD_TASK = 1
        const val REQUEST_EDIT_TASK = 2
    }
}