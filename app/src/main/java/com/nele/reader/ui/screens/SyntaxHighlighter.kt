package com.nele.reader.ui.screens

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.nele.reader.model.FoldSymbols
import com.nele.reader.model.SyntaxColors

/**
 * Builds an AnnotatedString with syntax highlighting spans for Markdown.
 * The output has the same character count as the input (only styles, no character changes),
 * so OffsetMapping.Identity is valid.
 */
fun buildAnnotatedString(
    text: String,
    colors: SyntaxColors,
    foldSymbols: FoldSymbols
): AnnotatedString = buildAnnotatedString {
    append(text)

    val lines = text.lines()
    var offset = 0

    for (line in lines) {
        val lineStart = offset
        val lineEnd = offset + line.length

        // Headings: lines starting with # characters
        val headingMatch = Regex("^(#{1,6})\\s").find(line)
        if (headingMatch != null) {
            addStyle(
                SpanStyle(color = colors.heading, fontWeight = FontWeight.Bold),
                lineStart,
                lineEnd
            )
        } else {
            // Blockquote: lines starting with >
            if (line.trimStart().startsWith(">")) {
                addStyle(
                    SpanStyle(color = colors.blockquote, fontStyle = FontStyle.Italic),
                    lineStart,
                    lineEnd
                )
            }

            // List markers: lines starting with - , * , or number.
            val listMatch = Regex("^(\\s*)([-*]|\\d+\\.)\\s").find(line)
            if (listMatch != null) {
                val markerEnd = lineStart + listMatch.groups[0]!!.range.last + 1
                addStyle(
                    SpanStyle(color = colors.listMarker),
                    lineStart + (listMatch.groups[1]?.range?.last?.plus(1) ?: 0),
                    markerEnd
                )
            }
        }

        offset = lineEnd + 1 // +1 for newline character
    }

    // Code blocks: ```...``` (multiline) — apply first so inline code doesn't override
    val codeBlockRegex = Regex("```[\\s\\S]*?```", RegexOption.DOT_MATCHES_ALL)
    for (match in codeBlockRegex.findAll(text)) {
        addStyle(
            SpanStyle(
                color = colors.code,
                fontFamily = FontFamily.Monospace
            ),
            match.range.first,
            match.range.last + 1
        )
    }

    // Inline code: `...`
    val inlineCodeRegex = Regex("`[^`\n]+`")
    for (match in inlineCodeRegex.findAll(text)) {
        addStyle(
            SpanStyle(
                color = colors.code,
                fontFamily = FontFamily.Monospace
            ),
            match.range.first,
            match.range.last + 1
        )
    }

    // Bold: **...**
    val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    for (match in boldRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.bold, fontWeight = FontWeight.Bold),
            match.range.first,
            match.range.last + 1
        )
    }

    // Italic: *...* or _..._ (not inside **)
    val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")
    for (match in italicRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.italic, fontStyle = FontStyle.Italic),
            match.range.first,
            match.range.last + 1
        )
    }

    // Links: [text](url)
    val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    for (match in linkRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.link),
            match.range.first,
            match.range.last + 1
        )
    }

    // Fold open symbol
    if (foldSymbols.openSymbol.isNotEmpty()) {
        val escapedOpen = Regex.escape(foldSymbols.openSymbol)
        val foldOpenRegex = Regex(escapedOpen)
        for (match in foldOpenRegex.findAll(text)) {
            addStyle(
                SpanStyle(color = colors.foldOpen, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }

    // Fold close symbol
    if (foldSymbols.closeSymbol.isNotEmpty()) {
        val escapedClose = Regex.escape(foldSymbols.closeSymbol)
        val foldCloseRegex = Regex(escapedClose)
        for (match in foldCloseRegex.findAll(text)) {
            addStyle(
                SpanStyle(color = colors.foldClose, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }
    }
}

/**
 * VisualTransformation that applies Markdown syntax highlighting.
 * Only adds styles (no character changes), so OffsetMapping.Identity is valid.
 */
class SyntaxHighlightTransformation(
    private val colors: SyntaxColors,
    private val foldSymbols: FoldSymbols
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString(text.text, colors, foldSymbols)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

/**
 * Finds foldable block ranges in [text] delimited by [symbols].
 * Returns character ranges from each openSymbol start to its matching closeSymbol end.
 */
fun findFoldableBlocks(text: String, symbols: FoldSymbols): List<IntRange> {
    if (symbols.openSymbol.isEmpty() || symbols.closeSymbol.isEmpty()) return emptyList()
    val blocks = mutableListOf<IntRange>()
    var searchFrom = 0
    while (true) {
        val openIdx = text.indexOf(symbols.openSymbol, searchFrom)
        if (openIdx == -1) break
        val closeIdx = text.indexOf(symbols.closeSymbol, openIdx + symbols.openSymbol.length)
        if (closeIdx == -1) break
        blocks.add(openIdx until closeIdx + symbols.closeSymbol.length)
        searchFrom = closeIdx + symbols.closeSymbol.length
    }
    return blocks
}
