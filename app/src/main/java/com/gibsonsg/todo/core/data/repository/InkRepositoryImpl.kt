package com.gibsonsg.todo.core.data.repository

import com.gibsonsg.todo.core.data.db.dao.InkPageDao
import com.gibsonsg.todo.core.data.db.entity.InkPageEntity
import com.gibsonsg.todo.core.domain.model.AnchorType
import com.gibsonsg.todo.core.domain.model.InkPage
import com.gibsonsg.todo.core.domain.model.Stroke
import com.gibsonsg.todo.util.IdGenerator
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class InkRepositoryImpl @Inject constructor(
    private val inkPageDao: InkPageDao
) : InkRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override fun observePagesForAnchor(anchorType: AnchorType, anchorId: String): Flow<List<InkPage>> =
        inkPageDao.observePagesForAnchor(anchorType.name, anchorId).map { list -> list.map { it.toDomain(json) } }

    override fun observeDatesWithInk(): Flow<Set<LocalDate>> =
        inkPageDao.observeAnchorIdsWithInk(AnchorType.DAY.name)
            .map { ids -> ids.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet() }

    override suspend fun appendStrokeToDayPage(date: LocalDate, stroke: Stroke) {
        val anchorId = date.toString()
        val existing = inkPageDao.getPagesForAnchor(AnchorType.DAY.name, anchorId).firstOrNull()
        if (existing != null) {
            appendStrokeToEntity(existing, stroke)
        } else {
            val now = Instant.now().toEpochMilli()
            val entity = InkPageEntity(
                id = IdGenerator.newId(),
                anchorType = AnchorType.DAY.name,
                anchorId = anchorId,
                createdAt = now,
                updatedAt = now,
                strokesJson = json.encodeToString(listOf(stroke))
            )
            inkPageDao.upsert(entity)
        }
    }

    override suspend fun appendStrokeToPage(pageId: String, stroke: Stroke) {
        val existing = inkPageDao.getById(pageId) ?: return
        appendStrokeToEntity(existing, stroke)
    }

    override suspend fun startNewTaskPage(taskId: String): InkPage {
        val now = Instant.now().toEpochMilli()
        val entity = InkPageEntity(
            id = IdGenerator.newId(),
            anchorType = AnchorType.TASK.name,
            anchorId = taskId,
            createdAt = now,
            updatedAt = now,
            strokesJson = json.encodeToString(emptyList<Stroke>())
        )
        inkPageDao.upsert(entity)
        return entity.toDomain(json)
    }

    private suspend fun appendStrokeToEntity(entity: InkPageEntity, stroke: Stroke) {
        val strokes = decodeStrokes(json, entity.strokesJson) + stroke
        inkPageDao.update(
            entity.copy(
                strokesJson = json.encodeToString(strokes),
                updatedAt = Instant.now().toEpochMilli()
            )
        )
    }
}

private fun decodeStrokes(json: Json, raw: String): List<Stroke> =
    runCatching { json.decodeFromString<List<Stroke>>(raw) }.getOrDefault(emptyList())

private fun InkPageEntity.toDomain(json: Json): InkPage = InkPage(
    id = id,
    anchorType = AnchorType.valueOf(anchorType),
    anchorId = anchorId,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    strokes = decodeStrokes(json, strokesJson),
    isArchived = isArchived,
    archivedAt = archivedAt?.let { Instant.ofEpochMilli(it) }
)
