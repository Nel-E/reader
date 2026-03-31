package com.nele.reader.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nele.reader.ui.ReaderViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import okhttp3.OkHttpClient
import io.noties.markwon.linkify.LinkifyPlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(vm: ReaderViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val markwon = remember { buildMarkwon(context) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(ui.saveSuccess) {
        if (ui.saveSuccess) {
            snackbarHostState.showSnackbar("File saved")
            vm.clearSaveSuccess()
        }
    }
    LaunchedEffect(ui.error) {
        if (ui.error != null) {
            snackbarHostState.showSnackbar(ui.error ?: "Error")
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        ui.currentFile?.displayName ?: "Viewer",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeFile(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (ui.isEditing) {
                        IconButton(onClick = { vm.cancelEditing() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = { vm.saveFile() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    } else {
                        if (ui.currentFile?.isReadOnly == false) {
                            IconButton(onClick = { vm.startEditing() }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                        if (ui.currentFile?.isRemote == true) {
                            IconButton(onClick = { ui.currentFile?.let { vm.openFile(it) } }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                ui.loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                ui.isEditing -> {
                    EditorPane(
                        text = ui.editText,
                        onTextChange = vm::updateEditText,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                ui.currentContent.isNotEmpty() -> {
                    MarkdownPane(
                        content = ui.currentContent,
                        markwon = markwon,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownPane(content: String, markwon: Markwon, modifier: Modifier = Modifier) {
    SelectionContainer {
        AndroidView(
            factory = { ctx ->
                android.widget.TextView(ctx).apply {
                    setPadding(24, 24, 24, 24)
                    textSize = 15f
                    setLineSpacing(4f, 1.2f)
                    setTextIsSelectable(true)
                }
            },
            update = { tv -> markwon.setMarkdown(tv, content) },
            modifier = modifier.verticalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun EditorPane(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier.padding(8.dp),
        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

private fun buildMarkwon(context: Context): Markwon = Markwon.builder(context)
    .usePlugin(StrikethroughPlugin.create())
    .usePlugin(TablePlugin.create(context))
    .usePlugin(HtmlPlugin.create())
    .usePlugin(ImagesPlugin.create { plugin ->
        plugin.addSchemeHandler(OkHttpNetworkSchemeHandler.create(OkHttpClient()))
    })
    .usePlugin(LinkifyPlugin.create())
    .build()
