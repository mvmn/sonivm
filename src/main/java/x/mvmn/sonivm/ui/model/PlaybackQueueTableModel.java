package x.mvmn.sonivm.ui.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.springframework.stereotype.Component;

import x.mvmn.sonivm.util.TimeUnitUtil;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

@Component
public class PlaybackQueueTableModel extends AbstractTableModel {
	private static final long serialVersionUID = -4393495956274405244L;

	private volatile int currentQueuePosition = -1;

	private static final String[] columnNames = new String[] { "#", "N", "Title", "Artist", "Album", "Length", "Date", "Genre" };

	private List<PlaybackQueueEntry> data = Collections.synchronizedList(new ArrayList<PlaybackQueueEntry>());

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

	// Queue position
	public int getCurrentQueuePosition() {
		return currentQueuePosition;
	}

	public PlaybackQueueEntry getCurrentEntry() {
		return currentQueuePosition >= 0 ? data.get(currentQueuePosition) : null;
	}

	public void setCurrentQueuePosition(int newPosition) {
		int rows = this.getRowCount();
		int oldPosition = this.currentQueuePosition;
		if (newPosition >= 0 && newPosition < rows) {
			this.currentQueuePosition = newPosition;
			SwingUtil.runOnEDT(() -> fireTableCellUpdated(newPosition, 0), false);
		} else {
			this.currentQueuePosition = -1;
		}
		if (oldPosition >= 0 && oldPosition < rows) {
			SwingUtil.runOnEDT(() -> fireTableCellUpdated(oldPosition, 0), false);
		}
	}
	//

	@Override
	public String getValueAt(int row, int column) {
		PlaybackQueueEntry entry = data.get(row);
		switch (column) {
			default:
			case 0:
				return (currentQueuePosition == row ? " >" : "") + (row + 1);
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
		return data.size();
	}

	public void addRows(Collection<PlaybackQueueEntry> newRows) {
		addRows(-1, newRows);
	}

	public void addRows(int atIndex, Collection<PlaybackQueueEntry> newRows) {
		int dataSizeBeforeAdd = data.size();
		if (atIndex < 0 || atIndex >= dataSizeBeforeAdd) {
			int numberAdded = newRows.size();
			data.addAll(newRows);
			SwingUtil.runOnEDT(() -> this.fireTableRowsInserted(dataSizeBeforeAdd, dataSizeBeforeAdd + numberAdded - 1), true);
		} else {
			int numberAdded = newRows.size();
			data.addAll(atIndex, newRows);
			SwingUtil.runOnEDT(() -> this.fireTableRowsInserted(atIndex, atIndex + numberAdded - 1), true);
		}
	}

	public void deleteRows(int fromIndex, int toIndex) {
		int dataSizeBeforeDelete = data.size();
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (toIndex < 0) {
			toIndex = 0;
		}
		if (fromIndex >= dataSizeBeforeDelete) {
			fromIndex = dataSizeBeforeDelete - 1;
		}
		if (toIndex > dataSizeBeforeDelete) {
			toIndex = dataSizeBeforeDelete;
		}

		if (fromIndex < toIndex) {
			for (int i = toIndex - 1; i >= fromIndex; i--) {
				data.remove(i);
			}
			int firstRow = fromIndex;
			int lastRow = toIndex - 1;
			SwingUtil.runOnEDT(() -> this.fireTableRowsDeleted(firstRow, lastRow - 1), true);
		}
	}

	public PlaybackQueueEntry getRowValue(int row) {
		return row >= 0 && row < data.size() ? data.get(row) : null;
	}

	public void signalUpdateInRow(int rowIndex) {
		SwingUtil.runOnEDT(() -> this.fireTableRowsUpdated(rowIndex, rowIndex), false);
	}
}
