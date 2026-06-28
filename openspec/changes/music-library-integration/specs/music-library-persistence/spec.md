# Spec: Music Library Persistence

## ADDED Requirements

### Requirement: Persist library entries to JSON
The system SHALL save the complete list of `MusicLibraryEntry` objects to `music_library.json` in the Sonivm data directory (`~/.sonivm/`) after each scan completes. Serialization SHALL use Jackson `ObjectMapper` and preserve all entry fields including `TrackMetadata`.

#### Scenario: Saving after scan
- **WHEN** a library scan completes and all tags are read
- **THEN** the full entry list is written to `music_library.json`

#### Scenario: Save failure is logged, not fatal
- **WHEN** writing to `music_library.json` fails (disk full, permission denied)
- **THEN** the error is logged at WARNING level and the in-memory library remains intact

### Requirement: Load library from JSON on startup
The system SHALL load `MusicLibraryEntry` objects from `music_library.json` on application startup. Loaded entries SHALL be available immediately for display before any background scan runs.

#### Scenario: Loading existing library
- **WHEN** the application starts and `music_library.json` exists with valid data
- **THEN** entries are loaded and displayed in the library table

#### Scenario: Loading when no file exists
- **WHEN** the application starts and `music_library.json` does not exist
- **THEN** the library is empty and no error is reported

#### Scenario: Loading corrupted file
- **WHEN** `music_library.json` contains invalid JSON
- **THEN** the error is logged and the library starts empty

### Requirement: Persist configured library folders
The system SHALL store the list of configured library folder paths in `PreferencesService` using the key `musiclibraryfolders`. The folders SHALL be persisted as a list of strings using the existing string-list preference mechanism.

#### Scenario: Adding a folder
- **WHEN** the user adds a directory to the library folders list
- **THEN** the updated list is saved to preferences

#### Scenario: Removing a folder
- **WHEN** the user removes a directory from the library folders list
- **THEN** the updated list is saved to preferences

#### Scenario: Folders restored on startup
- **WHEN** the application starts
- **THEN** the previously configured folder paths are loaded from preferences and used for scanning

### Requirement: reSync loads cached data then rescans
The `MusicLibraryService.reSync()` method SHALL first load entries from persistence for immediate display, then trigger a background scan of configured folders. The scan result SHALL replace the loaded entries, and the updated library SHALL be saved to persistence.

#### Scenario: Normal reSync on startup
- **WHEN** `reSync()` is called on application startup with an existing `music_library.json`
- **THEN** cached entries are loaded for display, background scan runs, and fresh results replace the cache

#### Scenario: reSync with no cached data
- **WHEN** `reSync()` is called and `music_library.json` does not exist
- **THEN** the library starts empty and the background scan populates it from scratch
