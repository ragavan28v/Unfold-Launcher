package com.ragavan.unfold.ui.workspace

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class WorkspaceState {

    var currentPage by mutableStateOf(0)
        internal set

    var drawerProgress by mutableFloatStateOf(0f)
        internal set

    var isEditing by mutableStateOf(false)
        internal set

}