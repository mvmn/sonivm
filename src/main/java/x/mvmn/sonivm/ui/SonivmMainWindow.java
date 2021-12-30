package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.model.RepeatMode;
import x.mvmn.sonivm.ui.model.ShuffleMode;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.TimeDateUtil;

public class SonivmMainWindow extends JFrame {
	private static final long serialVersionUID = -3402450540379541023L;

	private final PlaybackQueueTableModel playbackQueueTableModel;
	private final JLabel lblStatus;
	private final JLabel lblNowPlayingTrack;
	private final JLabel lblPlayTimeElapsed;
	private final JLabel lblPlayTimeRemaining;
	private final JTable tblPlayQueue;
	private final JButton btnPlayPause;
	private final JButton btnStop;
	private final JButton btnNextTrack;
	private final JButton btnPreviousTrack;
	private final JProgressBar playbackProgressBar;
	private final JSlider volumeSlider;
	private final JComboBox<RepeatMode> cmbRepeatMode;
	private final JComboBox<ShuffleMode> cmbShuffleMode;
	private final JLabel lastFMStatusIcon;
	private final ImageIcon lastFMDefault;
	private final ImageIcon lastFMConnected;
	private final ImageIcon lastFMDisconnected;
	private final JTextField tfSearch;
	private final JCheckBox cbSearchFullPhrase;
	private final JButton btnSearchClear;
	private final JButton btnSearchNextMatch;
	private final JButton btnSearchPreviousMatch;
	private final JLabel lblSearchMatchesCount;
	private final JLabel lblQueueSize;
	private final JButton btnToggleShowEq;
	private final JCheckBox cbAutoStop;

	private volatile List<Integer> searchMatches = Collections.emptyList();
	private volatile int currentSearchMatch = -1;

	public SonivmMainWindow(String title, SonivmController controller, PlaybackQueueTableModel playbackQueueTableModel) {
		super(title);
		this.playbackQueueTableModel = playbackQueueTableModel;

		tblPlayQueue = new JTable(playbackQueueTableModel);
		tblPlayQueue.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		tblPlayQueue.setCellSelectionEnabled(false);
		tblPlayQueue.setRowSelectionAllowed(true);
		tblPlayQueue.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 8392498039681170058L;

			private final JLabel renderJLabel = new JLabel();
			{
				renderJLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
				renderJLabel.setOpaque(true);
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
					int column) {
				// Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				renderJLabel.setText(value != null ? value.toString() : "");

				boolean isHighlighted = row == playbackQueueTableModel.getIndexOfHighlightedRow();
				renderJLabel.setFont(isHighlighted ? tblPlayQueue.getFont().deriveFont(Font.BOLD) : tblPlayQueue.getFont());
				int originalColumnIndex = tblPlayQueue.convertColumnIndexToModel(column);
				renderJLabel.setHorizontalAlignment(
						originalColumnIndex == 0 || originalColumnIndex == 1 || originalColumnIndex == 5 || originalColumnIndex == 6
								? JLabel.RIGHT
								: JLabel.LEFT);

				Color fgRegular = table.getForeground();
				Color bgRegular = table.getBackground();

				Color fgSelected = table.getSelectionForeground();
				Color bgSelected = table.getSelectionBackground();

				// From DefaultTableCellRenderer
				JTable.DropLocation dropLocation = table.getDropLocation();
				if (dropLocation != null && !dropLocation.isInsertRow() && !dropLocation.isInsertColumn() && dropLocation.getRow() == row
				// && dropLocation.getColumn() == column
				) {
					isSelected = true;
					fgSelected = lookupInUIDefaults("Table.dropCellForeground", fgSelected);
					bgSelected = lookupInUIDefaults("Table.dropCellBackground", bgSelected);
				}

				// if (hasFocus) {
				// isSelected = true;
				//
				// fgSelected = lookupInUIDefaults("Table.focusCellForeground", fgSelected);
				// bgSelected = lookupInUIDefaults("Table.focusCellBackground", bgSelected);
				// }

				if (isHighlighted) {
					bgRegular = altColor(bgRegular, false, 50, -10, -10);
				} else if (playbackQueueTableModel.isSearchMatched(row)) {
					bgRegular = altColor(bgRegular, false, -10, 50, 10);
				} else if (row % 2 == 0) {
					bgRegular = SwingUtil.modifyColor(bgRegular, -10, -10, -10);
				}

				fgRegular = altColor(fgRegular, isHighlighted, 20, 20, 20);
				fgSelected = altColor(fgSelected, isHighlighted, 20, 20, 20);

				renderJLabel.setForeground(isSelected ? fgSelected : fgRegular);
				renderJLabel.setBackground(isSelected ? bgSelected : bgRegular);

				// super.setForeground(isSelected ? fgSelected : fgRegular);
				// super.setBackground(isSelected ? bgSelected : bgRegular);

				return renderJLabel;
			}

			private Color altColor(Color color, boolean highlight, int deltaRed, int deltaGreen, int deltaBlue) {
				if (color.getRed() > 127 && !highlight) {
					return SwingUtil.modifyColor(color, -deltaRed, -deltaGreen, -deltaBlue);
				} else {
					return SwingUtil.modifyColor(color, deltaRed, deltaGreen, deltaBlue);
				}
			}

			@Override
			public boolean isOpaque() {
				return true;
			}

			private Color lookupInUIDefaults(String key, Color defaultColor) {
				Color result = UIManager.getColor(key);
				return result != null ? result : defaultColor;
			}
		});

		// DefaultTableCellRenderer rightRendererForDuration = new DefaultTableCellRenderer();
		// rightRendererForDuration.setHorizontalAlignment(JLabel.RIGHT);
		// tblPlayQueue.getColumnModel().getColumn(5).setCellRenderer(rightRendererForDuration);
		// DefaultTableCellRenderer rightRendererForDate = new DefaultTableCellRenderer();
		// rightRendererForDate.setHorizontalAlignment(JLabel.RIGHT);
		// tblPlayQueue.getColumnModel().getColumn(6).setCellRenderer(rightRendererForDate);

		btnPlayPause = new JButton("->");
		btnStop = new JButton("[x]");
		btnNextTrack = new JButton(">>");
		btnPreviousTrack = new JButton("<<");
		lblStatus = new JLabel("");
		lblNowPlayingTrack = new JLabel("");
		lblPlayTimeElapsed = new JLabel("00:00 / 00:00");
		lblPlayTimeRemaining = new JLabel("-00:00 / 00:00");
		lblQueueSize = new JLabel("" + playbackQueueTableModel.getRowCount());

		tfSearch = new JTextField("");
		tfSearch.setMinimumSize(
				new Dimension(tfSearch.getFontMetrics(tfSearch.getFont()).stringWidth("Hello to all the world out there 12345"),
						tfSearch.getMinimumSize().height));
		tfSearch.setPreferredSize(tfSearch.getMinimumSize());

		btnSearchNextMatch = new JButton("V");
		btnSearchPreviousMatch = new JButton("^");
		lblSearchMatchesCount = new JLabel("0");
		btnSearchClear = new JButton("x");

		cmbShuffleMode = new JComboBox<>(ShuffleMode.values());
		cmbRepeatMode = new JComboBox<>(RepeatMode.values());

		cbAutoStop = new JCheckBox("Auto-stop");
		cbAutoStop.setFocusable(false);

		cmbShuffleMode.setFocusable(false);
		cmbRepeatMode.setFocusable(false);

		btnToggleShowEq = new JButton("EQ");

		playbackProgressBar = new JProgressBar(0, 0);
		playbackProgressBar.setIndeterminate(false);
		playbackProgressBar.setFocusable(false);

		volumeSlider = new JSlider(JSlider.VERTICAL, 0, 100, 100);
		volumeSlider.setFocusable(false);
		SwingUtil.makeJSliderMoveToClickPoistion(volumeSlider);

		JPanel playbackButtonsPanel = new JPanel(new GridLayout(1, 4));
		JButton[] buttons = new JButton[] { btnPreviousTrack, btnPlayPause, btnStop, btnNextTrack };
		Stream.of(buttons).forEach(playbackButtonsPanel::add);
		Stream.of(buttons).forEach(btn -> btn.setFocusable(false));

		JLabel lblRepeat = new JLabel("Repeat: ", JLabel.RIGHT);
		JLabel lblShuffle = new JLabel("Shuffle: ", JLabel.RIGHT);
		lblRepeat.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		lblShuffle.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

		JLabel nowPlayingText = new JLabel("Now playing: ");
		nowPlayingText.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 2));
		JPanel topPanel = SwingUtil.panel(BorderLayout::new)
				.addEast(playbackButtonsPanel)
				.addCenter(SwingUtil.panel(BorderLayout::new)
						.addWest(lblPlayTimeElapsed)
						.addCenter(playbackProgressBar)
						.addEast(lblPlayTimeRemaining)
						.build())
				.addNorth(SwingUtil.panel(() -> new BorderLayout())
						.addWest(nowPlayingText)
						.addCenter(lblNowPlayingTrack)
						.addEast(SwingUtil.panel(pnl -> new BoxLayout(pnl, BoxLayout.X_AXIS))
								.add(cbAutoStop)
								.addSeparator(true)
								.add(lblRepeat)
								.add(cmbRepeatMode)
								.add(lblShuffle)
								.add(cmbShuffleMode)
								.add(btnToggleShowEq)
								.build())
						.build())
				.build();
		topPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		{
			int lastFMIconSize = 16;
			lastFMDefault = ImageUtil.resizeImageIcon(ImageUtil.fromClasspathResource("/lastfm_default.png"), lastFMIconSize,
					lastFMIconSize, Image.SCALE_SMOOTH);
			lastFMConnected = ImageUtil.resizeImageIcon(ImageUtil.fromClasspathResource("/lastfm_connected.png"), lastFMIconSize,
					lastFMIconSize, Image.SCALE_SMOOTH);
			lastFMDisconnected = ImageUtil.resizeImageIcon(ImageUtil.fromClasspathResource("/lastfm_disconnected.png"), lastFMIconSize,
					lastFMIconSize, Image.SCALE_SMOOTH);
		}

		lastFMStatusIcon = new JLabel();
		lastFMStatusIcon.setHorizontalAlignment(JLabel.RIGHT);
		lastFMStatusIcon.setIcon(lastFMDefault);
		lastFMStatusIcon.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

		cbSearchFullPhrase = new JCheckBox("full phrase");

		JPanel bottomPanel = SwingUtil.panel(BorderLayout::new)
				.addCenter(lblStatus)
				.addEast(SwingUtil.panel(pnl -> new BoxLayout(pnl, BoxLayout.X_AXIS))
						.addSeparator(true)
						.add(SwingUtil.withEmptyBorder(new JLabel("Tracks in queue:"), 0, 4, 0, 0))
						.add(SwingUtil.withEmptyBorder(lblQueueSize, 0, 2, 0, 4))
						.addSeparator(true)
						.add(SwingUtil.withEmptyBorder(new JLabel("Search:"), 0, 4, 0, 0))
						.add(tfSearch)
						.add(btnSearchClear)
						.add(cbSearchFullPhrase)
						.add(SwingUtil.withEmptyBorder(new JLabel("Matches:"), 0, 4, 0, 0))
						.add(SwingUtil.withEmptyBorder(lblSearchMatchesCount, 0, 2, 0, 4))
						.add(btnSearchNextMatch)
						.add(btnSearchPreviousMatch)
						.addSeparator(true)
						.add(lastFMStatusIcon)
						.build())
				.build();
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		volumeSlider.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		playbackProgressBar.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		lblPlayTimeElapsed.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		lblPlayTimeRemaining.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		lblNowPlayingTrack.setFont(lblNowPlayingTrack.getFont().deriveFont(Font.BOLD));

		JLabel lblDropTarget = new JLabel("add to queue", JLabel.CENTER);
		lblDropTarget.setToolTipText("Drag&drop here to add/move to the end of the playback queue");
		lblDropTarget.setBorder(BorderFactory.createEtchedBorder());

		JScrollPane scrollTblPlayQueue = new JScrollPane(tblPlayQueue);
		tblPlayQueue.setDragEnabled(true);
		tblPlayQueue.setDropMode(DropMode.USE_SELECTION);
		tblPlayQueue.setTransferHandler(new PlayQueueTableDnDTransferHandler(tblPlayQueue, controller));

		scrollTblPlayQueue.setDropTarget(new PlaybackQueueDropTarget(controller, tblPlayQueue));
		lblDropTarget.setDropTarget(new PlaybackQueueDropTarget(controller, tblPlayQueue));

		MouseListener onNowPlayingDoubleClick = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int nowPlayingQueuePos = playbackQueueTableModel.getIndexOfHighlightedRow();
					if (nowPlayingQueuePos > -1) {
						scrollToTrack(nowPlayingQueuePos);
					}
				}
			}
		};
		lblNowPlayingTrack.addMouseListener(onNowPlayingDoubleClick);
		nowPlayingText.addMouseListener(onNowPlayingDoubleClick);

		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(topPanel, BorderLayout.NORTH);
		this.getContentPane()
				.add(SwingUtil.panel(BorderLayout::new)
						.add(scrollTblPlayQueue, BorderLayout.CENTER)
						.add(lblDropTarget, BorderLayout.SOUTH)
						.build(), BorderLayout.CENTER);
		this.getContentPane().add(volumeSlider, BorderLayout.EAST);
		this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		registerActionsWithController(controller);
		//
		// this.addWindowListener(new WindowAdapter() {
		// @Override
		// public void windowClosing(WindowEvent wndEvent) {
		// controller.onQuit();
		// }
		// });
	}

	private void registerActionsWithController(SonivmController controller) {
		volumeSlider.addChangeListener(event -> controller.onVolumeChange(volumeSlider.getValue()));
		SwingUtil.makeJProgressBarMoveToClickPosition(playbackProgressBar, val -> controller.onSeek(val));

		btnPlayPause.addActionListener(event -> controller.onPlayPause());
		btnStop.addActionListener(event -> controller.onStop());
		btnNextTrack.addActionListener(event -> controller.onNextTrack());
		btnPreviousTrack.addActionListener(event -> controller.onPreviousTrack());

		btnToggleShowEq.addActionListener(actEvent -> controller.toggleShowEqualizer());
		btnToggleShowEq.setFocusable(false);

		tblPlayQueue.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2 && tblPlayQueue.getSelectedRow() != -1) {
					Point point = mouseEvent.getPoint();
					int row = tblPlayQueue.rowAtPoint(point);
					controller.onTrackSelect(row);
				}
			}
		});

		InputMap playQueueInputMap = tblPlayQueue.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap playQueueActionMap = tblPlayQueue.getActionMap();
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Delete");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "Delete");
		playQueueActionMap.put("Delete", new AbstractAction() {
			private static final long serialVersionUID = 8828376654199394308L;

			public void actionPerformed(ActionEvent e) {
				int[] selectedRows = tblPlayQueue.getSelectedRows();
				if (selectedRows != null && selectedRows.length > 0) {
					controller.onDeleteRowsFromQueue(selectedRows[0], selectedRows[selectedRows.length - 1]);
				}
			}
		});

		AbstractAction onSelect = AbstractActionAdaptor.of(event -> {
			int selectedRow = tblPlayQueue.getSelectedRow();
			if (selectedRow >= 0 && selectedRow < tblPlayQueue.getRowCount()) {
				controller.onTrackSelect(selectedRow);
			}
		});
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Select");
		playQueueActionMap.put("Select", onSelect);

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "PlayPause");
		playQueueActionMap.put("PlayPause", AbstractActionAdaptor.of(controller::onPlayPause));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.META_DOWN_MASK), "Search");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "Search");
		playQueueActionMap.put("Search", AbstractActionAdaptor.of(() -> tfSearch.requestFocus()));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.META_DOWN_MASK), "Shuffle");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "Shuffle");

		AbstractAction onShuffle = AbstractActionAdaptor.of(event -> {
			int selected = cmbShuffleMode.getSelectedIndex();
			cmbShuffleMode.setSelectedIndex((selected >= cmbShuffleMode.getItemCount() - 1) ? 0 : ++selected);
		});
		playQueueActionMap.put("Shuffle", onShuffle);

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_DOWN_MASK), "Repeat");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "Repeat");
		AbstractAction onRepeat = AbstractActionAdaptor.of(event -> {
			int selected = cmbRepeatMode.getSelectedIndex();
			cmbRepeatMode.setSelectedIndex((selected >= cmbRepeatMode.getItemCount() - 1) ? 0 : ++selected);
		});

		playQueueActionMap.put("Repeat", onRepeat);

		cmbRepeatMode.addActionListener(actEvent -> controller.onRepeatModeSwitch((RepeatMode) cmbRepeatMode.getSelectedItem()));
		cmbShuffleMode.addActionListener(actEvent -> controller.onShuffleModeSwitch((ShuffleMode) cmbShuffleMode.getSelectedItem()));

		tfSearch.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void removeUpdate(DocumentEvent e) {
				applySearch();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				applySearch();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {}
		});

		cbSearchFullPhrase.addItemListener(e -> applySearch());

		btnSearchNextMatch.addActionListener(actEvent -> onSearchNextMatch());
		btnSearchPreviousMatch.addActionListener(actEvent -> onSearchPreviousMatch());
		btnSearchClear.addActionListener(actEvent -> tfSearch.setText(""));

		InputMap searchInputMap = tfSearch.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap searchActionMap = tfSearch.getActionMap();
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.META_DOWN_MASK), "Shuffle");
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "Shuffle");
		searchActionMap.put("Shuffle", onShuffle);

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.META_DOWN_MASK), "Repeat");
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "Repeat");
		searchActionMap.put("Repeat", onRepeat);

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Select");
		searchActionMap.put("Select", onSelect);

		tfSearch.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				int code = e.getKeyCode();
				switch (code) {
					case KeyEvent.VK_UP: {
						onSearchPreviousMatch();
						break;
					}

					case KeyEvent.VK_DOWN: {
						onSearchNextMatch();
						break;
					}
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {}
		});

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.META_DOWN_MASK), "PlayPause");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "PlayPause");

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.META_DOWN_MASK), "PlayPause");
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "PlayPause");
		searchActionMap.put("PlayPause", AbstractActionAdaptor.of(controller::onPlayPause));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.META_DOWN_MASK), "NextTrack");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK), "NextTrack");
		playQueueActionMap.put("NextTrack", AbstractActionAdaptor.of(controller::onNextTrack));

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.META_DOWN_MASK), "NextTrack");
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK), "NextTrack");
		searchActionMap.put("NextTrack", AbstractActionAdaptor.of(controller::onNextTrack));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.META_DOWN_MASK), "PreviousTrack");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK), "PreviousTrack");
		playQueueActionMap.put("PreviousTrack", AbstractActionAdaptor.of(controller::onPreviousTrack));

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.META_DOWN_MASK), "PreviousTrack");
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK), "PreviousTrack");
		searchActionMap.put("PreviousTrack", AbstractActionAdaptor.of(controller::onPreviousTrack));

		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.META_DOWN_MASK), "Stop");
		playQueueInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "Stop");
		playQueueActionMap.put("Stop", AbstractActionAdaptor.of(controller::onStop));

		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.META_DOWN_MASK), "Stop");
		searchInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "Stop");
		searchActionMap.put("Stop", AbstractActionAdaptor.of(controller::onStop));
	}

	public void allowSeek(int maxSliderValue) {
		this.playbackProgressBar.setMaximum(maxSliderValue);
	}

	public void disallowSeek() {
		this.playbackProgressBar.setValue(0);
		this.playbackProgressBar.setMaximum(0);
	}

	public void updateSeekSliderPosition(int sliderNewPosition) {
		playbackProgressBar.setValue(sliderNewPosition);
		playbackProgressBar.invalidate();
		playbackProgressBar.revalidate();
		playbackProgressBar.repaint();
	}

	public void updateStatus(String status) {
		this.lblStatus.setText(status);
		this.lblStatus.setToolTipText(status);
	}

	public void setPlayPauseButtonState(boolean playing) {
		btnPlayPause.setText(playing ? "||" : "->");
	}

	public void setCurrentPlayTimeDisplay(int playedSeconds, int totalSeconds) {
		int remainingSeconds = totalSeconds - playedSeconds;
		lblPlayTimeRemaining.setText(
				"-" + TimeDateUtil.prettyPrintFromSeconds(remainingSeconds) + " / " + TimeDateUtil.prettyPrintFromSeconds(totalSeconds));
		lblPlayTimeElapsed
				.setText(TimeDateUtil.prettyPrintFromSeconds(playedSeconds) + " / " + TimeDateUtil.prettyPrintFromSeconds(totalSeconds));
	}

	public void updateNowPlaying(PlaybackQueueEntry trackInfo) {
		String trackInfoText;
		String fileInfo;
		if (trackInfo != null) {
			if (trackInfo.isCueSheetTrack()) {
				fileInfo = "CUE track " + trackInfo.getTrackNumber() + " ("
						+ TimeDateUtil.prettyPrintFromSeconds(trackInfo.getCueSheetTrackStartTimeMillis() / 1000) + " - "
						+ TimeDateUtil.prettyPrintFromSeconds(trackInfo.getCueSheetTrackFinishTimeMillis() / 1000) + ")" + " of "
						+ trackInfo.getTargetFileFullPath();
			} else {
				fileInfo = trackInfo.getTargetFileFullPath();
			}
			trackInfoText = trackInfo.toDisplayStr();
		} else {
			fileInfo = "";
			trackInfoText = "";
		}
		this.lblNowPlayingTrack.setText(trackInfoText);
		this.lblNowPlayingTrack.setToolTipText(fileInfo);
	}

	public void scrollToTrack(int rowNumber) {
		this.tblPlayQueue.scrollRectToVisible(new Rectangle(tblPlayQueue.getCellRect(rowNumber, 0, true)));
	}

	public void selectTrack(int rowNumber) {
		this.tblPlayQueue.getSelectionModel().setSelectionInterval(rowNumber, rowNumber);
	}

	public void updateRepeatView(RepeatMode repeatMode) {
		this.cmbRepeatMode.setSelectedItem(repeatMode);
	}

	public void updateShuffleView(RepeatMode shuffleMode) {
		this.cmbShuffleMode.setSelectedItem(shuffleMode);
	}

	public void updateLastFMStatus(boolean ok, String statusText) {
		this.lastFMStatusIcon.setIcon(ok ? lastFMConnected : lastFMDisconnected);
		this.lastFMStatusIcon.setToolTipText(statusText);
	}

	public void setSearchMatchedRows(List<Integer> rows) {
		this.playbackQueueTableModel.setSearchMatchedRows(rows);
		this.lblSearchMatchesCount.setText(String.valueOf(rows.size()));
	}

	public void updatePlayQueueSizeLabel() {
		this.lblQueueSize.setText("" + playbackQueueTableModel.getRowCount());
	}

	public String getSearchText() {
		return tfSearch.getText();
	}

	public boolean isSearchFullPhrase() {
		return cbSearchFullPhrase.isSelected();
	}

	public void setShuffleMode(ShuffleMode shuffleMode) {
		this.cmbShuffleMode.setSelectedItem(shuffleMode);
	}

	public void setRepeatMode(RepeatMode repeatMode) {
		this.cmbRepeatMode.setSelectedItem(repeatMode);
	}

	public boolean isAutoStop() {
		return this.cbAutoStop.isSelected();
	}

	public void setAutoStop(boolean autoStop) {
		this.cbAutoStop.setSelected(autoStop);
	}

	public void setSelectedPlayQueueRows(int start, int end) {
		tblPlayQueue.getSelectionModel().setSelectionInterval(start, end);
	}

	public int[] getPlayQueueTableColumnWidths() {
		TableColumnModel columnModel = tblPlayQueue.getColumnModel();
		int[] columnWidths = new int[columnModel.getColumnCount()];
		int totalWidth = 0;
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			int width = columnModel.getColumn(i).getWidth();
			totalWidth += width;
			columnWidths[i] = width;
		}
		for (int i = 0; i < columnWidths.length; i++) {
			columnWidths[i] = (columnWidths[i] * 10000) / totalWidth;
		}
		return columnWidths;
	}

	public int[] getPlayQueueTableColumnPositions() {
		TableColumnModel columnModel = tblPlayQueue.getColumnModel();
		int[] columnPositions = new int[columnModel.getColumnCount()];
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			columnPositions[i] = tblPlayQueue.convertColumnIndexToView(i);
		}
		return columnPositions;
	}

	public void setPlayQueueTableColumnPositions(int[] playQueueColumnPositions) {
		TableColumnModel columnModel = tblPlayQueue.getColumnModel();

		for (int i = 0; i < columnModel.getColumnCount() && i < playQueueColumnPositions.length; i++) {
			columnModel.moveColumn(tblPlayQueue.convertColumnIndexToView(i), playQueueColumnPositions[i]);
		}
	}

	public void setPlayQueueTableColumnWidths(int[] playQueueColumnWidths) {
		TableColumnModel columnModel = tblPlayQueue.getColumnModel();

		int totalWidth = 0;
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			totalWidth += columnModel.getColumn(i).getWidth();
		}

		for (int i = 0; i < columnModel.getColumnCount() && i < playQueueColumnWidths.length; i++) {
			long width10k = playQueueColumnWidths[i] * totalWidth;
			columnModel.getColumn(i).setPreferredWidth((int) (width10k / 10000));
		}
	}

	public void applySearch() {
		String text = getSearchText();
		boolean fullPhrase = isSearchFullPhrase();
		new Thread(() -> {
			if (text == null || text.trim().isEmpty()) {
				searchMatches = Collections.emptyList();
			} else {
				searchMatches = playbackQueueTableModel.search(text, fullPhrase);
			}
			currentSearchMatch = -1;
			SwingUtil.runOnEDT(() -> {
				setSearchMatchedRows(searchMatches);
				onSearchNextMatch();
			}, false);
		}).start();
	}

	public void onSearchNextMatch() {
		int matchesCount = searchMatches.size();
		if (matchesCount > 0) {
			currentSearchMatch++;
			if (currentSearchMatch >= matchesCount) {
				currentSearchMatch = 0;
			}
		}
		gotoSearchMatch();
	}

	public void onSearchPreviousMatch() {
		int matchesCount = searchMatches.size();
		if (matchesCount > 0) {
			currentSearchMatch--;
			if (currentSearchMatch < 0) {
				currentSearchMatch = matchesCount - 1;
			}
		}
		gotoSearchMatch();
	}

	private void gotoSearchMatch() {
		if (currentSearchMatch > -1 && currentSearchMatch < searchMatches.size()) {
			int row = searchMatches.get(currentSearchMatch);
			scrollToTrack(row);
			selectTrack(row);
		}
	}
}
