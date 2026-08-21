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
import com.unfold.core.domain.repository.AppRepository
import com.unfold.core.domain.usecase.GetInstalledAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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

enum class FileCategory { IMAGE, VIDEO, AUDIO, DOCUMENT }

data class UniversalSearchFile(
    val id: Long,
    val name: String,
    val uri: Uri,
    val mimeType: String?,
    val folderPath: String?,
    val category: FileCategory
)

data class UniversalSearchFolder(
    val path: String,
    val name: String
)

data class UniversalSearchUiState(
    val query: String = "",
    val filteredApps: List<AppInfo> = emptyList(),
    val filteredContacts: List<UniversalSearchContact> = emptyList(),
    val filteredImages: List<UniversalSearchFile> = emptyList(),
    val filteredVideos: List<UniversalSearchFile> = emptyList(),
    val filteredAudio: List<UniversalSearchFile> = emptyList(),
    val filteredDocuments: List<UniversalSearchFile> = emptyList(),
    val filteredFolders: List<UniversalSearchFolder> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val iconPackPackage: String = ""
) {
    val hasAnyResults: Boolean
        get() = filteredApps.isNotEmpty() || filteredContacts.isNotEmpty() ||
            filteredImages.isNotEmpty() || filteredVideos.isNotEmpty() ||
            filteredAudio.isNotEmpty() || filteredDocuments.isNotEmpty() ||
            filteredFolders.isNotEmpty()
}

sealed interface UniversalSearchUiIntent {
    data class QueryChanged(val query: String) : UniversalSearchUiIntent
    data class QuerySubmitted(val query: String) : UniversalSearchUiIntent
    data class RecentSelected(val query: String) : UniversalSearchUiIntent
    data class ContactSelected(val contactId: Long) : UniversalSearchUiIntent
    data class AppSelected(val appId: String) : UniversalSearchUiIntent
}

@HiltViewModel
class UniversalSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val appRepository: AppRepository,
    private val themeRepository: com.unfold.core.domain.repository.ThemeRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        UniversalSearchUiState(recentSearches = loadRecentSearches())
    )
    val uiState: StateFlow<UniversalSearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var allApps: List<AppInfo> = emptyList()
    private var searchJob: kotlinx.coroutines.Job? = null

    init {
        searchQueryFlow
            .debounce(200L)
            .distinctUntilChanged()
            .onEach { query ->
                performSearch(query)
            }
            .launchIn(viewModelScope)

        getInstalledApps(includeHidden = false)
            .onEach { apps ->
                allApps = apps
                performSearch(searchQueryFlow.value)
            }
            .launchIn(viewModelScope)

        themeRepository.observeTheme()
            .onEach { config ->
                _uiState.value = _uiState.value.copy(
                    iconPackPackage = config.iconPackPackage
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
                searchQueryFlow.value = intent.query
            }

            is UniversalSearchUiIntent.QuerySubmitted -> {
                _uiState.value = _uiState.value.copy(query = intent.query)
                searchQueryFlow.value = intent.query
                viewModelScope.launch {
                    rememberQuery(intent.query)
                }
            }

            is UniversalSearchUiIntent.RecentSelected -> {
                _uiState.value = _uiState.value.copy(query = intent.query)
                searchQueryFlow.value = intent.query
            }

            is UniversalSearchUiIntent.ContactSelected -> rememberContact(intent.contactId)

            is UniversalSearchUiIntent.AppSelected -> {
                viewModelScope.launch {
                    appRepository.recordLaunch(intent.appId)
                }
            }
        }
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            val filteredApps = if (query.isBlank()) {
                allApps.filter { it.lastUsedTimestamp > 0L || it.launchCount > 0L }
                    .sortedWith(
                    compareByDescending<AppInfo> { it.lastUsedTimestamp }
                        .thenByDescending { it.launchCount }
                        .thenBy { it.label.lowercase() }
                    ).take(MAX_INITIAL_RESULTS)
            }
            else allApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true) ||
                    (it.customLabel?.contains(query, ignoreCase = true) == true)
            }.sortedBy { it.label.lowercase() }

            val filteredContacts = searchContacts(query)
            val filteredFilesRaw = searchFiles(query)

            val filteredImages = filteredFilesRaw.filter { it.category == FileCategory.IMAGE }
            val filteredVideos = filteredFilesRaw.filter { it.category == FileCategory.VIDEO }
            val filteredAudio = filteredFilesRaw.filter { it.category == FileCategory.AUDIO }
            val filteredDocuments = filteredFilesRaw.filter { it.category == FileCategory.DOCUMENT }

            val filteredFolders = emptyList<UniversalSearchFolder>()

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    filteredApps = filteredApps,
                    filteredContacts = filteredContacts,
                    filteredImages = filteredImages,
                    filteredVideos = filteredVideos,
                    filteredAudio = filteredAudio,
                    filteredDocuments = filteredDocuments,
                    filteredFolders = filteredFolders,
                    isLoading = false
                )
            }
        }
    }

    private suspend fun loadDeviceSources() = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(recentSearches = loadRecentSearches())
        }
        performSearch(searchQueryFlow.value)
    }

    private fun searchContacts(query: String): List<UniversalSearchContact> {
        val contacts = mutableListOf<UniversalSearchContact>()
        val resolver = context.contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val isInitialSearch = query.isBlank()
        val selection = if (isInitialSearch) null
        else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val selectionArgs = if (isInitialSearch) null else arrayOf("%$query%", "%$query%")
        val recentContactIds = loadRecentContacts()
        if (isInitialSearch && recentContactIds.isEmpty()) return contacts

        runCatching {
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                if (isInitialSearch) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
                else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = linkedSetOf<String>()

                while (cursor.moveToNext() && contacts.size < if (isInitialSearch) MAX_INITIAL_RESULTS else MAX_QUERY_CONTACTS) {
                    val id = cursor.getLong(idIndex)
                    val name = cursor.getString(nameIndex)?.trim().orEmpty()
                    val number = cursor.getString(numberIndex)?.trim()
                    val key = "$name|$number"
                    if (name.isNotBlank() && seen.add(key) &&
                        (!isInitialSearch || recentContactIds.contains(id))) {
                        contacts += UniversalSearchContact(id, name, number)
                    }
                }

                if (isInitialSearch) {
                    contacts.sortBy { recentContactIds.indexOf(it.id) }
                }
            }
        }
        return contacts
    }

    private fun searchFiles(query: String): List<UniversalSearchFile> {
        val files = mutableListOf<UniversalSearchFile>()
        if (query.isBlank()) return files
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
            MediaStore.MediaColumns.DATA
        )

        val extensions = listOf(".pdf", ".txt", ".json", ".docx", ".doc", ".xlsx", ".pptx", ".png", ".jpg", ".jpeg", ".svg", ".gif", ".mp4", ".mkv", ".mp3", ".wav")
        
        // Search by name or mime type (allows searching for "pdf", "image", etc.)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? OR ${MediaStore.MediaColumns.MIME_TYPE} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%")

        runCatching {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

                while (cursor.moveToNext() && files.size < 50) {
                    val id = cursor.getLong(idIndex)
                    val dataPath = cursor.getString(pathIndex)?.trim().orEmpty()
                    var name = cursor.getString(nameIndex)?.trim()
                    
                    if (name.isNullOrBlank() && dataPath.isNotBlank()) {
                        name = dataPath.substringAfterLast('/')
                    }

                    if (name.isNullOrBlank()) continue
                    
                    val lowerName = name.lowercase()
                    if (!extensions.any { lowerName.endsWith(it) }) continue

                    val mimeType = cursor.getString(mimeIndex)?.trim()
                    val folderPath = if (dataPath.contains('/')) dataPath.substringBeforeLast('/') else null

                    val category = when {
                        mimeType?.startsWith("image/", ignoreCase = true) == true || lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".svg") || lowerName.endsWith(".gif") -> FileCategory.IMAGE
                        mimeType?.startsWith("video/", ignoreCase = true) == true || lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") -> FileCategory.VIDEO
                        mimeType?.startsWith("audio/", ignoreCase = true) == true || lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") -> FileCategory.AUDIO
                        else -> FileCategory.DOCUMENT
                    }

                    files += UniversalSearchFile(
                        id = id,
                        name = name,
                        uri = ContentUris.withAppendedId(collection, id),
                        mimeType = mimeType,
                        folderPath = folderPath,
                        category = category
                    )
                }
            }
        }
        return files
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

    private fun rememberContact(contactId: Long) {
        val updated = loadRecentContacts().toMutableList()
        updated.remove(contactId)
        updated.add(0, contactId)
        prefs.edit()
            .putString(KEY_RECENT_CONTACTS, updated.take(MAX_RECENT_CONTACTS).joinToString(DELIMITER))
            .apply()
    }

    private fun loadRecentContacts(): List<Long> {
        return prefs.getString(KEY_RECENT_CONTACTS, "")
            ?.split(DELIMITER)
            ?.mapNotNull { it.toLongOrNull() }
            ?.distinct()
            ?.take(MAX_RECENT_CONTACTS)
            ?: emptyList()
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
        const val MAX_INITIAL_RESULTS = 8
        const val MAX_QUERY_CONTACTS = 15
        const val MAX_RECENT_CONTACTS = 8
        const val KEY_RECENT_CONTACTS = "recent_contacts"
        const val DELIMITER = "\n"
    }
}

