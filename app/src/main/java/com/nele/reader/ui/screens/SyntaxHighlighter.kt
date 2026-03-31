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
// Fold block detection  (nested-aware, stack-based)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Finds ALL foldable block ranges in [text], including nested blocks and
 * multiple sibling blocks at any depth.
 *
 * Uses a stack so that nested `{{ … {{ … }} … }}` blocks are all found.
 * Each returned IntRange spans from the first char of the open symbol to
 * the last char of its matching close symbol (i.e. range.last is inclusive,
 * range.last + 1 is the exclusive end).
 *
 * Example with open="{{" close="}}" and text:
 *   "{{ A {{ B }} C }} {{ D }}"
 * Returns (in discovery order, outer before inner):
 *   [0..16], [5..10], [18..24]
 *
 * The list is sorted by start position so callers can iterate left-to-right.
 */
fun findFoldableBlocks(text: String, symbols: FoldSymbols): List<IntRange> {
    val open  = symbols.openSymbol
    val close = symbols.closeSymbol
    if (open.isEmpty() || close.isEmpty() || open == close) return emptyList()

    val blocks = mutableListOf<IntRange>()
    // Stack holds the start index of each unmatched open symbol
    val stack  = ArrayDeque<Int>()
    var i = 0

    while (i < text.length) {
        when {
            text.startsWith(open,  i) -> {
                stack.addLast(i)
                i += open.length
            }
            text.startsWith(close, i) -> {
                if (stack.isNotEmpty()) {
                    val start = stack.removeLast()
                    blocks.add(start until i + close.length)
                }
                i += close.length
            }
            else -> i++
        }
    }

    return blocks.sortedBy { it.first }
}

// ─────────────────────────────────────────────────────────────────────────────
// Combined VisualTransformation: folding + syntax highlighting
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Handles both folding and syntax colouring in one VisualTransformation pass.
 *
 * Folding works by replacing the raw characters of each folded range with a
 * short placeholder string.  Because the display length differs from the raw
 * length we build precise originalToDisplay / displayToOriginal tables so that
 * cursor positioning and text selection remain correct.
 *
 * Nested folding is supported: if an outer block is folded its placeholder
 * hides all inner blocks too; inner blocks can be folded independently while
 * the outer block is expanded.
 *
 * Overlapping ranges (e.g. outer already covers an inner) are handled by
 * skipping any range whose start falls inside a range that has already been
 * emitted as a placeholder.
 */
class CombinedEditorTransformation(
    private val colors: SyntaxColors,
    private val foldSymbols: FoldSymbols,
    private val foldedRanges: Set<IntRange>
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text

        if (foldedRanges.isEmpty()) {
            return TransformedText(
                applySyntaxHighlighting(raw, colors, foldSymbols),
                OffsetMapping.Identity
            )
        }

        // Sort folded ranges by start position; skip any range that is
        // entirely contained within a range we have already processed
        // (avoids double-replacing when both an outer and inner block are
        // folded at the same time — the outer wins).
        val sorted = foldedRanges.sortedBy { it.first }
        val effective = mutableListOf<IntRange>()
        var coveredUntil = -1
        for (r in sorted) {
            if (r.first >= coveredUntil) {
                effective.add(r)
                coveredUntil = r.last + 1
            }
        }

        // Build display string + offset tables
        val displayBuilder = StringBuilder()
        val originalToDisplay = IntArray(raw.length + 1)
        val displayToOriginalList = mutableListOf<Int>()

        var rawPos    = 0
        var displayPos = 0

        for (foldedRange in effective) {
            // ── Characters before this fold ──────────────────────────────────
            while (rawPos < foldedRange.first) {
                originalToDisplay[rawPos] = displayPos
                displayToOriginalList.add(rawPos)
                displayBuilder.append(raw[rawPos])
                rawPos++
                displayPos++
            }

            // ── Placeholder ──────────────────────────────────────────────────
            val placeholder    = "${foldSymbols.openSymbol} ⋯ ${foldSymbols.closeSymbol}"
            val placeholderLen = placeholder.length
            displayBuilder.append(placeholder)

            // All raw positions inside the fold map to the placeholder start
            val foldEnd = foldedRange.last + 1  // exclusive
            while (rawPos < foldEnd) {
                if (rawPos <= raw.length) originalToDisplay[rawPos] = displayPos
                rawPos++
            }

            // Display positions inside the placeholder all map back to
            // the first raw position of the fold
            repeat(placeholderLen) {
                displayToOriginalList.add(foldedRange.first)
            }
            displayPos += placeholderLen
        }

        // ── Characters after last fold ────────────────────────────────────────
        while (rawPos < raw.length) {
            originalToDisplay[rawPos] = displayPos
            displayToOriginalList.add(rawPos)
            displayBuilder.append(raw[rawPos])
            rawPos++
            displayPos++
        }
        // Sentinel for end-of-string
        originalToDisplay[raw.length] = displayPos
        displayToOriginalList.add(raw.length)

        val displayText        = displayBuilder.toString()
        val highlighted        = applySyntaxHighlighting(displayText, colors, foldSymbols)
        val displayToOriginal  = displayToOriginalList.toIntArray()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                originalToDisplay[offset.coerceIn(0, raw.length)]

            override fun transformedToOriginal(offset: Int): Int =
                displayToOriginal[offset.coerceIn(0, displayToOriginal.size - 1)]
        }

        return TransformedText(highlighted, offsetMapping)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Syntax highlighting
// ─────────────────────────────────────────────────────────────────────────────

private fun applySyntaxHighlighting(
    text: String,
    colors: SyntaxColors,
    foldSymbols: FoldSymbols
): AnnotatedString = buildAnnotatedString {
    append(text)

    // ── Line-level patterns ───────────────────────────────────────────────────
    val lines = text.lines()
    var offset = 0
    for (line in lines) {
        val lineStart = offset
        val lineEnd   = offset + line.length

        if (Regex("^#{1,6}\\s").containsMatchIn(line)) {
            addStyle(
                SpanStyle(color = colors.heading, fontWeight = FontWeight.Bold),
                lineStart, lineEnd
            )
        } else {
            if (line.trimStart().startsWith(">")) {
                addStyle(
                    SpanStyle(color = colors.blockquote, fontStyle = FontStyle.Italic),
                    lineStart, lineEnd
                )
            }
            val listMatch = Regex("^(\\s*)([-*]|\\d+\\.)\\s").find(line)
            if (listMatch != null) {
                val indent    = listMatch.groups[1]?.range?.last?.plus(1) ?: 0
                val markerEnd = lineStart + (listMatch.groups[0]?.range?.last?.plus(1) ?: 0)
                addStyle(SpanStyle(color = colors.listMarker), lineStart + indent, markerEnd)
            }
        }
        offset = lineEnd + 1
    }

    // ── Inline / span patterns ────────────────────────────────────────────────

    // Code blocks ```...```  (before inline code so ``` wins over `)
    for (m in Regex("```[\\s\\S]*?```", RegexOption.DOT_MATCHES_ALL).findAll(text))
        addStyle(SpanStyle(color = colors.code, fontFamily = FontFamily.Monospace),
            m.range.first, m.range.last + 1)

    // Inline code `...`
    for (m in Regex("`[^`\n]+`").findAll(text))
        addStyle(SpanStyle(color = colors.code, fontFamily = FontFamily.Monospace),
            m.range.first, m.range.last + 1)

    // Bold **...**
    for (m in Regex("\\*\\*(.+?)\\*\\*").findAll(text))
        addStyle(SpanStyle(color = colors.bold, fontWeight = FontWeight.Bold),
            m.range.first, m.range.last + 1)

    // Italic *...* or _..._
    for (m in Regex("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)|(?<!_)_(?!_)(.+?)(?<!_)_(?!_)").findAll(text))
        addStyle(SpanStyle(color = colors.italic, fontStyle = FontStyle.Italic),
            m.range.first, m.range.last + 1)

    // Links [text](url)
    for (m in Regex("\\[([^\\]]+)\\]\\(([^)]+)\\)").findAll(text))
        addStyle(SpanStyle(color = colors.link), m.range.first, m.range.last + 1)

    // Fold symbols
    if (foldSymbols.openSymbol.isNotEmpty())
        for (m in Regex(Regex.escape(foldSymbols.openSymbol)).findAll(text))
            addStyle(SpanStyle(color = colors.foldOpen, fontWeight = FontWeight.Bold),
                m.range.first, m.range.last + 1)

    if (foldSymbols.closeSymbol.isNotEmpty())
        for (m in Regex(Regex.escape(foldSymbols.closeSymbol)).findAll(text))
            addStyle(SpanStyle(color = colors.foldClose, fontWeight = FontWeight.Bold),
                m.range.first, m.range.last + 1)
}
