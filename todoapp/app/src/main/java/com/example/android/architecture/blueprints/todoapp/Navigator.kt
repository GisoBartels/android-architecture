package com.example.android.architecture.blueprints.todoapp

interface Navigator {

    fun navToAddTask()
    fun navToTaskDetails(taskId: String)

}