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
package com.example.android.architecture.blueprints.todoapp.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

/**
 * Model class for a Task.
 *
 * @param title       title of the task
 * @param description description of the task
 * @param id          id of the task
 */
@Entity(tableName = "tasks")
@Serializable
data class Task @JvmOverloads constructor(
        @ColumnInfo(name = "title") var title: String = "",
        @ColumnInfo(name = "description") var description: String = "",
        @PrimaryKey @ColumnInfo(name = "entryid") var id: String = UUID.randomUUID().toString()
) {

    /**
     * True if the task is completed, false if it's active.
     */
    @ColumnInfo(name = "completed") var isCompleted = false

    @Transient
    val titleForList: String
        get() = if (title.isNotEmpty()) title else description

    @Transient
    val isActive
        get() = !isCompleted

    @Transient
    val isEmpty
        get() = title.isEmpty() && description.isEmpty()
}