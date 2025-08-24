package x.mvmn.sonivm.ui.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.util.StringUtil;
import x.mvmn.sonivm.util.TimeDateUtil;

@Component
public class PlaybackQueueTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -4393495956274405244L;

	private static final String[] columnNames = new String[] { "#", "N", "Title", "Artist", "Album", "Length", "Date", "Genre" };

	@Autowired
	private PlaybackQueueService playQueueService;

	private volatile List<Integer> searchMatchedRows = Collections.emptyList();

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
		int currentQueuePlayPosition = playQueueService.getCurrentQueuePosition();
		PlaybackQueueEntry entry = playQueueService.getEntryByIndex(row);
		boolean nowPlayed = currentQueuePlayPosition == row && playQueueService.getCurrentQueue() == playQueueService.getCurrentPlayQueue();
		switch (column) {
			default:
			case 0:
				return (nowPlayed ? "\uD83D\uDD0A " : "") + (row + 1);
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

	public PlaybackQueueEntry getEntry(int row) {
		return playQueueService.getEntryByIndex(row);
	}

	@Override
	public int getRowCount() {
		return playQueueService.getQueueSize();
	}

	public int getIndexOfHighlightedRow() {
		return playQueueService.getCurrentPlayQueue() == playQueueService.getCurrentQueue() ? playQueueService.getCurrentQueuePosition()
				: -1;
	}

	public int getCurrentQueuePosition() {
		return playQueueService.getCurrentQueuePosition();
	}

	public boolean isSearchMatched(Integer row) {
		return this.searchMatchedRows.contains(row);
	}

	public void setSearchMatchedRows(List<Integer> rows) {
		// fireRowsChanged(this.searchMatchedRows);
		this.searchMatchedRows = rows;
		// fireRowsChanged(rows);
	}

	// private void fireRowsChanged(List<Integer> rows) {
	// if (!rows.isEmpty()) {
	// rows.forEach(row -> fireTableRowsUpdated(row, row));
	// }
	// }

	private Predicate<PlaybackQueueEntry> searchPredicate(String searchText, boolean searchFullPhrase) {
		searchText = normalizeForSearch(searchText);
		String[] searchTerms;
		if (searchFullPhrase) {
			searchTerms = new String[] { searchText };
		} else {
			searchTerms = searchText.split(" ");
		}
		return queueEntry -> queueEntryMatchesSearch(queueEntry, searchTerms);
	}

	private static final List<Function<PlaybackQueueEntry, String>> QUEUE_ENTRY_TO_SEARCH_FIELDS = Arrays.asList(
			PlaybackQueueEntry::getArtist, PlaybackQueueEntry::getAlbum, PlaybackQueueEntry::getTitle, PlaybackQueueEntry::getDate,
			PlaybackQueueEntry::getGenre);

	private boolean queueEntryMatchesSearch(PlaybackQueueEntry queueEntry, String[] searchTerms) {
		for (String term : searchTerms) {
			boolean match = false;
			for (Function<PlaybackQueueEntry, String> getFieldFunct : QUEUE_ENTRY_TO_SEARCH_FIELDS) {
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
		List<Integer> result = IntStream.of(playQueueService.findTracks(searchPredicate(text, fullPhrase)))
				.mapToObj(Integer::valueOf)
				.collect(Collectors.toList());
		setSearchMatchedRows(result);
		return result;
	}

	public void rowChanged(int row) {
		playQueueService.signalUpdateInRow(row);
	}

	public void addQueue(String queueName) {
		playQueueService.addQueue(queueName);
	}

	public int getQueuesCount() {
		return playQueueService.getQueuesCount();
	}

	public String getQueueName(int index) {
		return playQueueService.getQueueName(index);
	}

	public void deleteQueue(int index) {
		playQueueService.removeQueue(index);
	}

	public void switchQueue(int index) {
		playQueueService.setCurrentQueue(index);
	}

	public int getCurrentPlayQueue() {
		return playQueueService.getCurrentPlayQueue();
	}
}
