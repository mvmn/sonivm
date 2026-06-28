# Spec: Music Library Scanning

## ADDED Requirements

### Requirement: Scan configured directories recursively
The system SHALL walk each configured library folder recursively to discover audio files. Only files whose extensions match the user's supported file extensions list SHALL be included.

#### Scenario: Scanning a directory with mixed files
- **WHEN** the user configures a folder containing both audio files (.mp3, .flac) and non-audio files (.txt, .jpg)
- **THEN** only the audio files are discovered and added as library entries

#### Scenario: Scanning nested subdirectories
- **WHEN** a configured folder contains audio files in nested subdirectories
- **THEN** audio files at all depth levels are discovered

#### Scenario: Scanning an empty directory
- **WHEN** a configured folder contains no audio files
- **THEN** no entries are added from that folder and no error is reported

### Requirement: Read audio metadata concurrently
The system SHALL read tag metadata (artist, album, title, track number, date, genre, duration) for each discovered file using `TagRetrievalService`. Tag reading tasks SHALL execute concurrently on a dedicated thread pool.

#### Scenario: Reading tags for multiple files
- **WHEN** scanning discovers 10 audio files
- **THEN** tag reading tasks are submitted concurrently and all 10 entries receive their `TrackMetadata`

#### Scenario: Tag reading failure for a file
- **WHEN** tag reading fails for one file (corrupt tags, unsupported format)
- **THEN** the entry is still added with `null` metadata, and the remaining files' tags are read normally

### Requirement: Deduplicate entries by file path
The system SHALL not create duplicate entries for the same file path. If a file appears in multiple configured folders (e.g., overlapping directories), only one entry SHALL exist.

#### Scenario: Overlapping configured folders
- **WHEN** the user configures both `/music` and `/music/rock` as library folders
- **THEN** files in `/music/rock` appear only once in the library

### Requirement: Notify listeners after scan completes
The system SHALL fire `MusicLibraryChangeListener.onMusicLibraryUpdate()` after a scan finishes and all tag reading is complete, allowing the UI to refresh.

#### Scenario: UI refresh after scan
- **WHEN** scanning completes and all tags are read
- **THEN** registered change listeners are notified and the table model refreshes

### Requirement: Manual re-scan on demand
The system SHALL provide a mechanism for the user to trigger a full re-scan of all configured folders at any time.

#### Scenario: User triggers re-scan
- **WHEN** the user invokes the re-scan action from the UI
- **THEN** all configured folders are rescanned, entries are replaced with fresh results, and listeners are notified
