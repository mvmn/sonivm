package x.mvmn.sonivm.ui.model;

import javax.swing.table.DefaultTableModel;

import org.springframework.stereotype.Component;

@Component
public class PlaybackQueueTableModel extends DefaultTableModel {
	private static final long serialVersionUID = -4393495956274405244L;

	private volatile int currentQueuePosition = -1;

	public PlaybackQueueTableModel() {
		super(new String[] { "#", "N", "Title", "Artist", "Album", "Length", "Date", "Genre" }, 0);
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public int getCurrentQueuePosition() {
		return currentQueuePosition;
	}

	public void setCurrentQueuePosition(int newPosition) {
		int oldPosition = this.currentQueuePosition;
		int rows = this.getRowCount();
		if (oldPosition >= 0 && oldPosition < rows) {
			fireTableCellUpdated(newPosition, oldPosition);
		}
		this.currentQueuePosition = newPosition;
	}

	@Override
	public Object getValueAt(int row, int column) {
		if (column == 0) {
			return (currentQueuePosition == row ? " >" : "") + (row + 1);
		}
		return super.getValueAt(row, column);
	}
}
