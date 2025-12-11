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
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.jaudiotagger.tag.images.Artwork;

import lombok.Getter;
import lombok.Setter;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.util.swing.ImageUtil;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.TimeDateUtil;

public class SonivmMainWindow extends JFrame {
	private static final String BTN_TEXT_PAUSE = " \u23F8 ";
	private static final String BTN_TEXT_PREVTRACK = " \u23EE ";
	private static final String BTN_TEXT_NEXTTRACK = " \u23ED ";
	private static final String BTN_TEXT_STOP = " \u23F9 ";
	private static final String BTN_TEXT_PLAY = " \u25B6 ";

	private static final long serialVersionUID = -3402450540379541023L;

	private final PlaybackQueueTableModel playbackQueueTableModel;
	private final JLabel lblStatus;
	private final JLabel lblNowPlayingTrack;
	private final JLabel lblPlayTimeElapsed;
	private final JLabel lblPlayTimeRemaining;
	private final JTable tblPlayQueue;
	private final JTabbedPane tabsPlaylists;

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
	private final JLabel lblDropTarget;
	private final JScrollPane scrollTblPlayQueue;
	private final ArtworkDisplay artworkDisplay;

	private volatile List<Integer> searchMatches = Collections.emptyList();
	private volatile int currentSearchMatch = -1;
	@Getter
	@Setter
	private volatile boolean columnsMoved = false;
	@Getter
	@Setter
	private volatile boolean columnsResized = false;

	public SonivmMainWindow(String title, PlaybackQueueTableModel playbackQueueTableModel) {
		super(title);
		this.playbackQueueTableModel = playbackQueueTableModel;

		tblPlayQueue = new JTable(playbackQueueTableModel) {
			private static final long serialVersionUID = -6310314844571818281L;

			@Override
			public String getToolTipText(MouseEvent e) {
				Point point = e.getPoint();
				Object val = getValueAt(rowAtPoint(point), columnAtPoint(point));
				return val != null ? val.toString() : "";
			}
		};
		tblPlayQueue.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		tblPlayQueue.setCellSelectionEnabled(false);
		tblPlayQueue.setRowSelectionAllowed(true);
		tblPlayQueue.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 8392498039681170058L;

			Border emptyBorder = BorderFactory.createEmptyBorder(0, 4, 0, 4);
			Border highlightBorder = new CompoundBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, tblPlayQueue.getForeground()),
					BorderFactory.createEmptyBorder(0, 4, 0, 4));

			private final JLabel renderJLabel = new JLabel();
			{
				renderJLabel.setBorder(emptyBorder);
				renderJLabel.setOpaque(true);
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
					int column) {
				// Component result = super.getTableCellRendererComponent(table, value,
				// isSelected, hasFocus, row, column);
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
					int delta = -10;
					if (SwingUtil.getColorBrightness(bgRegular) < 30) {
						delta = 20;
					}
					bgRegular = SwingUtil.modifyColor(bgRegular, delta, delta, delta);
				}

				fgRegular = altColor(fgRegular, isHighlighted, 20, 20, 20);
				fgSelected = altColor(fgSelected, isHighlighted, 20, 20, 20);

				renderJLabel.setForeground(isSelected ? fgSelected : fgRegular);
				renderJLabel.setBackground(isSelected ? bgSelected : bgRegular);
				renderJLabel.setBorder(isHighlighted ? highlightBorder : emptyBorder);

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
		tblPlayQueue.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnAdded(TableColumnModelEvent e) {}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
				columnsMoved = true;
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
				columnsResized = true;
			}

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {}
		});
		tblPlayQueue.addMouseListener(new MouseAdapter() {
			private JPopupMenu popup = menu();

			public JPopupMenu menu() {
				JPopupMenu popup = new JPopupMenu();
				JMenuItem edit = new JMenuItem("Edit metadata");
				popup.add(edit);
				edit.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						SonivmMainWindow.this.showEditMetadataDialog();
					}
				});
				return popup;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON3) {
					Point p = e.getPoint();
					int row = tblPlayQueue.rowAtPoint(p);

					// If click is not on a valid row, you might want to ignore or clear selection:
					if (row == -1) {
						return;
					}

					// If that row isn't selected already, select it (useful for single selection or to
					// ensure Edit/Delete apply to the clicked row)
					if (!tblPlayQueue.isRowSelected(row)) {
						tblPlayQueue.getSelectionModel().setSelectionInterval(row, row);
					}

					// Optionally select the column too:
					// if (!table.isColumnSelected(column)) table.getColumnModel().getSelectionModel().setSelectionInterval(column, column);

					popup.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});

		// DefaultTableCellRenderer rightRendererForDuration = new
		// DefaultTableCellRenderer();
		// rightRendererForDuration.setHorizontalAlignment(JLabel.RIGHT);
		// tblPlayQueue.getColumnModel().getColumn(5).setCellRenderer(rightRendererForDuration);
		// DefaultTableCellRenderer rightRendererForDate = new
		// DefaultTableCellRenderer();
		// rightRendererForDate.setHorizontalAlignment(JLabel.RIGHT);
		// tblPlayQueue.getColumnModel().getColumn(6).setCellRenderer(rightRendererForDate);

		btnPlayPause = new JButton(BTN_TEXT_PLAY);
		btnStop = new JButton(BTN_TEXT_STOP);
		btnNextTrack = new JButton(BTN_TEXT_NEXTTRACK);
		btnPreviousTrack = new JButton(BTN_TEXT_PREVTRACK);
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
		cbSearchFullPhrase = new JCheckBox("full phrase");

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

		artworkDisplay = new ArtworkDisplay();
		artworkDisplay.setPreferredSize(new Dimension(0, tfSearch.getMinimumSize().height * 2));
		topPanel = SwingUtil.panel(BorderLayout::new).addWest(artworkDisplay).addCenter(topPanel).build();

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

		lblDropTarget = new JLabel("add to queue", JLabel.CENTER);
		lblDropTarget.setToolTipText("Drag&drop here to add/move to the end of the playback queue");
		lblDropTarget.setBorder(BorderFactory.createEtchedBorder());

		tabsPlaylists = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		scrollTblPlayQueue = new JScrollPane(tblPlayQueue);
		tblPlayQueue.setDragEnabled(true);
		tblPlayQueue.setDropMode(DropMode.USE_SELECTION);

		MouseListener onNowPlayingDoubleClick = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					int nowPlayingQueuePos = playbackQueueTableModel.getCurrentQueuePosition();
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
						.add(tabsPlaylists, BorderLayout.NORTH)
						.add(scrollTblPlayQueue, BorderLayout.CENTER)
						.add(lblDropTarget, BorderLayout.SOUTH)
						.build(), BorderLayout.CENTER);
		this.getContentPane().add(volumeSlider, BorderLayout.EAST);
		this.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

		this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
	}

	public void registerHandler(SonivmUIController controller) {
		tabsPlaylists.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int idx = tabsPlaylists.getSelectedIndex();
				playbackQueueTableModel.switchQueue(idx);
				updatePlayQueueSizeLabel();
			}
		});		
		AtomicInteger clickedTab = new AtomicInteger();
		SwingUtil.addPopupMenu(tabsPlaylists, e -> clickedTab.set(tabsPlaylists.indexAtLocation(e.getX(), e.getY())),
				new JMenuItem(new AbstractActionAdaptor("Close", e -> {
					int index = clickedTab.get();
					if (index >= 1) {
						if (JOptionPane.showConfirmDialog(tabsPlaylists,
								"Are you sure you want to close " + tabsPlaylists.getTitleAt(index) + "?", "Are you sure?",
								JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
							tabsPlaylists.removeTabAt(index);
							this.playbackQueueTableModel.deleteQueue(index);
							controller.onQueueRemove();
						}
					}
				})), new JMenuItem(new AbstractActionAdaptor("Add", e -> {
					String name = JOptionPane.showInputDialog(tabsPlaylists, "Enter name");
					if (name != null && !name.trim().isEmpty()) {
						this.playbackQueueTableModel.addQueue(name);
						controller.onQueueAdd();
						tabsPlaylists.addTab(name, new JLabel());
					}
				})));

		tblPlayQueue.setTransferHandler(new PlayQueueTableDnDTransferHandler(tblPlayQueue, controller));

		scrollTblPlayQueue.setDropTarget(new PlaybackQueueDropTarget(controller, tblPlayQueue));
		tabsPlaylists.setDropTarget(new PlaybackQueueDropTarget(controller, tblPlayQueue));

		// this.addWindowListener(new WindowAdapter() {
		// @Override
		// public void windowClosing(WindowEvent wndEvent) {
		// controller.onQuit();
		// }
		// });

		SwingUtil.addValueChangeByUserListener(volumeSlider, event -> controller.onVolumeChange(volumeSlider.getValue()));
		SwingUtil.makeJProgressBarMoveToClickPosition(playbackProgressBar, val -> controller.onSeek(val));

		btnPlayPause.addActionListener(event -> controller.onPlayPause());
		btnStop.addActionListener(event -> controller.onStop());
		btnNextTrack.addActionListener(event -> controller.onNextTrack());
		btnPreviousTrack.addActionListener(event -> controller.onPreviousTrack());

		btnToggleShowEq.addActionListener(actEvent -> controller.toggleShowEqualizer());
		btnToggleShowEq.setFocusable(false);

		tblPlayQueue.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				if (mouseEvent.getClickCount() == 2 && tblPlayQueue.getSelectedRow() != -1
						&& mouseEvent.getButton() == MouseEvent.BUTTON1) {
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
		cbAutoStop.addChangeListener(e -> controller.onAutoStopChange(cbAutoStop.isSelected()));

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

	public void setStatusDisplay(String status) {
		this.lblStatus.setText(status);
		this.lblStatus.setToolTipText(status);
	}

	public void setPlayPauseButtonState(boolean playing) {
		btnPlayPause.setText(playing ? BTN_TEXT_PAUSE : BTN_TEXT_PLAY);
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

		int idx = this.playbackQueueTableModel.getCurrentPlayQueue();
		this.tabsPlaylists.setSelectedIndex(idx); // redundant?
		if (trackInfo == null) {
			idx = -1;
		}
		for (int i = 0; i < this.tabsPlaylists.getTabCount(); i++) {
			String title = this.tabsPlaylists.getTitleAt(i);
			if (title.startsWith("\uD83D\uDD0A ")) {
				title = title.substring(3);
			}
			this.tabsPlaylists.setTitleAt(i, idx == i ? ("\uD83D\uDD0A " + title) : title);
		}
	}

	public void scrollToTrack(int rowNumber) {
		if (rowNumber > -1) {
			this.tabsPlaylists.setSelectedIndex(this.playbackQueueTableModel.getCurrentPlayQueue());
			this.tblPlayQueue.scrollRectToVisible(new Rectangle(tblPlayQueue.getCellRect(rowNumber, 0, true)));
		}
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
		this.tblPlayQueue.repaint();
		this.lblSearchMatchesCount.setText(String.valueOf(rows.size()));
	}

	public void updatePlayQueueSizeLabel() {
		long totalSeconds = playbackQueueTableModel.getQueueLength();
		String queueLength = String.format("%02d:%02d:%02d", totalSeconds / 3600, (totalSeconds % 3600) / 60, totalSeconds % 60);
		this.lblQueueSize.setText("" + playbackQueueTableModel.getRowCount() + ". Length: " + queueLength);
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

	public void setAutoStop(boolean autoStop) {
		this.cbAutoStop.setSelected(autoStop);
	}

	public void setSelectedPlayQueueRows(int start, int end) {
		tblPlayQueue.getSelectionModel().setSelectionInterval(start, end);
	}

	public int[] getPlayQueueTableColumnPositions() {
		return SwingUtil.getJTableColumnPositions(tblPlayQueue);
	}

	public void setPlayQueueTableColumnPositions(int[] playQueueColumnPositions) {
		SwingUtil.applyJTableColumnPositions(tblPlayQueue, playQueueColumnPositions);
	}

	public int[] getPlayQueueTableColumnWidths() {
		return SwingUtil.getJTableColumnWidths(tblPlayQueue);
	}

	public void setPlayQueueTableColumnWidths(int[] playQueueColumnWidths) {
		SwingUtil.applyJTableColumnWidths(tblPlayQueue, playQueueColumnWidths);
	}

	public void applySearch() {
		String text = getSearchText();
		boolean fullPhrase = isSearchFullPhrase();
		new Thread(() -> {
			if (text == null || text.trim().isEmpty()) {
				searchMatches = Collections.emptyList();
				playbackQueueTableModel.setSearchMatchedRows(searchMatches);
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

	public void setVolumeSliderPosition(int percent) {
		this.volumeSlider.setValue(percent);
	}

	public void setArtwork(Artwork artwork) {
		this.artworkDisplay.set(artwork);
		this.artworkDisplay.repaint();
	}

	private void showEditMetadataDialog() {
		int[] selectedRows = this.tblPlayQueue.getSelectedRows();
		if (selectedRows.length > 0) {
			EditMetadataDialog dialog = new EditMetadataDialog(this, playbackQueueTableModel, selectedRows);
			dialog.setVisible(true);
		}
	}

	public void reloadQueueTabNames() {
		tabsPlaylists.removeAll();
		for (int i = 0; i < this.playbackQueueTableModel.getQueuesCount(); i++) {
			tabsPlaylists.addTab(this.playbackQueueTableModel.getQueueName(i), new JLabel());
		}
	}
}
