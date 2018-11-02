package com.example.android.architecture.blueprints.todoapp

interface Navigator {

    fun navToAddTask()
    fun navToEditTask(taskId: String)
    fun navToTaskDetails(taskId: String)

    fun goBack()
    fun returnResultOk()

}