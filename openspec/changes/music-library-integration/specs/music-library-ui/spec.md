# Spec: Music Library UI

## ADDED Requirements

### Requirement: Search field for library entries
The music library tab SHALL provide a text input field that filters table rows in real time. As the user types, only entries matching the search text across title, artist, album, date, and genre fields SHALL be visible.

#### Scenario: Searching by artist name
- **WHEN** the user types "Pink Floyd" in the search field
- **THEN** only entries whose artist contains "Pink Floyd" are displayed

#### Scenario: Searching with multiple terms
- **WHEN** the user types "dark side" in the search field
- **THEN** entries matching both "dark" AND "side" across any searchable field are displayed

#### Scenario: Clearing search
- **WHEN** the user clears the search field
- **THEN** all entries (subject to active filters) are displayed again

### Requirement: Filter combo boxes for artist, album, and year
The music library tab SHALL provide three combo box controls for filtering by artist, album, and year. Each combo box SHALL be populated with the unique values extracted from the library entries. An empty selection SHALL mean "no filter" for that field.

#### Scenario: Filtering by artist
- **WHEN** the user selects "Metallica" from the artist combo box
- **THEN** only entries by "Metallica" are displayed

#### Scenario: Combining multiple filters
- **WHEN** the user selects an artist, an album, and a year
- **THEN** only entries matching all three filter values are displayed

#### Scenario: Clearing all filters
- **WHEN** the user clicks the clear-filters button
- **THEN** all combo boxes reset to empty and all entries are displayed

### Requirement: Double-click to play
The system SHALL play a library entry when the user double-clicks its row in the table. The entry SHALL be added to the current play queue and playback SHALL start.

#### Scenario: Double-click on a track
- **WHEN** the user double-clicks a row in the library table
- **THEN** the track is added to the play queue and playback begins

#### Scenario: Double-click with no valid entry
- **WHEN** the user double-clicks on an empty area of the table
- **THEN** no action occurs

### Requirement: Drag-and-drop from library to play queue
The system SHALL support dragging rows from the library table and dropping them onto the play queue table. Dropped entries SHALL be inserted at the drop position in the queue.

#### Scenario: Dragging a single track to queue
- **WHEN** the user drags one row from the library table and drops it on the play queue
- **THEN** the track is added to the play queue at the drop position

#### Scenario: Dragging multiple tracks to queue
- **WHEN** the user selects multiple rows and drags them to the play queue
- **THEN** all selected tracks are added to the play queue in their library order

### Requirement: Persist column widths and positions
The system SHALL save the music library table's column widths and column positions when the main window is hidden, and restore them when the window is shown. The persistence mechanism SHALL use `PreferencesService` with the same pattern as the play queue table.

#### Scenario: Column resized and restored
- **WHEN** the user resizes a column in the library table, then closes and reopens the application
- **THEN** the column retains its custom width

#### Scenario: Column reordered and restored
- **WHEN** the user drags a column to a new position, then closes and reopens the application
- **THEN** the column retains its new position

### Requirement: Auto-refresh on library changes
The `MusicLibraryTableModel` SHALL register as a `MusicLibraryChangeListener` on `MusicLibraryService`. When the library is updated (scan complete, entries added/removed), the table SHALL refresh to reflect the new data.

#### Scenario: Table refresh after scan
- **WHEN** a background scan completes and new entries are added to the library
- **THEN** the table automatically displays the updated entries

### Requirement: Display library statistics
The `MusicLibraryTableModel.getLibraryLength()` method SHALL return the total duration of all entries in the library, computed by summing each entry's duration field.

#### Scenario: Computing total library length
- **WHEN** the library contains entries with known durations
- **THEN** `getLibraryLength()` returns the sum of all durations in seconds

#### Scenario: Entries with missing duration
- **WHEN** some entries have `null` duration
- **THEN** those entries contribute 0 to the total and are not counted in the sum
