package com.gibsonsg.todo.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.gibsonsg.todo.core.data.db.entity.InkPageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InkPageDao {

    @Query(
        "SELECT * FROM ink_pages WHERE anchorType = :anchorType AND anchorId = :anchorId " +
            "AND isArchived = 0 ORDER BY createdAt ASC"
    )
    fun observePagesForAnchor(anchorType: String, anchorId: String): Flow<List<InkPageEntity>>

    @Query("SELECT * FROM ink_pages WHERE id = :id")
    suspend fun getById(id: String): InkPageEntity?

    @Query(
        "SELECT * FROM ink_pages WHERE anchorType = :anchorType AND anchorId = :anchorId " +
            "AND isArchived = 0 ORDER BY createdAt DESC"
    )
    suspend fun getPagesForAnchor(anchorType: String, anchorId: String): List<InkPageEntity>

    @Query("SELECT DISTINCT anchorId FROM ink_pages WHERE anchorType = :anchorType AND isArchived = 0")
    fun observeAnchorIdsWithInk(anchorType: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(inkPage: InkPageEntity)

    @Update
    suspend fun update(inkPage: InkPageEntity)

    @Query(
        "UPDATE ink_pages SET isArchived = 1, archivedAt = :archivedAt " +
            "WHERE anchorType = 'TASK' AND anchorId = :taskId"
    )
    suspend fun archivePagesForTask(taskId: String, archivedAt: Long)
}
