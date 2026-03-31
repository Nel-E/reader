package com.nele.reader.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nele.reader.model.MdFile
import com.nele.reader.ui.ReaderViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: ReaderViewModel, onOpenFile: (MdFile) -> Unit, onOpenSettings: () -> Unit = {}) {
    val files by vm.allFiles.collectAsStateWithLifecycle()
    val favoriteFiles by vm.favoriteFiles.collectAsStateWithLifecycle()
    val recentFiles by vm.recentFiles.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var fileToAction by remember { mutableStateOf<MdFile?>(null) }
    var showLongPressMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Favourites", "Recent")

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val name = it.lastPathSegment?.substringAfterLast("/") ?: it.toString()
            vm.addLocalFile(it, name)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reader", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { filePicker.launch(arrayOf("text/*")) }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Open local file")
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.AddLink, contentDescription = "Add network URL")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Tab row
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // All tab
                    if (files.isEmpty()) {
                        EmptyState(
                            modifier = Modifier.fillMaxSize(),
                            onAddLocal = { filePicker.launch(arrayOf("text/*")) },
                            onAddRemote = { showAddDialog = true }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val local = files.filter { !it.isRemote }
                            val remote = files.filter { it.isRemote }

                            if (local.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "Local Files", icon = Icons.Default.Storage)
                                }
                                items(local, key = { it.id }) { file ->
                                    FileCard(
                                        file = file,
                                        onClick = { onOpenFile(file) },
                                        onLongClick = {
                                            fileToAction = file
                                            showLongPressMenu = true
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(file) }
                                    )
                                }
                            }

                            if (remote.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(8.dp))
                                    SectionHeader(title = "Network Files", icon = Icons.Default.Cloud)
                                }
                                items(remote, key = { it.id }) { file ->
                                    FileCard(
                                        file = file,
                                        onClick = { onOpenFile(file) },
                                        onLongClick = {
                                            fileToAction = file
                                            showLongPressMenu = true
                                        },
                                        onToggleFavorite = { vm.toggleFavorite(file) }
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Favourites tab
                    if (favoriteFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.StarOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No favourites yet — long-press a file to favourite it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(favoriteFiles, key = { it.id }) { file ->
                                FileCard(
                                    file = file,
                                    onClick = { onOpenFile(file) },
                                    onLongClick = {
                                        fileToAction = file
                                        showLongPressMenu = true
                                    },
                                    onToggleFavorite = { vm.toggleFavorite(file) }
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Recent tab
                    if (recentFiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No recently opened files",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(recentFiles, key = { it.id }) { file ->
                                FileCard(
                                    file = file,
                                    onClick = { onOpenFile(file) },
                                    onLongClick = {
                                        fileToAction = file
                                        showLongPressMenu = true
                                    },
                                    onToggleFavorite = { vm.toggleFavorite(file) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddUrlDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { url ->
                vm.addRemoteUrl(url)
                showAddDialog = false
            }
        )
    }

    // Long-press menu: Favourite/Unfavourite + Remove
    if (showLongPressMenu && fileToAction != null) {
        val file = fileToAction!!
        AlertDialog(
            onDismissRequest = {
                showLongPressMenu = false
                fileToAction = null
            },
            title = { Text(file.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            vm.toggleFavorite(file)
                            showLongPressMenu = false
                            fileToAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (file.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (file.isFavorite) "Unfavourite" else "Favourite")
                    }
                    TextButton(
                        onClick = {
                            vm.removeFile(file)
                            showLongPressMenu = false
                            fileToAction = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showLongPressMenu = false
                    fileToAction = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCard(
    file: MdFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (file.isRemote)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (file.isRemote) Icons.Default.Cloud else Icons.Default.Description,
                    contentDescription = null,
                    tint = if (file.isRemote)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (file.isRemote && file.url != null) {
                    Text(
                        text = file.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Star icon button
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (file.isFavorite) Icons.Default.Star else Icons.Default.StarOutline,
                    contentDescription = if (file.isFavorite) "Unfavourite" else "Favourite",
                    tint = if (file.isFavorite)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    onAddLocal: () -> Unit,
    onAddRemote: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Article,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No files yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Add a local .md file or a network URL",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onAddLocal) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Local File")
            }
            Button(onClick = onAddRemote) {
                Icon(Icons.Default.AddLink, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Network URL")
            }
        }
    }
}

@Composable
private fun AddUrlDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Network File") },
        text = {
            Column {
                Text(
                    "Enter a URL to a .md file (http/https, raw GitHub, etc.)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    label = { Text("URL") },
                    placeholder = { Text("https://raw.githubusercontent.com/...") },
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (url.isBlank()) error = "URL cannot be empty"
                else if (!url.startsWith("http://") && !url.startsWith("https://"))
                    error = "URL must start with http:// or https://"
                else onAdd(url.trim())
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
