package com.nele.reader.model

/** Represents a Markdown file, either local (uri) or remote (url). */
data class MdFile(
    val id: String,          // unique id (uri string or url)
    val displayName: String,  // filename shown in list
    val isRemote: Boolean,
    val uri: String? = null,  // android content uri (local)
    val url: String? = null,  // http(s) url (remote)
    val isReadOnly: Boolean = isRemote // remote files are read-only by default
)
