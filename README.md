# Reader — Android Markdown Reader/Writer

A clean Android app for reading and writing Markdown (`.md`) files, with full support for **local files** (via SAF) and **network files** (HTTP/HTTPS).

## Features

- 📂 **Open local `.md` files** using Android Storage Access Framework (SAF)
- 🌐 **Add network URLs** to remote Markdown files (raw GitHub, HTTP servers, etc.)
- ✏️ **Edit & save** local files with a full-screen monospace editor
- 👁️ **Rendered Markdown** preview with tables, strikethrough, images, links, and syntax highlighting
- 🔄 **Reload** network files on demand
- 🌙 **Light & dark mode** following system preference
- 💾 **Persistent file list** across app restarts

## Architecture

```
app/
├── data/
│   └── FileRepository.kt     — SAF I/O, OkHttp network fetch, DataStore persistence
├── model/
│   └── MdFile.kt             — Data model for local/remote files
├── ui/
│   ├── ReaderViewModel.kt    — State management (AndroidViewModel)
│   ├── ReaderApp.kt          — Jetpack Compose NavHost
│   ├── screens/
│   │   ├── HomeScreen.kt     — File list (local + network sections)
│   │   └── ViewerScreen.kt   — Markdown viewer + editor
│   └── theme/
│       └── ReaderTheme.kt    — Material 3 theme (teal/warm)
```

## Tech Stack

| Library | Purpose |
|---|---|
| Jetpack Compose | UI |
| Navigation Compose | Screen routing |
| OkHttp | HTTP file fetching |
| Markwon | Markdown rendering (tables, images, HTML) |
| DataStore Preferences | Persistent file list |
| DocumentFile (SAF) | Local file I/O |

## Usage

### Local files
1. Tap the folder icon → pick a `.md` file
2. Tap a file to view rendered Markdown
3. Tap ✏️ to edit, 💾 to save

### Network files
1. Tap the link icon → paste a URL (e.g. raw GitHub URL)
2. Tap to load and render
3. Tap 🔄 to re-fetch

## Requirements

- Android 8.0+ (API 26)
- Internet permission for network files

## Build

```bash
./gradlew assembleDebug
```
