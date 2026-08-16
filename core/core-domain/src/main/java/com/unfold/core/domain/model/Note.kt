package com.unfold.core.domain.model

data class Note(
    val id: String,
    val text: String,
    val lastModified: Long,
    val pinned: Boolean = false
)
