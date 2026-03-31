package com.nele.reader.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nele.reader.model.MdFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "reader_prefs")

class FileRepository(private val context: Context) {

    private val httpClient = OkHttpClient()

    private val LOCAL_FILES_KEY  = stringPreferencesKey("local_files")
    private val REMOTE_URLS_KEY  = stringPreferencesKey("remote_urls")

    val localFiles: Flow<List<MdFile>> = context.dataStore.data.map { prefs ->
        val json = prefs[LOCAL_FILES_KEY] ?: "[]"
        parseJsonArray(json) { obj ->
            MdFile(
                id = obj.getString("id"),
                displayName = obj.getString("displayName"),
                isRemote = false,
                uri = obj.getString("uri")
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

    val remoteUrls: Flow<List<MdFile>> = context.dataStore.data.map { prefs ->
        val json = prefs[REMOTE_URLS_KEY] ?: "[]"
        parseJsonArray(json) { obj ->
            MdFile(
                id = obj.getString("id"),
                displayName = obj.getString("displayName"),
                isRemote = true,
                url = obj.getString("url"),
                isReadOnly = true
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

    private fun <T> parseJsonArray(json: String, mapper: (JSONObject) -> T): List<T> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { mapper(arr.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
    }
}
