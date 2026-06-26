package x.mvmn.sonivm.ui.model;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.musiclibrary.MusicLibraryEntry;
import x.mvmn.sonivm.musiclibrary.MusicLibraryService;
import x.mvmn.sonivm.util.StringUtil;
import x.mvmn.sonivm.util.TimeDateUtil;

@Component
public class MusicLibraryTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -4393495956274405244L;

	private static final String[] columnNames = new String[] { "#", "N", "Title", "Artist", "Album", "Length", "Date", "Genre" };

	@Autowired
	private MusicLibraryService musicLibraryService;

//	private volatile List<Integer> searchMatchedRows = Collections.emptyList();
	
	// Filter state
	private String artistFilter = "";
	private String albumFilter = "";
	private String yearFilter = "";

	// Columns
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int col) {
		return columnNames[col];
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return String.class;
	}
	//

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public String getValueAt(int row, int column) {
		MusicLibraryEntry entry = musicLibraryService.getEntryByIndex(row);
		switch (column) {
			default:
			case 0:
				return "" + (row + 1);
			case 1:
				return StringUtil.blankForNull(entry.getTrackNumber());
			case 2:
				return entry.getTitle();
			case 3:
				return entry.getArtist();
			case 4:
				return entry.getAlbum();
			case 5:
				return entry.getDuration() != null ? TimeDateUtil.prettyPrintFromSeconds(entry.getDuration()) : "";
			case 6:
				return entry.getDate();
			case 7:
				return entry.getGenre();
		}
	}

	public MusicLibraryEntry getEntry(int row) {
		return musicLibraryService.getEntryByIndex(row);
	}

	@Override
	public int getRowCount() {
		return musicLibraryService.getSize();
	}

	public long getLibraryLength() {
		// TODO: Implement library length calculation
		return 0L;
	}
	
	public void setArtistFilter(String artist) {
		this.artistFilter = artist != null ? artist : "";
	}
	
	public void setAlbumFilter(String album) {
		this.albumFilter = album != null ? album : "";
	}
	
	public void setYearFilter(String year) {
		this.yearFilter = year != null ? year : "";
	}
	
	public String getArtistFilter() {
		return artistFilter;
	}
	
	public String getAlbumFilter() {
		return albumFilter;
	}
	
	public String getYearFilter() {
		return yearFilter;
	}

	private Predicate<MusicLibraryEntry> searchPredicate(String searchText, boolean searchFullPhrase) {
		searchText = normalizeForSearch(searchText);
		String[] searchTerms;
		if (searchFullPhrase) {
			searchTerms = new String[] { searchText };
		} else {
			searchTerms = searchText.split(" ");
		}
		return queueEntry -> queueEntryMatchesSearch(queueEntry, searchTerms);
	}

	private static final List<Function<MusicLibraryEntry, String>> LIBRARY_ENTRY_TO_SEARCH_FIELDS = Arrays.asList(
			MusicLibraryEntry::getArtist, MusicLibraryEntry::getAlbum, MusicLibraryEntry::getTitle, MusicLibraryEntry::getDate,
			MusicLibraryEntry::getGenre);

	private boolean queueEntryMatchesSearch(MusicLibraryEntry queueEntry, String[] searchTerms) {
		for (String term : searchTerms) {
			boolean match = false;
			for (Function<MusicLibraryEntry, String> getFieldFunct : LIBRARY_ENTRY_TO_SEARCH_FIELDS) {
				if (normalizeForSearch(getFieldFunct.apply(queueEntry)).contains(term)) {
					match = true;
					break;
				}
			}
			if (!match) {
				return false;
			}
		}
		return true;
	}

	private String normalizeForSearch(String val) {
		return StringUtil.stripAccents(StringUtil.blankForNull(val)).toLowerCase().replaceAll("\\s+", " ").trim();
	}

	public List<Integer> search(String text, boolean fullPhrase) {
		List<Integer> result = IntStream.of(musicLibraryService.search(searchPredicate(text, fullPhrase)).stream()
				.mapToInt(entry -> musicLibraryService.getEntries().indexOf(entry))
				.toArray())
				.mapToObj(Integer::valueOf)
				.collect(Collectors.toList());
		return result;
	}
	
	public Predicate<MusicLibraryEntry> getFilterPredicate() {
		return entry -> {
			if (!artistFilter.isEmpty()) {
				String artist = entry.getArtist();
				if (artist == null || !normalizeForSearch(artist).contains(normalizeForSearch(artistFilter))) {
					return false;
				}
			}
			
			if (!albumFilter.isEmpty()) {
				String album = entry.getAlbum();
				if (album == null || !normalizeForSearch(album).contains(normalizeForSearch(albumFilter))) {
					return false;
				}
			}
			
			if (!yearFilter.isEmpty()) {
				String date = entry.getDate();
				if (date == null || !normalizeForSearch(date).contains(normalizeForSearch(yearFilter))) {
					return false;
				}
			}
			
			return true;
		};
	}
	
	public Set<String> getUniqueArtists() {
		return musicLibraryService.getEntries().stream()
				.map(MusicLibraryEntry::getArtist)
				.filter(artist -> artist != null && !artist.trim().isEmpty())
				.map(StringUtil::stripAccents)
				.collect(Collectors.toSet());
	}
	
	public Set<String> getUniqueAlbums() {
		return musicLibraryService.getEntries().stream()
				.map(MusicLibraryEntry::getAlbum)
				.filter(album -> album != null && !album.trim().isEmpty())
				.map(StringUtil::stripAccents)
				.collect(Collectors.toSet());
	}
	
	public Set<String> getUniqueYears() {
		return musicLibraryService.getEntries().stream()
				.map(MusicLibraryEntry::getDate)
				.filter(date -> date != null && !date.trim().isEmpty())
				.map(StringUtil::stripAccents)
				.collect(Collectors.toSet());
	}
	
	public void clearFilters() {
		this.artistFilter = "";
		this.albumFilter = "";
		this.yearFilter = "";
	}
}