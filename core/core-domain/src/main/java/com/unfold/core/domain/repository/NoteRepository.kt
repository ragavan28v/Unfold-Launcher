package com.unfold.core.domain.repository

import com.unfold.core.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: String): Note?
    suspend fun saveNote(note: Note)
    suspend fun deleteNote(id: String)
}
