package x.mvmn.sonivm.ui.model;

import javax.annotation.PostConstruct;
import javax.swing.table.AbstractTableModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.playqueue.PlaybackQueueChangeListener;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.util.TimeUnitUtil;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

@Component
public class PlaybackQueueTableModel extends AbstractTableModel implements PlaybackQueueChangeListener {
	private static final long serialVersionUID = -4393495956274405244L;

	private static final String[] columnNames = new String[] { "#", "N", "Title", "Artist", "Album", "Length", "Date", "Genre" };

	@Autowired
	private PlaybackQueueService playQueueService;

	@PostConstruct
	private void initPostConstruct() {
		playQueueService.setChangeListener(this);
	}

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
		switch (column) {
			default:
			case 0:
				return (currentQueuePlayPosition == row ? "> " : "") + (row + 1);
			case 1:
				return entry.getTrackNumber() != null ? String.valueOf(entry.getTrackNumber()) : "";
			case 2:
				return entry.getTitle();
			case 3:
				return entry.getArtist();
			case 4:
				return entry.getAlbum();
			case 5:
				return entry.getDuration() != null ? TimeUnitUtil.prettyPrintFromSeconds(entry.getDuration()) : "";
			case 6:
				return entry.getDate();
			case 7:
				return entry.getGenre();
		}
	}

	@Override
	public int getRowCount() {
		return playQueueService.queueSize();
	}

	public int getIndexOfHighlightedRow() {
		return playQueueService.getCurrentQueuePosition();
	}

	@Override
	public void onTableRowsUpdate(int firstRow, int lastRow, boolean waitForUiUpdate) {
		SwingUtil.runOnEDT(() -> this.fireTableRowsUpdated(firstRow, lastRow), waitForUiUpdate);
	}

	@Override
	public void onTableRowsInsert(int firstRow, int lastRow, boolean waitForUiUpdate) {
		SwingUtil.runOnEDT(() -> this.fireTableRowsInserted(firstRow, lastRow), waitForUiUpdate);
	}

	@Override
	public void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate) {
		SwingUtil.runOnEDT(() -> this.fireTableRowsDeleted(firstRow, lastRow), waitForUiUpdate);
	}
}
