# LaTeX Editor for Android

A native Android LaTeX editor with syntax highlighting, autocomplete, file/project
management, and cloud-compiled PDF preview.

## Setup

1. Open this folder in **Android Studio** (Hedgehog/2023.1+ recommended).
2. Let it sync Gradle. If it complains about a missing `gradlew`, use
   **File -> Sync Project with Gradle Files**, or run `gradle wrapper` once
   with a local Gradle install - this project's wrapper properties file
   (`gradle/wrapper/gradle-wrapper.properties`) is already configured for
   Gradle 8.4, AGP 8.2.2, Kotlin 1.9.22.
3. Run on a device/emulator (minSdk 24 / Android 7.0+).
4. Replace the placeholder launcher icon via **Right-click `res` -> New -> Image Asset**.

## Configuring the compile server

Compilation happens in the cloud, not on-device (a real TeX install is
multiple gigabytes and needs native binaries - not practical to bundle in
an app). Go to **Settings** in the app and set the compile endpoint URL.

The app POSTs a multipart request (project ZIP + main filename + engine
name) and expects either:
- `200 OK` with `Content-Type: application/pdf` and PDF bytes, or
- an error status with a plain-text/JSON compiler log in the body.

This matches many "LaTeX-as-a-service" style HTTP APIs, but **the exact
request shape has not been verified against a live server** - this project
was built without live network access to test it. You may need to adjust
`CompileApi.kt` / `CompileRepository.kt` field names to match whatever
endpoint you use. The most reliable path is running your own small backend
(e.g. a container with `texlive` + `latexmk` behind a tiny HTTP wrapper) so
you control the contract, uptime, and privacy of your documents.

## What's implemented

- Code editor: line numbers, syntax highlighting (3 themes), auto-indent,
  auto-closing brackets, undo/redo, search & replace, word wrap toggle
- Autocomplete for commands, math symbols, environments, packages
- Multi-file project management: create/rename/delete files & folders,
  autosave, ZIP export/import, "set as main file"
- 8 starter templates (article, report, resume, research paper, letter,
  Beamer, assignment, book)
- Cloud compile (pdflatex/xelatex/lualatex selectable per project) with
  PDF preview via Android's native `PdfRenderer` (pinch-to-zoom, continuous
  scroll across pages)
- Error detection: instant local `\begin`/`\end` balance checking, plus
  parsing of the compiler log into a tappable list that jumps to the
  offending line
- Table generator (rows/cols/alignment -> LaTeX), symbol palette (Greek
  letters, operators, fraction/matrix/integral/limit builders)
- BibTeX-aware: `.bib` files get file-type icons and the same syntax
  highlighting rules apply as `.tex`

## Known gaps (not included)

- **Real-time collaboration** (Overleaf-style multi-cursor/live edits/chat) -
  needs a WebSocket backend with operational transforms; out of scope for a
  client app.
- **Git integration** - needs a git backend/credentials flow; not wired up.
- **AI assistant / grammar & spell check** - needs an LLM or NLP service;
  not wired up.
- **DOCX/HTML/Markdown export via Pandoc** - needs Pandoc running
  somewhere (device or server); not implemented.
- **True code folding & simultaneous multi-cursor editing** - Android's
  standard text-editing model doesn't support these without a fully custom
  text renderer (like what desktop IDEs use). Not attempted here.
- This project has **not been compiled/run** in the environment it was
  built in (no Android SDK / Google Maven access in that sandbox), so
  treat it as a strong, real starting point rather than a guaranteed
  zero-error build - expect to fix a handful of small issues (an import
  here, a resource id there) on first sync in Android Studio.

## Architecture

```
app/src/main/java/com/latexeditor/app/
  MainActivity.kt              Project list (home screen)
  data/                        Project/ProjectFile models, on-disk repository, prefs
  editor/                      CodeEditorView, syntax highlighter, autocomplete,
                                undo/redo, search/replace, templates, EditorActivity
  compile/                     Retrofit API, compile repository, log/error parsing
  pdf/                         PdfRenderer-based page adapter + zoomable image view
  files/                       RecyclerView adapters for project list & file explorer
  ui/                          Settings, template picker, table generator, symbol palette
```

Projects are stored under the app's private storage
(`filesDir/projects/<uuid>/...`) as plain files, so everything except the
compile step works fully offline.
