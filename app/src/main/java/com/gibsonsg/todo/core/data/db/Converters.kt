package com.gibsonsg.todo.core.data.db

import androidx.room.TypeConverter
import com.gibsonsg.todo.core.domain.model.AnchorType
import com.gibsonsg.todo.core.domain.model.Priority
import com.gibsonsg.todo.core.domain.model.TaskStatus

class Converters {
    @TypeConverter
    fun priorityToInt(priority: Priority): Int = priority.sortWeight

    @TypeConverter
    fun intToPriority(value: Int): Priority =
        Priority.entries.firstOrNull { it.sortWeight == value } ?: Priority.NONE

    @TypeConverter
    fun statusToInt(status: TaskStatus): Int = status.ordinal

    @TypeConverter
    fun intToStatus(value: Int): TaskStatus = TaskStatus.entries.getOrElse(value) { TaskStatus.TODO }

    @TypeConverter
    fun anchorTypeToString(anchorType: AnchorType): String = anchorType.name

    @TypeConverter
    fun stringToAnchorType(value: String): AnchorType = AnchorType.valueOf(value)
}
