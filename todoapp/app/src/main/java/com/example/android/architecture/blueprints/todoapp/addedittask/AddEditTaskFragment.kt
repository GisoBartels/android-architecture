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
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskView.AddEditTaskIntent.*
import wtf.mvi.android.publishOnTextChange
import wtf.mvi.intents

/**
 * Main UI for the add task screen. Users can enter a task title and description.
 */
class AddEditTaskFragment : Fragment(), AddEditTaskView {

    private lateinit var title: TextView
    private lateinit var description: TextView
    private lateinit var saveButton: FloatingActionButton

    private var snackbar: Snackbar? = null

    override fun render(viewState: AddEditTaskView.State) {
        if (viewState.showEmptyTaskError)
            showEmptyTaskError()
        else
            hideEmptyTaskError()

        title.text = viewState.title
        description.text = viewState.description
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        saveButton = activity!!.findViewById<FloatingActionButton>(R.id.fab_edit_task_done).apply {
            setImageResource(R.drawable.ic_done)
            setOnClickListener { intents.publish(SaveTask) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.addtask_frag, container, false)
        with(root) {
            title = findViewById(R.id.add_task_title)
            description = findViewById(R.id.add_task_description)
        }
        intents.publishOnTextChange(title) { TitleChanged(it) }
        intents.publishOnTextChange(description) { DescriptionChanged(it) }

        setHasOptionsMenu(true)
        return root
    }

    private fun showEmptyTaskError() {
        if (snackbar == null)
            snackbar = Snackbar.make(title, getString(R.string.empty_task_message), Snackbar.LENGTH_INDEFINITE)
                .also { it.show() }
    }

    private fun hideEmptyTaskError() {
        snackbar?.dismiss()
        snackbar = null
    }

    companion object {
        const val ARGUMENT_EDIT_TASK_ID = "EDIT_TASK_ID"

        fun newInstance(taskId: String?) =
            AddEditTaskFragment().apply {
                arguments = Bundle().apply {
                    putString(AddEditTaskFragment.ARGUMENT_EDIT_TASK_ID, taskId)
                }
            }
    }
}
