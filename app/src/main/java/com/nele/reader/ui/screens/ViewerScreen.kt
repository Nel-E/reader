package com.nele.reader.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nele.reader.model.FoldSymbols
import com.nele.reader.model.SyntaxColors
import com.nele.reader.ui.ReaderViewModel
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import io.noties.markwon.linkify.LinkifyPlugin
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(vm: ReaderViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val syntaxColors by vm.syntaxColors.collectAsStateWithLifecycle()
    val foldSymbols by vm.foldSymbols.collectAsStateWithLifecycle()
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
                        syntaxColors = syntaxColors,
                        foldSymbols = foldSymbols,
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
private fun EditorPane(
    text: String,
    onTextChange: (String) -> Unit,
    syntaxColors: SyntaxColors,
    foldSymbols: FoldSymbols,
    modifier: Modifier = Modifier
) {
    // Track folded block ranges (character ranges in raw text)
    var foldedRanges by remember { mutableStateOf(setOf<IntRange>()) }

    // TextFieldValue to preserve cursor position
    var tfValue by remember { mutableStateOf(TextFieldValue(text)) }
    // When text changes externally, update content but preserve selection
    LaunchedEffect(text) {
        if (tfValue.text != text) tfValue = TextFieldValue(text)
    }

    val colorScheme = MaterialTheme.colorScheme
    val foldableBlocks = remember(text, foldSymbols) { findFoldableBlocks(text, foldSymbols) }

    Row(modifier = modifier) {
        // Fold gutter: narrow column on the left
        if (foldableBlocks.isNotEmpty()) {
            FoldGutter(
                text = text,
                foldableBlocks = foldableBlocks,
                foldedRanges = foldedRanges,
                onToggleFold = { block ->
                    foldedRanges = if (foldedRanges.any { it == block }) {
                        foldedRanges.filter { it != block }.toSet()
                    } else {
                        foldedRanges.toMutableSet().also { it.add(block) }
                    }
                },
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight()
                    .background(colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )
        }

        // BasicTextField with syntax highlighting visual transformation
        BasicTextField(
            value = tfValue,
            onValueChange = { new ->
                tfValue = new
                onTextChange(new.text)
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(8.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = colorScheme.onBackground
            ),
            cursorBrush = SolidColor(colorScheme.primary),
            visualTransformation = SyntaxHighlightTransformation(syntaxColors, foldSymbols),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize()) {
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun FoldGutter(
    text: String,
    foldableBlocks: List<IntRange>,
    foldedRanges: Set<IntRange>,
    onToggleFold: (IntRange) -> Unit,
    modifier: Modifier = Modifier
) {
    // Find which line numbers contain fold open symbols
    val lines = text.lines()
    val lineStarts = mutableListOf<Int>()
    var pos = 0
    for (line in lines) {
        lineStarts.add(pos)
        pos += line.length + 1
    }

    Column(modifier = modifier) {
        lines.forEachIndexed { lineIndex, _ ->
            val lineStart = lineStarts[lineIndex]
            // Find if any foldable block starts on this line
            val blockOnLine = foldableBlocks.firstOrNull { block ->
                block.first >= lineStart && block.first < lineStart + lines[lineIndex].length + 1
            }
            if (blockOnLine != null) {
                val isFolded = foldedRanges.any { it == blockOnLine }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggleFold(blockOnLine) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isFolded) "▸" else "▾",
                        style = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(28.dp))
            }
        }
    }
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
