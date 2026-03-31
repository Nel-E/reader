package com.nele.reader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nele.reader.model.FoldSymbols
import com.nele.reader.model.SyntaxColors
import com.nele.reader.model.ThemeMode
import com.nele.reader.ui.ReaderViewModel

// Preset colour palette (16 Material swatches)
private val PRESET_COLORS = listOf(
    Color(0xFF1565C0), Color(0xFF0277BD), Color(0xFF00838F), Color(0xFF2E7D32),
    Color(0xFF558B2F), Color(0xFFAD1457), Color(0xFF6A1B9A), Color(0xFF4527A0),
    Color(0xFF37474F), Color(0xFF4E342E), Color(0xFFE65100), Color(0xFFF57F17),
    Color(0xFF212121), Color(0xFF757575), Color(0xFFBDBDBD), Color(0xFFFFFFFF)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: ReaderViewModel, onBack: () -> Unit) {
    val currentTheme by vm.themeMode.collectAsStateWithLifecycle()
    val syntaxColors by vm.syntaxColors.collectAsStateWithLifecycle()
    val foldSymbols by vm.foldSymbols.collectAsStateWithLifecycle()

    // Color picker state
    var colorPickerTarget by remember { mutableStateOf<String?>(null) }
    var currentPickerColor by remember { mutableStateOf(Color.Black) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── Appearance section ───────────────────────────────────────────
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeOption(
                        icon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) },
                        label = "Follow system",
                        description = "Matches your device theme",
                        selected = currentTheme == ThemeMode.SYSTEM,
                        onClick = { vm.setThemeMode(ThemeMode.SYSTEM) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    ThemeOption(
                        icon = { Icon(Icons.Default.Brightness7, contentDescription = null) },
                        label = "Light",
                        description = "Always use light theme",
                        selected = currentTheme == ThemeMode.LIGHT,
                        onClick = { vm.setThemeMode(ThemeMode.LIGHT) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    ThemeOption(
                        icon = { Icon(Icons.Default.Brightness4, contentDescription = null) },
                        label = "Dark",
                        description = "Always use dark theme",
                        selected = currentTheme == ThemeMode.DARK,
                        onClick = { vm.setThemeMode(ThemeMode.DARK) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Syntax colours section ───────────────────────────────────────
            Text(
                text = "Syntax colours",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    ColorRow(label = "Heading", color = syntaxColors.heading) {
                        currentPickerColor = syntaxColors.heading
                        colorPickerTarget = "heading"
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ColorRow(label = "Bold", color = syntaxColors.bold) {
                        currentPickerColor = syntaxColors.bold
                        colorPickerTarget = "bold"
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ColorRow(label = "Italic", color = syntaxColors.italic) {
                        currentPickerColor = syntaxColors.italic
                        colorPickerTarget = "italic"
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ColorRow(label = "Code", color = syntaxColors.code) {
                        currentPickerColor = syntaxColors.code
                        colorPickerTarget = "code"
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ColorRow(label = "Link", color = syntaxColors.link) {
                        currentPickerColor = syntaxColors.link
                        colorPickerTarget = "link"
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ColorRow(label = "Blockquote", color = syntaxColors.blockquote) {
                        currentPickerColor = syntaxColors.blockquote
                        colorPickerTarget = "blockquote"
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ColorRow(label = "List marker", color = syntaxColors.listMarker) {
                        currentPickerColor = syntaxColors.listMarker
                        colorPickerTarget = "listMarker"
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Folding section ──────────────────────────────────────────────
            Text(
                text = "Folding",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    var openSymbolText by remember(foldSymbols.openSymbol) {
                        mutableStateOf(foldSymbols.openSymbol)
                    }
                    var closeSymbolText by remember(foldSymbols.closeSymbol) {
                        mutableStateOf(foldSymbols.closeSymbol)
                    }

                    OutlinedTextField(
                        value = openSymbolText,
                        onValueChange = {
                            openSymbolText = it
                            vm.setFoldSymbols(foldSymbols.copy(openSymbol = it))
                        },
                        label = { Text("Open symbol") },
                        placeholder = { Text("{{") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = closeSymbolText,
                        onValueChange = {
                            closeSymbolText = it
                            vm.setFoldSymbols(foldSymbols.copy(closeSymbol = it))
                        },
                        label = { Text("Close symbol") },
                        placeholder = { Text("}}") },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Live preview
                    val open = openSymbolText.ifEmpty { "{{" }
                    val close = closeSymbolText.ifEmpty { "}}" }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$open content $close  →  folds to: $open ⋯ $close",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Color picker dialog
    if (colorPickerTarget != null) {
        ColorPickerDialog(
            initialColor = currentPickerColor,
            onDismiss = { colorPickerTarget = null },
            onColorSelected = { selected ->
                val updated = when (colorPickerTarget) {
                    "heading"    -> syntaxColors.copy(heading = selected)
                    "bold"       -> syntaxColors.copy(bold = selected)
                    "italic"     -> syntaxColors.copy(italic = selected)
                    "code"       -> syntaxColors.copy(code = selected)
                    "link"       -> syntaxColors.copy(link = selected)
                    "blockquote" -> syntaxColors.copy(blockquote = selected)
                    "listMarker" -> syntaxColors.copy(listMarker = selected)
                    else         -> syntaxColors
                }
                vm.setSyntaxColors(updated)
                colorPickerTarget = null
            }
        )
    }
}

@Composable
private fun ColorRow(label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selected by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose colour") },
        text = {
            Column {
                // 4 × 4 grid of preset colours
                val rows = PRESET_COLORS.chunked(4)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { color ->
                            val isSelected = selected == color
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { selected = color }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(selected) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ThemeOption(
    icon: @Composable () -> Unit,
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}
