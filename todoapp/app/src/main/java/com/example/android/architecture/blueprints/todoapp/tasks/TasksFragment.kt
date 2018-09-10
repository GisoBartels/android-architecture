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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.annotation.StringRes
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.PopupMenu
import android.view.*
import android.widget.*
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.addedittask.AddEditTaskActivity
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TaskDisplay.*
import com.example.android.architecture.blueprints.todoapp.tasks.TasksView.TasksMessage.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import wtf.mvi.MviIntent
import wtf.mvi.post
import java.util.*

/**
 * Display a grid of [Task]s. User can choose to view all, active or completed tasks.
 */
class TasksFragment : Fragment(), TasksView {

    override val filterTasksIntent = MviIntent<TasksFilterType>()
    override val refreshTasksIntent = MviIntent<Unit>()
    override val addNewTaskIntent = MviIntent<Unit>()
    override val openTaskDetailsIntent = MviIntent<Task>()
    override val completeTaskIntent = MviIntent<Task>()
    override val activateTaskIntent = MviIntent<Task>()
    override val clearCompletedTasksIntent = MviIntent<Unit>()
    override val taskSuccessfullySavedIntent = MviIntent<Unit>()

    private lateinit var noTasksView: View
    private lateinit var noTaskIcon: ImageView
    private lateinit var noTaskMainView: TextView
    private lateinit var noTaskAddView: TextView
    private lateinit var tasksView: LinearLayout
    private lateinit var filteringLabelView: TextView

    private var snackbar: Snackbar? = null

    /**
     * Listener for clicks on tasks in the ListView.
     */
    private var itemListener: TaskItemListener = object : TaskItemListener {
        override fun onTaskClick(clickedTask: Task) {
            openTaskDetailsIntent.post(clickedTask)
        }

        override fun onCompleteTaskClick(completedTask: Task) {
            completeTaskIntent.post(completedTask)
        }

        override fun onActivateTaskClick(activatedTask: Task) {
            activateTaskIntent.post(activatedTask)
        }
    }

    private val listAdapter = TasksAdapter(ArrayList(0), itemListener)

    private var currentMessage = NoMessage

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (AddEditTaskActivity.REQUEST_ADD_TASK == requestCode && Activity.RESULT_OK == resultCode) {
            Handler().post { // post intent, when view is attached
                taskSuccessfullySavedIntent.post()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.tasks_frag, container, false)

        // Set up tasks view
        with(root) {
            val listView = findViewById<ListView>(R.id.tasks_list).apply { adapter = listAdapter }

            // Set up progress indicator
            findViewById<ScrollChildSwipeRefreshLayout>(R.id.refresh_layout).apply {
                setColorSchemeColors(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                    ContextCompat.getColor(requireContext(), R.color.colorAccent),
                    ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                )
                // Set the scrolling view in the custom SwipeRefreshLayout.
                scrollUpChild = listView
                setOnRefreshListener { refreshTasksIntent.post() }
            }

            filteringLabelView = findViewById(R.id.filteringLabel)
            tasksView = findViewById(R.id.tasksLL)

            // Set up  no tasks view
            noTasksView = findViewById(R.id.noTasks)
            noTaskIcon = findViewById(R.id.noTasksIcon)
            noTaskMainView = findViewById(R.id.noTasksMain)
            noTaskAddView = (findViewById<TextView>(R.id.noTasksAdd))
                .apply { setOnClickListener { addNewTaskIntent.post() } }
        }

        // Set up floating action button
        requireActivity().findViewById<FloatingActionButton>(R.id.fab_add_task).apply {
            setImageResource(R.drawable.ic_add)
            setOnClickListener { addNewTaskIntent.post() }
        }
        setHasOptionsMenu(true)

        return root
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> clearCompletedTasksIntent.post()
            R.id.menu_filter -> showFilteringPopUpMenu()
            R.id.menu_refresh -> refreshTasksIntent.post()
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater) {
        inflater.inflate(R.menu.tasks_fragment_menu, menu)
    }

    private fun showFilteringPopUpMenu() {
        val activity = activity ?: return
        val context = context ?: return
        PopupMenu(context, activity.findViewById(R.id.menu_filter)).apply {
            menuInflater.inflate(R.menu.filter_tasks, menu)
            setOnMenuItemClickListener { item ->
                filterTasksIntent.post(
                    when (item.itemId) {
                        R.id.active -> TasksFilterType.ACTIVE_TASKS
                        R.id.completed -> TasksFilterType.COMPLETED_TASKS
                        else -> TasksFilterType.ALL_TASKS
                    }
                )
                true
            }
            show()
        }
    }

    override fun render(viewState: TasksView.State) {
        launch(UI) {
            with(viewState) {
                setLoadingIndicator(showLoadingIndicator)

                when (taskDisplay) {
                    ShowTasks -> showTasks(taskList)
                    ShowNoActiveTasks -> showNoActiveTasks()
                    ShowNoTasks -> showNoTasks()
                    ShowNoCompletedTasks -> showNoCompletedTasks()
                }

                filteringLabelView.text = resources.getString(
                    when (activeFilter) {
                        TasksFilterType.ACTIVE_TASKS -> R.string.label_active
                        TasksFilterType.COMPLETED_TASKS -> R.string.label_completed
                        TasksFilterType.ALL_TASKS -> R.string.label_all
                    }
                )

                if (currentMessage != showMessage) {
                    currentMessage = showMessage
                    if (showMessage == NoMessage)
                        snackbar?.dismiss()
                    else
                        snackbar = showMessage.show()
                }
            }
        }
    }

    private fun setLoadingIndicator(active: Boolean) {
        val root = view ?: return
        with(root.findViewById<SwipeRefreshLayout>(R.id.refresh_layout)) {
            // Make sure setRefreshing() is called after the layout is done with everything else.
            post { isRefreshing = active }
        }
    }

    private fun showTasks(tasks: List<Task>) {
        listAdapter.tasks = tasks
        tasksView.visibility = View.VISIBLE
        noTasksView.visibility = View.GONE
    }

    private fun showNoActiveTasks() {
        showNoTasksViews(resources.getString(R.string.no_tasks_active), R.drawable.ic_check_circle_24dp, false)
    }

    private fun showNoTasks() {
        showNoTasksViews(resources.getString(R.string.no_tasks_all), R.drawable.ic_assignment_turned_in_24dp, false)
    }

    private fun showNoCompletedTasks() {
        showNoTasksViews(resources.getString(R.string.no_tasks_completed), R.drawable.ic_verified_user_24dp, false)
    }

    private fun showNoTasksViews(mainText: String, iconRes: Int, showAddView: Boolean) {
        tasksView.visibility = View.GONE
        noTasksView.visibility = View.VISIBLE

        noTaskMainView.text = mainText
        noTaskIcon.setImageResource(iconRes)
        noTaskAddView.visibility = if (showAddView) View.VISIBLE else View.GONE
    }

    private fun TasksView.TasksMessage.show() =
        view?.let { view -> Snackbar.make(view, textRes(), Snackbar.LENGTH_INDEFINITE).also { it.show() } }

    @StringRes
    private fun TasksView.TasksMessage.textRes() = when (this) {
        NoMessage -> 0
        TaskMarkedCompleted -> R.string.task_marked_complete
        TaskMarkedActive -> R.string.task_marked_active
        CompletedTasksCleared -> R.string.completed_tasks_cleared
        SuccessfullySaved -> R.string.successfully_saved_task_message
        LoadingTasksError -> R.string.loading_tasks_error
    }

    private class TasksAdapter(tasks: List<Task>, private val itemListener: TaskItemListener) : BaseAdapter() {

        var tasks: List<Task> = tasks
            set(tasks) {
                field = tasks
                notifyDataSetChanged()
            }

        override fun getCount() = tasks.size

        override fun getItem(i: Int) = tasks[i]

        override fun getItemId(i: Int) = i.toLong()

        override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
            val task = getItem(i)
            val rowView = view ?: LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.task_item, viewGroup, false)

            with(rowView.findViewById<TextView>(R.id.title)) {
                text = task.titleForList
            }

            with(rowView.findViewById<CheckBox>(R.id.complete)) {
                // Active/completed task UI
                isChecked = task.isCompleted
                val rowViewBackground =
                    if (task.isCompleted) R.drawable.list_completed_touch_feedback
                    else R.drawable.touch_feedback
                rowView.setBackgroundResource(rowViewBackground)
                setOnClickListener {
                    if (!task.isCompleted) {
                        itemListener.onCompleteTaskClick(task)
                    } else {
                        itemListener.onActivateTaskClick(task)
                    }
                }
            }
            rowView.setOnClickListener { itemListener.onTaskClick(task) }
            return rowView
        }
    }

    interface TaskItemListener {

        fun onTaskClick(clickedTask: Task)

        fun onCompleteTaskClick(completedTask: Task)

        fun onActivateTaskClick(activatedTask: Task)
    }

    companion object {

        fun newInstance() = TasksFragment()
    }

}
