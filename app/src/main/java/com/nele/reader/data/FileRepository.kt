package com.nele.reader.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nele.reader.model.FoldSymbols
import com.nele.reader.model.MdFile
import com.nele.reader.model.SyntaxColors
import com.nele.reader.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.graphics.Color

private val Context.dataStore by preferencesDataStore(name = "reader_prefs")

class FileRepository(private val context: Context) {

    private val httpClient = OkHttpClient()

    private val LOCAL_FILES_KEY   = stringPreferencesKey("local_files")
    private val REMOTE_URLS_KEY   = stringPreferencesKey("remote_urls")
    private val THEME_MODE_KEY    = stringPreferencesKey("theme_mode")
    private val SYNTAX_COLORS_KEY = stringPreferencesKey("syntax_colors")
    private val FOLD_SYMBOLS_KEY  = stringPreferencesKey("fold_symbols")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_MODE_KEY]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK"  -> ThemeMode.DARK
            else    -> ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }

    // ── Syntax Colors ────────────────────────────────────────────────────────

    val syntaxColors: Flow<SyntaxColors> = context.dataStore.data.map { prefs ->
        val json = prefs[SYNTAX_COLORS_KEY]
        if (json == null) {
            SyntaxColors()
        } else {
            try {
                val obj = JSONObject(json)
                SyntaxColors(
                    heading    = Color(obj.optLong("heading",    0xFF1565C0L).toULong()),
                    bold       = Color(obj.optLong("bold",       0xFFAD1457L).toULong()),
                    italic     = Color(obj.optLong("italic",     0xFF6A1B9AL).toULong()),
                    code       = Color(obj.optLong("code",       0xFF2E7D32L).toULong()),
                    link       = Color(obj.optLong("link",       0xFF00838FL).toULong()),
                    blockquote = Color(obj.optLong("blockquote", 0xFF4E342EL).toULong()),
                    listMarker = Color(obj.optLong("listMarker", 0xFF37474FL).toULong()),
                    foldOpen   = Color(obj.optLong("foldOpen",   0xFFE65100L).toULong()),
                    foldClose  = Color(obj.optLong("foldClose",  0xFFE65100L).toULong())
                )
            } catch (e: Exception) {
                SyntaxColors()
            }
        }
    }

    suspend fun setSyntaxColors(colors: SyntaxColors) {
        context.dataStore.edit { prefs ->
            val obj = JSONObject().apply {
                put("heading",    colors.heading.value.toLong())
                put("bold",       colors.bold.value.toLong())
                put("italic",     colors.italic.value.toLong())
                put("code",       colors.code.value.toLong())
                put("link",       colors.link.value.toLong())
                put("blockquote", colors.blockquote.value.toLong())
                put("listMarker", colors.listMarker.value.toLong())
                put("foldOpen",   colors.foldOpen.value.toLong())
                put("foldClose",  colors.foldClose.value.toLong())
            }
            prefs[SYNTAX_COLORS_KEY] = obj.toString()
        }
    }

    // ── Fold Symbols ─────────────────────────────────────────────────────────

    val foldSymbols: Flow<FoldSymbols> = context.dataStore.data.map { prefs ->
        val json = prefs[FOLD_SYMBOLS_KEY]
        if (json == null) {
            FoldSymbols()
        } else {
            try {
                val obj = JSONObject(json)
                FoldSymbols(
                    openSymbol  = obj.optString("open",  "{{"),
                    closeSymbol = obj.optString("close", "}}")
                )
            } catch (e: Exception) {
                FoldSymbols()
            }
        }
    }

    suspend fun setFoldSymbols(symbols: FoldSymbols) {
        context.dataStore.edit { prefs ->
            val obj = JSONObject().apply {
                put("open",  symbols.openSymbol)
                put("close", symbols.closeSymbol)
            }
            prefs[FOLD_SYMBOLS_KEY] = obj.toString()
        }
    }

    // ── Local files ──────────────────────────────────────────────────────────

    val localFiles: Flow<List<MdFile>> = context.dataStore.data.map { prefs ->
        val json = prefs[LOCAL_FILES_KEY] ?: "[]"
        parseJsonArray(json) { obj ->
            MdFile(
                id           = obj.getString("id"),
                displayName  = obj.getString("displayName"),
                isRemote     = false,
                uri          = obj.getString("uri"),
                isFavorite   = obj.optBoolean("isFavorite", false),
                lastOpenedAt = obj.optLong("lastOpenedAt", 0L)
            )
        }
    }

    suspend fun addLocalFile(uri: Uri, displayName: String) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray(prefs[LOCAL_FILES_KEY] ?: "[]")
            val obj = JSONObject().apply {
                put("id", uri.toString())
                put("displayName", displayName)
                put("uri", uri.toString())
                put("isFavorite", false)
                put("lastOpenedAt", 0L)
            }
            arr.put(obj)
            prefs[LOCAL_FILES_KEY] = arr.toString()
        }
        context.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }

    suspend fun removeLocalFile(id: String) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray(prefs[LOCAL_FILES_KEY] ?: "[]")
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") != id) newArr.put(obj)
            }
            prefs[LOCAL_FILES_KEY] = newArr.toString()
        }
    }

    suspend fun readLocalFile(uri: Uri): String = withContext(Dispatchers.IO) {
        context.contentResolver.openInputStream(uri)
            ?.bufferedReader()?.readText() ?: ""
    }

    suspend fun writeLocalFile(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            context.contentResolver.openOutputStream(uri, "wt")
                ?.bufferedWriter()?.use { it.write(content) }
            true
        } catch (e: Exception) { false }
    }

    suspend fun createNewFile(dirUri: Uri, fileName: String, content: String): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val docDir = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, dirUri)
                    ?: return@withContext null
                val newFile = docDir.createFile("text/markdown", fileName)
                    ?: return@withContext null
                context.contentResolver.openOutputStream(newFile.uri)
                    ?.bufferedWriter()?.use { it.write(content) }
                newFile.uri
            } catch (e: Exception) { null }
        }

    // ── Remote URLs ───────────────────────────────────────────────────────────

    val remoteUrls: Flow<List<MdFile>> = context.dataStore.data.map { prefs ->
        val json = prefs[REMOTE_URLS_KEY] ?: "[]"
        parseJsonArray(json) { obj ->
            MdFile(
                id           = obj.getString("id"),
                displayName  = obj.getString("displayName"),
                isRemote     = true,
                url          = obj.getString("url"),
                isReadOnly   = true,
                isFavorite   = obj.optBoolean("isFavorite", false),
                lastOpenedAt = obj.optLong("lastOpenedAt", 0L)
            )
        }
    }

    suspend fun addRemoteUrl(url: String, displayName: String) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray(prefs[REMOTE_URLS_KEY] ?: "[]")
            val obj = JSONObject().apply {
                put("id", url)
                put("displayName", displayName)
                put("url", url)
                put("isFavorite", false)
                put("lastOpenedAt", 0L)
            }
            arr.put(obj)
            prefs[REMOTE_URLS_KEY] = arr.toString()
        }
    }

    suspend fun removeRemoteUrl(id: String) {
        context.dataStore.edit { prefs ->
            val arr = JSONArray(prefs[REMOTE_URLS_KEY] ?: "[]")
            val newArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") != id) newArr.put(obj)
            }
            prefs[REMOTE_URLS_KEY] = newArr.toString()
        }
    }

    suspend fun fetchRemoteFile(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string() ?: ""
            else throw Exception("HTTP ${response.code}: ${response.message}")
        }
    }

    // ── Favorites & Recents ───────────────────────────────────────────────────

    suspend fun toggleFavorite(id: String) {
        context.dataStore.edit { prefs ->
            // Try local files first
            val localJson = prefs[LOCAL_FILES_KEY] ?: "[]"
            val localArr = JSONArray(localJson)
            var foundLocal = false
            for (i in 0 until localArr.length()) {
                val obj = localArr.getJSONObject(i)
                if (obj.getString("id") == id) {
                    obj.put("isFavorite", !obj.optBoolean("isFavorite", false))
                    foundLocal = true
                    break
                }
            }
            if (foundLocal) {
                prefs[LOCAL_FILES_KEY] = localArr.toString()
                return@edit
            }
            // Try remote files
            val remoteJson = prefs[REMOTE_URLS_KEY] ?: "[]"
            val remoteArr = JSONArray(remoteJson)
            for (i in 0 until remoteArr.length()) {
                val obj = remoteArr.getJSONObject(i)
                if (obj.getString("id") == id) {
                    obj.put("isFavorite", !obj.optBoolean("isFavorite", false))
                    break
                }
            }
            prefs[REMOTE_URLS_KEY] = remoteArr.toString()
        }
    }

    suspend fun recordOpen(id: String) {
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            // Try local files first
            val localJson = prefs[LOCAL_FILES_KEY] ?: "[]"
            val localArr = JSONArray(localJson)
            var foundLocal = false
            for (i in 0 until localArr.length()) {
                val obj = localArr.getJSONObject(i)
                if (obj.getString("id") == id) {
                    obj.put("lastOpenedAt", now)
                    foundLocal = true
                    break
                }
            }
            if (foundLocal) {
                prefs[LOCAL_FILES_KEY] = localArr.toString()
                return@edit
            }
            // Try remote files
            val remoteJson = prefs[REMOTE_URLS_KEY] ?: "[]"
            val remoteArr = JSONArray(remoteJson)
            for (i in 0 until remoteArr.length()) {
                val obj = remoteArr.getJSONObject(i)
                if (obj.getString("id") == id) {
                    obj.put("lastOpenedAt", now)
                    break
                }
            }
            prefs[REMOTE_URLS_KEY] = remoteArr.toString()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun <T> parseJsonArray(json: String, mapper: (JSONObject) -> T): List<T> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { mapper(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }
}
