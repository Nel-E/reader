package com.nele.reader.model

import androidx.compose.ui.graphics.Color

data class SyntaxColors(
    val heading: Color = Color(0xFF1565C0),      // # ## ###
    val bold: Color = Color(0xFFAD1457),          // **text**
    val italic: Color = Color(0xFF6A1B9A),        // *text*
    val code: Color = Color(0xFF2E7D32),          // `code` and ```blocks```
    val link: Color = Color(0xFF00838F),          // [text](url)
    val blockquote: Color = Color(0xFF4E342E),    // > quote
    val listMarker: Color = Color(0xFF37474F),    // - * 1.
    val foldOpen: Color = Color(0xFFE65100),      // custom fold open symbol
    val foldClose: Color = Color(0xFFE65100)      // custom fold close symbol
)
