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
package com.example.android.architecture.blueprints.todoapp.taskdetail

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.example.android.architecture.blueprints.todoapp.Injection
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.util.replaceFragmentInActivity
import com.example.android.architecture.blueprints.todoapp.util.setupActionBar
import kotlinx.coroutines.Dispatchers

/**
 * Displays task details screen.
 */
class TaskDetailActivity : AppCompatActivity() {

    lateinit var presenter: TaskDetailPresenter
    lateinit var fragment: TaskDetailFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.taskdetail_act)

        // Set up the toolbar.
        setupActionBar(R.id.toolbar) {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        // Get the requested task id
        val taskId = intent.getStringExtra(EXTRA_TASK_ID)

        fragment = supportFragmentManager.findFragmentById(R.id.contentFrame) as TaskDetailFragment? ?:
                TaskDetailFragment.newInstance(taskId).also {
                    replaceFragmentInActivity(it, R.id.contentFrame)
                }

        // Create the presenter
        presenter = TaskDetailPresenter(
            taskId,
            Injection.provideTasksRepository(applicationContext),
            Injection.provideNavigator(fragment),
            Dispatchers.Main
        )
    }

    override fun onResume() {
        super.onResume()
        presenter.attachView(fragment)
    }

    override fun onPause() {
        presenter.detachView()
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_TASK_ID = "TASK_ID"
    }
}
