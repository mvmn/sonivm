# Proposal: Music Library Integration

## Why

The music library feature is partially implemented but unusable — users cannot add music to the library, search/filter UI is missing, and column layout is not persisted. The existing scaffolding (service, entry model, persistence, table model) needs to be wired together and completed to make the feature functional.

## What Changes

- Add `musicLibraryFolders` preference to `PreferencesService`/`PreferencesServiceImpl` so users can configure which directories to scan
- Implement `MusicLibraryServiceImpl.scanForMusicFiles()` — directory walking, concurrent tag reading (reusing `TagRetrievalService`), and entry creation
- Implement `MusicLibraryServiceImpl.reSync()` — load saved library from persistence, save after changes
- Wire `MusicLibraryService` and `MusicLibraryPersistenceService` into `PlaybackControllerImpl` via `@Autowired`
- Add music library methods to `SonivmUIController` (double-click play, drop-to-queue, search, folder management)
- Wire music library controller methods into `SonivmUI`
- Complete `MusicLibraryTab` with search field, filter combo boxes (artist/album/year), and double-click-to-play
- Add drag-and-drop from library table to play queue
- Persist library table column widths and positions (same pattern as play queue)
- Register `MusicLibraryTableModel` as a `MusicLibraryChangeListener` so the UI updates on library changes
- Fix `getLibraryLength()` to compute actual total duration

## Capabilities

### New Capabilities
- `music-library-scanning`: Scanning configured directories for audio files, reading tags concurrently, building the library index
- `music-library-ui`: Complete library tab with search, filter controls, column persistence, double-click-to-play, and drag-to-queue
- `music-library-persistence`: Save/load library state and configured folder paths across sessions

### Modified Capabilities
_(none — no existing specs to modify)_

## Impact

- `x/mvmn/sonivm/prefs/PreferencesService.java` — add `musicLibraryFolders` getter/setter
- `x/mvmn/sonivm/prefs/impl/PreferencesServiceImpl.java` — implement folder persistence
- `x/mvmn/sonivm/musiclibrary/impl/MusicLibraryServiceImpl.java` — implement scan, reSync, length calculation
- `x/mvmn/sonivm/musiclibrary/MusicLibraryPersistenceService.java` — already exists, needs wiring
- `x/mvmn/sonivm/impl/MusicLibraryPersistenceServiceImpl.java` — already exists, needs wiring
- `x/mvmn/sonivm/impl/PlaybackControllerImpl.java` — inject library services, implement controller methods
- `x/mvmn/sonivm/ui/SonivmUIController.java` — add library action methods
- `x/mvmn/sonivm/ui/SonivmUI.java` — wire library controller, save/restore column state
- `x/mvmn/sonivm/ui/MusicLibraryTab.java` — complete UI with search, filters, DnD, column listener
- `x/mvmn/sonivm/ui/model/MusicLibraryTableModel.java` — wire change listener
- `x/mvmn/sonivm/config/SonivmConfig.java` — bean wiring updates
- Data file `~/.sonivm/muslibrary.json` — populated at runtime after scan
