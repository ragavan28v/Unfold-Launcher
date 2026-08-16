package com.unfold.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.unfold.core.domain.model.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val lastModified: Long,
    val pinned: Boolean
) {
    fun toDomain(): Note {
        return Note(
            id = id,
            text = text,
            lastModified = lastModified,
            pinned = pinned
        )
    }

    companion object {
        fun fromDomain(note: Note): NoteEntity {
            return NoteEntity(
                id = note.id,
                text = note.text,
                lastModified = note.lastModified,
                pinned = note.pinned
            )
        }
    }
}
