# Design: Music Library Integration

## Context

The music library package (`x.mvmn.sonivm.musiclibrary`) has scaffolding: `MusicLibraryService` interface, `MusicLibraryEntry` data model, `MusicLibraryPersistenceService` with JSON serialization, `MusicLibraryChangeListener`, and a stub `MusicLibraryServiceImpl`. The UI has `MusicLibraryTab` (stub — just a JTable) and `MusicLibraryTableModel` (filter state, search predicate, unique-values helpers — but no UI controls wired). The table model is Spring-injected and added to the main window's tabbed pane. However, `scanForMusicFiles()` is empty, `reSync()` uses a hardcoded path, no persistence is called, no preferences for library folders exist, and the controller/UI are not wired together.

The play queue already has a proven pattern for: concurrent tag reading (`PlaybackQueueFileImportServiceImpl` with `ExecutorService` + `TagRetrievalService`), column persistence (`SwingUtil.getJTableColumnPositions/Widths` + `PreferencesService` + boolean change flags), and drag-and-drop file import (`PlayQueueTableDnDTransferHandler`).

## Goals / Non-Goals

**Goals:**
- Users can configure music library folders (add/remove directories) that are scanned for audio files
- Scanning walks directories recursively, reads tags concurrently using existing `TagRetrievalService`, and populates the library
- Library state (entries + configured folders) persists across sessions via JSON + preferences
- Library tab provides search text field, filter combo boxes (artist/album/year), double-click-to-play, and drag-to-queue
- Library table column widths and positions persist across sessions (same pattern as play queue)
- `MusicLibraryTableModel` auto-updates when the library changes via `MusicLibraryChangeListener`

**Non-Goals:**
- Folder management dialog (initially use a simple file chooser from menu; fancy folder list UI is future work)
- Library sync conflict resolution or incremental scan (full rescan on start, manual re-scan on demand)
- Retro UI music library support
- LastFM integration with library entries
- Deleting entries from library (files removed from disk will be filtered out on re-scan)

## Decisions

### 1. Directory scanning: `Files.walkFileTree` with extension filter
Use `java.nio.file.Files.walkFileTree` starting from each configured folder. The `SimpleFileVisitor` checks file extensions against `preferencesService.getSupportedFileExtensions()` to determine if a file is audio. This avoids scanning non-audio files and is consistent with how the app already determines supported formats.

*Alternative considered:* Using `PlaybackQueueFileImportService.addFileToQueue()` directly — but that method has queue-specific logic (CUE sheet handling, position tracking) not applicable to library scanning. Better to write a dedicated walker.

### 2. Concurrent tag reading: dedicated `ExecutorService` in `MusicLibraryServiceImpl`
Mirror `PlaybackQueueFileImportServiceImpl`'s pattern: submit tag-reading tasks to a fixed-thread-pool `ExecutorService`, track active tasks with `AtomicInteger`, and only persist/save after all tasks complete. Reuse `TagRetrievalService.getAudioFileMetadata()` — no need to duplicate tag-reading logic.

*Alternative considered:* Sharing the queue's executor — rejected because library scanning may run independently (e.g., on app start) while queue operations are active. Separate pools avoid contention.

### 3. `reSync()` loads from persistence first, then rescans
On app start, `reSync()` loads saved entries from `music_library.json` for immediate display, then triggers background scan of configured folders. The scan result replaces the loaded entries. This gives fast startup (show cached library immediately) with eventual consistency (fresh scan updates).

*Alternative considered:* Only load from persistence and require manual re-scan — rejected because users expect their library to be current on startup.

### 4. Folder storage: `List<String>` in preferences
Store configured library folder paths as a list of strings in `PreferencesService` using the existing `getStringListProperty`/`setStringListProperty` helpers (same mechanism as `supportedFileExtensions`). Key: `musiclibraryfolders`.

*Alternative considered:* Storing folders in the JSON library file — rejected because preferences are the established location for user-configurable settings, and folders are configuration, not data.

### 5. Column persistence: replicate play queue pattern exactly
Add `KEY_MUSICLIBRARY_COLUMN_WIDTHS` / `KEY_MUSICLIBRARY_COLUMN_POSITIONS` to `PreferencesServiceImpl`. Add `columnsMoved`/`columnsResized` boolean flags and `TableColumnModelListener` to `MusicLibraryTab` (same as `SonivmMainWindow`). Add `saveMusicLibraryColumnsState()`/`restoreMusicLibraryColumnsState()` to `SonivmUI` called from the same `componentHidden`/`componentShown` listeners.

*Alternative considered:* Using a separate persistence mechanism — rejected to maintain consistency with existing column persistence code.

### 6. Filter UI in `MusicLibraryTab`: search field + combo boxes
The tab panel layout: top bar with `JTextField` (search) + three `JComboBox` (artist/album/year filters) + a clear-filters button. Combo boxes are populated from `MusicLibraryTableModel.getUniqueArtists()/Albums/Years()`. Search uses the existing `searchPredicate()`; filters use `getFilterPredicate()`. Table rows are filtered client-side — the table model's `getRowCount()` and `getValueAt()` apply the filter predicate.

*Alternative considered:* Virtualizing the table (only show matching rows) — rejected because `MusicLibraryTableModel` already has filter state and the play queue uses a similar in-memory filter approach. Keeping it simple avoids index-mapping complexity.

### 7. Controller wiring: extend `SonivmUIController` with library methods
Add `onLibraryTrackDoubleClick(int index)`, `onLibrarySearch(String text)`, `onLibraryFilterChange()`, `onLibraryDropToQueue(int row, List<File>)`, `onLibraryRescan()` to the interface. Implement in `PlaybackControllerImpl` (which already implements `SonivmUIController`). Inject `MusicLibraryService` and `MusicLibraryPersistenceService` into the controller.

*Alternative considered:* A separate `MusicLibraryController` — rejected because `PlaybackControllerImpl` is already the central orchestrator and `SonivmUI` already passes `this` (implementing `SonivmUIController`) to all UI components.

### 8. Double-click action: play track directly (add to queue and play)
Double-clicking a library entry adds it to the current play queue and starts playback, matching the existing "drop files to queue" behavior. This is consistent with how the app already handles file-to-play transitions.

### 9. `MusicLibraryTableModel` implements `MusicLibraryChangeListener`
The table model registers itself as a change listener on `MusicLibraryService`. On `onMusicLibraryUpdate()`, it calls `fireTableDataChanged()` to refresh the view. This is the existing Swing pattern used elsewhere.

## Risks / Trade-offs

- **Large library scan blocks UI**: Tag reading runs on a background thread pool, but `fireTableDataChanged()` fires on arbitrary threads. **Mitigation**: Wrap `fireTableDataChanged()` in `SwingUtilities.invokeLater()`.
- **Full re-scan on every startup may be slow**: For large libraries (10k+ files), scanning all folders takes time. **Mitigation**: Show cached library from persistence immediately; background scan updates entries incrementally. Could add a progress indicator in future.
- **Stale entries (deleted files)**: Files removed from disk remain in the library until re-scan. **Mitigation**: Re-scan on startup and manual re-scan button. Future: mark missing entries with a visual indicator.
- **`CopyOnWriteArrayList` for entries**: Current implementation uses `CopyOnWriteArrayList` which is fine for read-heavy workloads but has O(N) write cost during scan. **Mitigation**: Acceptable for library sizes up to ~50k entries. If needed, switch to `Collections.synchronizedList` later.

## Open Questions

- Should the library tab show a status label ("Scanning...", "5,234 tracks", "Total: 3h 42m")?
- Should filter combo boxes support "all" as the default selection, or empty string?
- Should the search field use the same instant-search pattern as the play queue (debounced text field)?
