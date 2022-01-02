package x.mvmn.sonivm.ui.retro;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
	@Getter
	protected final PlaylistColors playlistColors;

	@Getter
	protected final JTextField retroUISearchInput;
	protected final JButton retroUISearchNextMatchButton;
	protected final JButton retroUISearchPrevMatchButton;
	@Getter
	protected final JLabel retroUISearchMatchCountLabel;
	protected final JButton retroUISearchClearButton;
	@Getter
	protected final JCheckBox retroUISerchCheckboxFullPhrase;

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
			this.playlistTable.scrollRectToVisible(new Rectangle(playlistTable.getCellRect(rowNumber, 0, true)));
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

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK), "Search");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "Search");
		playQueueActionMap.put("Search", AbstractActionAdaptor.of(() -> retroUISearchInput.requestFocus()));

		retroUISerchCheckboxFullPhrase.addItemListener(e -> listener.onRetroUISearchTextChange());
		retroUISearchInput.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				listener.onRetroUISearchTextChange();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				listener.onRetroUISearchTextChange();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub

			}
		});

		retroUISearchClearButton.addActionListener(actEvent -> {
			retroUISearchInput.setText("");
			listener.onRetroUISearchTextChange();
		});

		retroUISearchNextMatchButton.addActionListener(actEvent -> listener.onSearchNextMatch());
		retroUISearchPrevMatchButton.addActionListener(actEvent -> listener.onSearchPreviousMatch());

		InputMap searchInputMap = retroUISearchInput.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap searchActionMap = retroUISearchInput.getActionMap();

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Select");
		searchActionMap.put("Select", onSelect);
		retroUISearchInput.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				int code = e.getKeyCode();
				switch (code) {
					case KeyEvent.VK_UP: {
						listener.onSearchPreviousMatch();
						break;
					}

					case KeyEvent.VK_DOWN: {
						listener.onSearchNextMatch();
						break;
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {}
		});
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
