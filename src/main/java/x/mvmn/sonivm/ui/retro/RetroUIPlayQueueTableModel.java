package x.mvmn.sonivm.ui.retro;

import javax.swing.table.AbstractTableModel;

import x.mvmn.sonivm.ui.SonivmUI;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.util.Tuple3;

public class RetroUIPlayQueueTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 5628564877472125514L;

	private final SonivmUI ui;
	private final PlaybackQueueTableModel playQueueTableModel;
	private final String[] COLUMN_NAMES = { "#", "N", "Track", "Length" };

	public RetroUIPlayQueueTableModel(SonivmUI ui, PlaybackQueueTableModel playQueueTableModel) {
		this.ui = ui;
		this.playQueueTableModel = playQueueTableModel;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnIndex == 0) {
			return playQueueTableModel.getValueAt(rowIndex, 0);
		} else if (columnIndex == 1) {
			return playQueueTableModel.getValueAt(rowIndex, 1);
		} else if (columnIndex == 2) {
			String artist = playQueueTableModel.getValueAt(rowIndex, 3);
			String title = playQueueTableModel.getValueAt(rowIndex, 2);
			String album = playQueueTableModel.getValueAt(rowIndex, 4);
			String date = playQueueTableModel.getValueAt(rowIndex, 6);
			if (artist != null) {
				StringBuilder result = new StringBuilder();
				result.append(artist);
				Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> retroUIWindows = ui.getRetroUIWindows();
				if (retroUIWindows != null
						&& retroUIWindows.getC().getPlaylistTable().getColumnModel().getColumn(2).getWidth() > (int) retroUIWindows.getC()
								.getPlaylistTable()
								.getFontMetrics(retroUIWindows.getC().getPlaylistTable().getFont())
								.getStringBounds("average artist name average album name 1234 - average track name",
										retroUIWindows.getC().getPlaylistTable().getGraphics())
								.getWidth()) {
					if (album != null) {
						result.append(" \"").append(album).append("\"");
					}
					if (date != null) {
						result.append(" (").append(date).append(")");
					}
				}
				if (result.length() > 0) {
					result.append(" - ");
				}
				result.append(title);
				return result.toString();
			} else {
				return title;
			}
		} else if (columnIndex == 3) {
			return playQueueTableModel.getValueAt(rowIndex, 5);
		}
		return null;
	}

	@Override
	public String getColumnName(int col) {
		return COLUMN_NAMES[col];
	}

	@Override
	public int getRowCount() {
		return playQueueTableModel.getRowCount();
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public Class<?> getColumnClass(int col) {
		return String.class;
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}