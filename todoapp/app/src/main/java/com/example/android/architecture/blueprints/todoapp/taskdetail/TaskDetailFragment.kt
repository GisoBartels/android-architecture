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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.*
import android.widget.CheckBox
import android.widget.TextView
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailMessage.*
import com.example.android.architecture.blueprints.todoapp.taskdetail.TaskDetailsView.TaskDetailsIntent.*
import wtf.mvi.intents

/**
 * Main UI for the task detail screen.
 */
class TaskDetailFragment : Fragment(), TaskDetailsView {

    private lateinit var detailTitle: TextView
    private lateinit var detailDescription: TextView
    private lateinit var detailCompleteStatus: CheckBox

    private var snackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.taskdetail_frag, container, false)
        setHasOptionsMenu(true)
        with(root) {
            detailTitle = findViewById(R.id.task_detail_title)
            detailDescription = findViewById(R.id.task_detail_description)
            detailCompleteStatus = findViewById(R.id.task_detail_complete)
        }

        detailCompleteStatus.setOnClickListener {
            if (detailCompleteStatus.isChecked) {
                intents.publish(CompleteTask)
            } else {
                intents.publish(ActivateTask)
            }
        }

        // Set up floating action button
        activity?.findViewById<FloatingActionButton>(R.id.fab_edit_task)?.setOnClickListener {
            intents.publish(EditTask)
        }

        return root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val deletePressed = item.itemId == R.id.menu_delete
        if (deletePressed)
            intents.publish(DeleteTask)
        return deletePressed
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.taskdetail_fragment_menu, menu)
    }

    override fun render(viewState: TaskDetailsView.State) {
        detailTitle.text = viewState.title
        detailDescription.text = when {
            viewState.showLoadingIndicator -> getString(R.string.loading)
            viewState.taskMissing -> getString(R.string.no_data)
            else -> viewState.description
        }
        detailCompleteStatus.isChecked = viewState.completionStatus

        if (viewState.showMessage == NoMessage)
            snackbar?.dismiss()
        else
            snackbar = viewState.showMessage.show()
    }

    private fun TaskDetailsView.TaskDetailMessage.show() =
        view?.let { view -> Snackbar.make(view, textRes(), Snackbar.LENGTH_INDEFINITE).also { it.show() } }

    @StringRes
    private fun TaskDetailsView.TaskDetailMessage.textRes() = when (this) {
        NoMessage -> 0
        TaskMarkedCompleted -> R.string.task_marked_complete
        TaskMarkedActive -> R.string.task_marked_active
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AddEditTaskActivity.REQUEST_EDIT_TASK) {
            // If the task was edited successfully, go back to the list.
            if (resultCode == Activity.RESULT_OK) {
                activity?.finish()
            }
        }
    }

    companion object {
        private const val ARGUMENT_TASK_ID = "TASK_ID"

        fun newInstance(taskId: String?) =
            TaskDetailFragment().apply {
                arguments = Bundle().apply { putString(ARGUMENT_TASK_ID, taskId) }
            }
    }

}
