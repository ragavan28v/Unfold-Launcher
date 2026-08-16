package com.unfold.core.data.repositoryimpl

import com.unfold.core.data.local.dao.NoteDao
import com.unfold.core.data.local.entity.NoteEntity
import com.unfold.core.domain.model.Note
import com.unfold.core.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getNotes(): Flow<List<Note>> {
        return noteDao.getNotes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getNoteById(id: String): Note? {
        return noteDao.getNoteById(id)?.toDomain()
    }

    override suspend fun saveNote(note: Note) {
        noteDao.insertNote(NoteEntity.fromDomain(note))
    }

    override suspend fun deleteNote(id: String) {
        noteDao.deleteNote(id)
    }
}
