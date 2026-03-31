package com.nele.reader.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nele.reader.data.FileRepository
import com.nele.reader.model.FoldSymbols
import com.nele.reader.model.MdFile
import com.nele.reader.model.SyntaxColors
import com.nele.reader.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val loading: Boolean = false,
    val currentContent: String = "",
    val currentFile: MdFile? = null,
    val isEditing: Boolean = false,
    val editText: String = "",
    val error: String? = null,
    val saveSuccess: Boolean = false
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    val repo = FileRepository(application)

    val themeMode: StateFlow<ThemeMode> = repo.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }

    val syntaxColors: StateFlow<SyntaxColors> = repo.syntaxColors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SyntaxColors())

    fun setSyntaxColors(colors: SyntaxColors) = viewModelScope.launch { repo.setSyntaxColors(colors) }

    val foldSymbols: StateFlow<FoldSymbols> = repo.foldSymbols
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FoldSymbols())

    fun setFoldSymbols(symbols: FoldSymbols) = viewModelScope.launch { repo.setFoldSymbols(symbols) }

    val allFiles: StateFlow<List<MdFile>> = combine(
        repo.localFiles, repo.remoteUrls
    ) { local, remote -> local + remote }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFiles: StateFlow<List<MdFile>> = allFiles
        .map { files -> files.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentFiles: StateFlow<List<MdFile>> = allFiles
        .map { files ->
            files.filter { it.lastOpenedAt > 0L }
                .sortedByDescending { it.lastOpenedAt }
                .take(10)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    // Open file
    fun openFile(file: MdFile) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val content = if (file.isRemote) {
                    repo.fetchRemoteFile(file.url!!)
                } else {
                    repo.readLocalFile(Uri.parse(file.uri!!))
                }
                _ui.value = _ui.value.copy(
                    loading = false,
                    currentContent = content,
                    currentFile = file,
                    isEditing = !file.isRemote && !file.isReadOnly,   // edit by default for local files
                    editText = content
                )
                // Record open timestamp (fire-and-forget)
                launch { repo.recordOpen(file.id) }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun toggleFavorite(file: MdFile) = viewModelScope.launch {
        repo.toggleFavorite(file.id)
    }

    fun startEditing() {
        _ui.value = _ui.value.copy(isEditing = true, editText = _ui.value.currentContent)
    }

    fun updateEditText(text: String) {
        _ui.value = _ui.value.copy(editText = text)
    }

    fun cancelEditing() {
        _ui.value = _ui.value.copy(isEditing = false, editText = _ui.value.currentContent)
    }

    fun saveFile() {
        val file = _ui.value.currentFile ?: return
        if (file.isReadOnly) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true)
            val ok = repo.writeLocalFile(Uri.parse(file.uri!!), _ui.value.editText)
            _ui.value = _ui.value.copy(
                loading = false,
                currentContent = if (ok) _ui.value.editText else _ui.value.currentContent,
                isEditing = false,
                saveSuccess = ok,
                error = if (!ok) "Failed to save file" else null
            )
        }
    }

    fun addLocalFile(uri: Uri, displayName: String) = viewModelScope.launch {
        repo.addLocalFile(uri, displayName)
    }

    fun addRemoteUrl(url: String) = viewModelScope.launch {
        val name = url.substringAfterLast("/").ifBlank { url }
        repo.addRemoteUrl(url, name)
    }

    fun removeFile(file: MdFile) = viewModelScope.launch {
        if (file.isRemote) repo.removeRemoteUrl(file.id)
        else repo.removeLocalFile(file.id)
        if (_ui.value.currentFile?.id == file.id) {
            _ui.value = UiState()
        }
    }

    fun clearError() { _ui.value = _ui.value.copy(error = null) }
    fun clearSaveSuccess() { _ui.value = _ui.value.copy(saveSuccess = false) }
    fun closeFile() { _ui.value = UiState() }
}
