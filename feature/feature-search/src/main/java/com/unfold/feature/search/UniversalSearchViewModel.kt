package com.unfold.feature.search

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.ContactsContract
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unfold.core.domain.model.AppInfo
import com.unfold.core.domain.usecase.GetInstalledAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class UniversalSearchContact(
    val id: Long,
    val name: String,
    val phoneNumber: String?
)

data class UniversalSearchFile(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String?,
    val folderPath: String?
)

data class UniversalSearchFolder(
    val path: String,
    val name: String
)

data class UniversalSearchUiState(
    val query: String = "",
    val apps: List<AppInfo> = emptyList(),
    val contacts: List<UniversalSearchContact> = emptyList(),
    val files: List<UniversalSearchFile> = emptyList(),
    val folders: List<UniversalSearchFolder> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = true
) {
    val filteredApps: List<AppInfo>
        get() = filterApps(apps, query)

    val filteredContacts: List<UniversalSearchContact>
        get() = filterContacts(contacts, query)

    val filteredFiles: List<UniversalSearchFile>
        get() = filterFiles(files, query)

    val filteredFolders: List<UniversalSearchFolder>
        get() = filterFolders(folders, query)

    val hasAnyResults: Boolean
        get() = filteredApps.isNotEmpty() || filteredContacts.isNotEmpty() ||
            filteredFiles.isNotEmpty() || filteredFolders.isNotEmpty()

    companion object {
        private fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
            if (query.isBlank()) return apps.sortedBy { it.label.lowercase() }
            return apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }.sortedBy { it.label.lowercase() }
        }

        private fun filterContacts(
            contacts: List<UniversalSearchContact>,
            query: String
        ): List<UniversalSearchContact> {
            if (query.isBlank()) return contacts
            return contacts.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.phoneNumber?.contains(query, ignoreCase = true) == true
            }
        }

        private fun filterFiles(
            files: List<UniversalSearchFile>,
            query: String
        ): List<UniversalSearchFile> {
            if (query.isBlank()) return files
            return files.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.folderPath?.contains(query, ignoreCase = true) == true ||
                    it.mimeType?.contains(query, ignoreCase = true) == true
            }
        }

        private fun filterFolders(
            folders: List<UniversalSearchFolder>,
            query: String
        ): List<UniversalSearchFolder> {
            if (query.isBlank()) return folders
            return folders.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.path.contains(query, ignoreCase = true)
            }
        }
    }
}

sealed interface UniversalSearchUiIntent {
    data class QueryChanged(val query: String) : UniversalSearchUiIntent
    data class QuerySubmitted(val query: String) : UniversalSearchUiIntent
    data class RecentSelected(val query: String) : UniversalSearchUiIntent
}

@HiltViewModel
class UniversalSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getInstalledApps: GetInstalledAppsUseCase
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        UniversalSearchUiState(recentSearches = loadRecentSearches())
    )
    val uiState: StateFlow<UniversalSearchUiState> = _uiState.asStateFlow()

    init {
        getInstalledApps(includeHidden = false)
            .onEach { apps ->
                _uiState.value = _uiState.value.copy(
                    apps = apps,
                    recentSearches = loadRecentSearches(),
                    isLoading = false
                )
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            loadDeviceSources()
        }
    }

    fun refreshDeviceSources() {
        viewModelScope.launch {
            loadDeviceSources()
        }
    }

    fun onIntent(intent: UniversalSearchUiIntent) {
        when (intent) {
            is UniversalSearchUiIntent.QueryChanged -> {
                _uiState.value = _uiState.value.copy(query = intent.query)
            }

            is UniversalSearchUiIntent.QuerySubmitted -> {
                _uiState.value = _uiState.value.copy(query = intent.query)
                viewModelScope.launch {
                    rememberQuery(intent.query)
                }
            }

            is UniversalSearchUiIntent.RecentSelected -> {
                _uiState.value = _uiState.value.copy(query = intent.query)
            }
        }
    }

    private suspend fun loadDeviceSources() = withContext(Dispatchers.IO) {
        val contacts = loadContacts()
        val (files, folders) = loadFilesAndFolders()
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                contacts = contacts,
                files = files,
                folders = folders,
                recentSearches = loadRecentSearches()
            )
        }
    }

    private fun loadContacts(): List<UniversalSearchContact> {
        val contacts = mutableListOf<UniversalSearchContact>()
        val resolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        runCatching {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = linkedSetOf<String>()

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val number = cursor.getString(numberIndex)?.trim()
                    val key = "$name|$number"
                    if (name.isNotBlank() && seen.add(key)) {
                        contacts += UniversalSearchContact(
                            id = id,
                            name = name,
                            phoneNumber = number
                        )
                    }
                }
            }
        }

        return contacts
    }

    private fun loadFilesAndFolders(): Pair<List<UniversalSearchFile>, List<UniversalSearchFolder>> {
        val files = mutableListOf<UniversalSearchFile>()
        val folderMap = linkedMapOf<String, UniversalSearchFolder>()
        val resolver = context.contentResolver

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.RELATIVE_PATH
        )

        runCatching {
            resolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val mimeType = cursor.getString(mimeIndex)?.trim()
                    val relativePath = cursor.getString(pathIndex)?.trim()
                        ?.trimEnd('/')
                        ?.takeIf { it.isNotBlank() }

                    if (name.isNotBlank()) {
                        files += UniversalSearchFile(
                            id = id,
                            name = name,
                            uri = ContentUris.withAppendedId(collection, id),
                            mimeType = mimeType,
                            folderPath = relativePath
                        )
                    }

                    if (!relativePath.isNullOrBlank()) {
                        val folderName = relativePath.substringAfterLast('/', relativePath)
                        folderMap.putIfAbsent(
                            relativePath,
                            UniversalSearchFolder(
                                path = relativePath,
                                name = folderName.ifBlank { relativePath }
                            )
                        )
                    }
                }
            }
        }

        return files to folderMap.values.sortedBy { it.name.lowercase() }
    }

    private fun rememberQuery(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return

        val updated = loadRecentSearches().toMutableList()
        updated.removeAll { it.equals(normalized, ignoreCase = true) }
        updated.add(0, normalized)

        prefs.edit()
            .putString(KEY_RECENT_SEARCHES, updated.take(MAX_RECENTS).joinToString(DELIMITER))
            .apply()

        _uiState.value = _uiState.value.copy(recentSearches = loadRecentSearches())
    }

    private fun loadRecentSearches(): List<String> {
        return prefs.getString(KEY_RECENT_SEARCHES, "")
            ?.split(DELIMITER)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinctBy { it.lowercase() }
            ?.take(MAX_RECENTS)
            ?: emptyList()
    }

    private companion object {
        const val PREFS_NAME = "universal_search_preferences"
        const val KEY_RECENT_SEARCHES = "recent_searches"
        const val MAX_RECENTS = 8
        const val DELIMITER = "\n"
    }
}

