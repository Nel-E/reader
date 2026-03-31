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

// ─────────────────────────────────────────────────────────────────────────────
// Fold block detection
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Finds foldable block character ranges in [text] using [symbols].
 * Each range spans from the first char of the open symbol to the last char of
 * the matching close symbol (inclusive end, so range.last + 1 = exclusive end).
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

// ─────────────────────────────────────────────────────────────────────────────
// Combined VisualTransformation: folding + syntax highlighting
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A VisualTransformation that:
 *  1. Replaces folded block ranges with a placeholder ("open ⋯ close")
 *  2. Applies syntax-highlight SpanStyles to the resulting display string
 *
 * The placeholder has a different length than the folded content, so we build
 * a precise OffsetMapping that translates cursor offsets between the raw text
 * and the display text.
 */
class CombinedEditorTransformation(
    private val colors: SyntaxColors,
    private val foldSymbols: FoldSymbols,
    /** Sorted set of folded block ranges (from findFoldableBlocks). */
    private val foldedRanges: Set<IntRange>
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text

        if (foldedRanges.isEmpty()) {
            // No folding — just syntax highlight, identity mapping
            val highlighted = applySyntaxHighlighting(raw, colors, foldSymbols)
            return TransformedText(highlighted, OffsetMapping.Identity)
        }

        // Sort folded ranges so we process left-to-right
        val sorted = foldedRanges.sortedBy { it.first }

        // Build the display string and the offset translation tables
        val displayBuilder = StringBuilder()
        // originalToDisplay[i] = display offset for raw offset i
        val originalToDisplay = IntArray(raw.length + 1)
        // displayToOriginal[j] = raw offset for display offset j
        val displayToOriginalList = mutableListOf<Int>()

        var rawPos = 0
        var displayPos = 0

        for (foldedRange in sorted) {
            // Copy the part before this folded range
            while (rawPos < foldedRange.first) {
                originalToDisplay[rawPos] = displayPos
                displayToOriginalList.add(rawPos)
                displayBuilder.append(raw[rawPos])
                rawPos++
                displayPos++
            }

            // Emit the placeholder
            val placeholder = "${foldSymbols.openSymbol} ⋯ ${foldSymbols.closeSymbol}"
            val placeholderLen = placeholder.length
            displayBuilder.append(placeholder)

            // Map all raw positions inside the folded range to the start of placeholder
            val foldEnd = foldedRange.last + 1  // exclusive
            while (rawPos < foldEnd && rawPos < raw.length) {
                originalToDisplay[rawPos] = displayPos
                rawPos++
            }
            // The raw position just after the fold maps to just after the placeholder
            if (rawPos <= raw.length) {
                originalToDisplay[rawPos.coerceAtMost(raw.length)] = displayPos + placeholderLen
            }

            // Display positions inside the placeholder all map back to foldedRange.first
            repeat(placeholderLen) {
                displayToOriginalList.add(foldedRange.first)
            }
            displayPos += placeholderLen
        }

        // Copy anything after the last folded range
        while (rawPos < raw.length) {
            originalToDisplay[rawPos] = displayPos
            displayToOriginalList.add(rawPos)
            displayBuilder.append(raw[rawPos])
            rawPos++
            displayPos++
        }
        // Sentinel: end-of-string
        originalToDisplay[raw.length] = displayPos
        displayToOriginalList.add(raw.length)

        val displayText = displayBuilder.toString()

        // Apply syntax highlighting to the display string
        val highlighted = applySyntaxHighlighting(displayText, colors, foldSymbols)

        // Build the OffsetMapping
        val displayToOriginal = displayToOriginalList.toIntArray()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return originalToDisplay[clamped]
            }
            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, displayToOriginal.size - 1)
                return displayToOriginal[clamped]
            }
        }

        return TransformedText(highlighted, offsetMapping)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Syntax highlighting (operates on already-display text, no length changes)
// ─────────────────────────────────────────────────────────────────────────────

private fun applySyntaxHighlighting(
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

        // Headings: lines starting with #
        if (Regex("^#{1,6}\\s").containsMatchIn(line)) {
            addStyle(
                SpanStyle(color = colors.heading, fontWeight = FontWeight.Bold),
                lineStart, lineEnd
            )
        } else {
            // Blockquote
            if (line.trimStart().startsWith(">")) {
                addStyle(
                    SpanStyle(color = colors.blockquote, fontStyle = FontStyle.Italic),
                    lineStart, lineEnd
                )
            }
            // List markers
            val listMatch = Regex("^(\\s*)([-*]|\\d+\\.)\\s").find(line)
            if (listMatch != null) {
                val indent = listMatch.groups[1]?.range?.last?.plus(1) ?: 0
                val markerEnd = lineStart + (listMatch.groups[0]?.range?.last?.plus(1) ?: 0)
                addStyle(
                    SpanStyle(color = colors.listMarker),
                    lineStart + indent,
                    markerEnd
                )
            }
        }
        offset = lineEnd + 1
    }

    // Code blocks ```...```
    val codeBlockRegex = Regex("```[\\s\\S]*?```", RegexOption.DOT_MATCHES_ALL)
    for (m in codeBlockRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.code, fontFamily = FontFamily.Monospace),
            m.range.first, m.range.last + 1
        )
    }

    // Inline code `...`
    val inlineCodeRegex = Regex("`[^`\n]+`")
    for (m in inlineCodeRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.code, fontFamily = FontFamily.Monospace),
            m.range.first, m.range.last + 1
        )
    }

    // Bold **...**
    val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
    for (m in boldRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.bold, fontWeight = FontWeight.Bold),
            m.range.first, m.range.last + 1
        )
    }

    // Italic *...* or _..._
    val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)(.+?)(?<!_)_(?!_)")
    for (m in italicRegex.findAll(text)) {
        addStyle(
            SpanStyle(color = colors.italic, fontStyle = FontStyle.Italic),
            m.range.first, m.range.last + 1
        )
    }

    // Links [text](url)
    val linkRegex = Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)")
    for (m in linkRegex.findAll(text)) {
        addStyle(SpanStyle(color = colors.link), m.range.first, m.range.last + 1)
    }

    // Fold open symbol
    if (foldSymbols.openSymbol.isNotEmpty()) {
        val r = Regex(Regex.escape(foldSymbols.openSymbol))
        for (m in r.findAll(text)) {
            addStyle(
                SpanStyle(color = colors.foldOpen, fontWeight = FontWeight.Bold),
                m.range.first, m.range.last + 1
            )
        }
    }

    // Fold close symbol
    if (foldSymbols.closeSymbol.isNotEmpty()) {
        val r = Regex(Regex.escape(foldSymbols.closeSymbol))
        for (m in r.findAll(text)) {
            addStyle(
                SpanStyle(color = colors.foldClose, fontWeight = FontWeight.Bold),
                m.range.first, m.range.last + 1
            )
        }
    }
}
