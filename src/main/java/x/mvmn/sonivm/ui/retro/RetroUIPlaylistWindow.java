package x.mvmn.sonivm.ui.retro;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import x.mvmn.sonivm.ui.AbstractActionAdaptor;
import x.mvmn.sonivm.ui.SonivmUIController;
import x.mvmn.sonivm.ui.retro.rasterui.RasterFrameWindow;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

@AllArgsConstructor
@Builder
public class RetroUIPlaylistWindow {

	@Getter
	protected final RasterFrameWindow window;
	@Getter
	protected final JTable playlistTable;
	protected final PlaylistColors playlistColors;

	@AllArgsConstructor
	@Builder
	public static class PlaylistColors {
		@Getter
		protected final Color backgroundColor;
		@Getter
		protected final Color textColor;
		@Getter
		protected final Color currentTrackTextColor;
		@Getter
		protected final Color selectionBackgroundColor;
	}

	public void scrollToTrack(int rowNumber) {
		if (rowNumber > -1) {
			window.scrollToEntry(rowNumber);
		}
	}

	public void addListener(SonivmUIController listener) {
		// TODO: reuse code with SonivmMainWindow
		playlistTable.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2 && playlistTable.getSelectedRow() != -1) {
					Point point = mouseEvent.getPoint();
					int row = playlistTable.rowAtPoint(point);
					listener.onTrackSelect(row);
				}
			}
		});

		InputMap playQueueInputMap = playlistTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap playQueueActionMap = playlistTable.getActionMap();
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "Delete");
		playQueueActionMap.put("Delete", new AbstractAction() {
			private static final long serialVersionUID = 8828376654199394308L;

			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = playlistTable.getSelectedRows();
				if (selectedRows != null && selectedRows.length > 0) {
					listener.onDeleteRowsFromQueue(selectedRows[0], selectedRows[selectedRows.length - 1]);
				}
			}
		});

		AbstractAction onSelect = AbstractActionAdaptor.of(event -> {
			int selectedRow = playlistTable.getSelectedRow();
			if (selectedRow >= 0 && selectedRow < playlistTable.getRowCount()) {
				listener.onTrackSelect(selectedRow);
			}
		});

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Select");
		playQueueActionMap.put("Select", onSelect);

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "PlayPause");
		playQueueActionMap.put("PlayPause", AbstractActionAdaptor.of(listener::onPlayPause));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.META_DOWN_MASK), "PlayPause");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "PlayPause");

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.META_DOWN_MASK), "NextTrack");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK), "NextTrack");
		playQueueActionMap.put("NextTrack", AbstractActionAdaptor.of(listener::onNextTrack));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.META_DOWN_MASK), "PreviousTrack");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK), "PreviousTrack");
		playQueueActionMap.put("PreviousTrack", AbstractActionAdaptor.of(listener::onPreviousTrack));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.META_DOWN_MASK), "Stop");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "Stop");
		playQueueActionMap.put("Stop", AbstractActionAdaptor.of(listener::onStop));
	}

	public int[] getPlayQueueTableColumnPositions() {
		return SwingUtil.getJTableColumnPositions(playlistTable);
	}

	public void setPlayQueueTableColumnPositions(int[] playQueueColumnPositions) {
		SwingUtil.applyJTableColumnPositions(playlistTable, playQueueColumnPositions);
	}

	public int[] getPlayQueueTableColumnWidths() {
		return SwingUtil.getJTableColumnWidths(playlistTable);
	}

	public void setPlayQueueTableColumnWidths(int[] playQueueColumnWidths) {
		SwingUtil.applyJTableColumnWidths(playlistTable, playQueueColumnWidths);
	}

}
