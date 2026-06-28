# Tasks: Music Library Integration

## 1. Preferences — library folder storage

- [x] 1.1 Add `KEY_MUSICLIBRARY_FOLDERS` constant to `PreferencesServiceImpl`
- [x] 1.2 Add `getMusicLibraryFolders()` and `setMusicLibraryFolders(List<String>)` to `PreferencesService` interface
- [x] 1.3 Implement getters/setters in `PreferencesServiceImpl` using `getStringListProperty`/`setStringListProperty`

## 2. Persistence wiring

- [x] 2.1 Inject `MusicLibraryPersistenceService` into `MusicLibraryServiceImpl` via `@Autowired`
- [x] 2.2 Inject `MusicLibraryService` and `MusicLibraryPersistenceService` into `PlaybackControllerImpl` via `@Autowired`
- [x] 2.3 Update `SonivmConfig` if bean wiring needs adjustment for new dependencies

## 3. Scanning implementation

- [x] 3.1 Implement `scanForMusicFiles(File)` in `MusicLibraryServiceImpl` using `Files.walkFileTree` with extension filtering against `preferencesService.getSupportedFileExtensions()`
- [x] 3.2 Add `ExecutorService` tag-reading thread pool to `MusicLibraryServiceImpl` (fixed thread pool sized to `availableProcessors()`)
- [x] 3.3 Inject `TagRetrievalService` into `MusicLibraryServiceImpl`
- [x] 3.4 Implement concurrent tag reading: submit `TagRetrievalService.getAudioFileMetadata()` tasks per file, track active tasks with `AtomicInteger`
- [x] 3.5 Deduplicate entries by `targetFileFullPath` (use a `Set<String>` to track seen paths across folders)
- [x] 3.6 After all tag tasks complete, call `notifyListeners()` on the EDT via `SwingUtilities.invokeLater()`

## 4. reSync implementation

- [x] 4.1 Implement `reSync()` to load entries from `MusicLibraryPersistenceService.loadMusicLibrary()` into the in-memory list
- [x] 4.2 After loading, notify listeners to display cached entries
- [x] 4.3 Trigger background scan of all folders from `preferencesService.getMusicLibraryFolders()`
- [x] 4.4 After background scan completes, replace in-memory entries with scan results and save via `MusicLibraryPersistenceService.saveMusicLibrary()`
- [x] 4.5 Call `reSync()` from `PlaybackControllerImpl`'s `@PostConstruct` init or from `SonivmUI` startup

## 5. Controller and UI wiring

- [x] 5.1 Add `onLibraryTrackDoubleClick(int index)`, `onLibrarySearch(String text)`, `onLibraryFilterChange()`, `onLibraryRescan()`, `onLibraryAddFolder()`, `onLibraryRemoveFolder(int index)` to `SonivmUIController`
- [x] 5.2 Implement each controller method in `PlaybackControllerImpl` (delegate to `MusicLibraryService`, update table model, dispatch to EDT)
- [x] 5.3 Register `MusicLibraryTableModel` as `MusicLibraryChangeListener` in `SonivmUI` constructor or `init()`
- [x] 5.4 Implement `MusicLibraryTableModel.onMusicLibraryUpdate()` to call `fireTableDataChanged()` on EDT and refresh filter combo box data

## 6. UI — search and filter controls

- [ ] 6.1 Add `JTextField` search field to `MusicLibraryTab` top bar with `DocumentListener` calling `controller.onLibrarySearch(text)`
- [ ] 6.2 Add three `JComboBox` controls (artist, album, year) populated from `musicLibraryTableModel.getUniqueArtists()/Albums/Years()`
- [ ] 6.3 Wire combo box `ActionListener` to call `controller.onLibraryFilterChange()` and apply filters via `musicLibraryTableModel.setArtistFilter/AlbumFilter/YearFilter`
- [ ] 6.4 Add "Clear filters" button that calls `musicLibraryTableModel.clearFilters()` and refreshes the table
- [ ] 6.5 Apply filter predicate in `MusicLibraryTableModel.getValueAt()` and `getRowCount()` so only matching rows display

## 7. UI — double-click play and drag-to-queue

- [ ] 7.1 Wire double-click handler in `MusicLibraryTab` to call `controller.onLibraryTrackDoubleClick(selectedRow)`
- [ ] 7.2 Implement `onLibraryTrackDoubleClick` in `PlaybackControllerImpl` to get entry by index, add its file to play queue, and start playback
- [ ] 7.3 Add `TransferHandler` to the library table for drag-to-queue, exporting `MusicLibraryEntry` objects
- [ ] 7.4 Implement drop handling in controller to convert library entries to queue entries and insert at drop position

## 8. UI — column persistence

- [ ] 8.1 Add `KEY_MUSICLIBRARY_COLUMN_WIDTHS` and `KEY_MUSICLIBRARY_COLUMN_POSITIONS` constants to `PreferencesServiceImpl`
- [ ] 8.2 Add `getMusicLibraryColumnWidths()`/`setMusicLibraryColumnWidths()` and `getMusicLibraryColumnPositions()`/`setMusicLibraryColumnPositions()` to `PreferencesService` and implementation
- [ ] 8.3 Add `columnsMoved`/`columnsResized` boolean flags and `TableColumnModelListener` to `MusicLibraryTab`
- [ ] 8.4 Add `getMusicLibraryTableColumnWidths()`/`getPositions()` getters and `setPositions()` setters to `MusicLibraryTab` (or expose the table's `TableColumnModel`)
- [ ] 8.5 Add `saveMusicLibraryColumnsState()`/`restoreMusicLibraryColumnsState()` to `SonivmUI`, called from `componentHidden`/`componentShown` listeners

## 9. Table model fixes

- [ ] 9.1 Implement `getLibraryLength()` to sum all entries' durations (skip `null` values)
- [ ] 9.2 Add status label to `MusicLibraryTab` showing track count and total duration (optional, from open questions)

## 10. Menu and folder management

- [ ] 10.1 Add "Add Folder..." and "Rescan Library" menu items to `SonivmMenuBar` wired to controller methods
- [ ] 10.2 Implement `onLibraryAddFolder()` with `JFileChooser` (directory selection mode), append to folders list, trigger scan
- [ ] 10.3 Implement `onLibraryRemoveFolder(int)` to remove from list, save preferences, trigger re-scan
