package x.mvmn.sonivm.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.PlaybackListener;
import x.mvmn.sonivm.WinAmpSkinsService;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.eq.EqualizerPresetService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;
import x.mvmn.sonivm.lastfm.LastFMScrobblingService;
import x.mvmn.sonivm.playqueue.PlaybackQueueChangeListener;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.retro.RetroUIEqualizerWindow;
import x.mvmn.sonivm.ui.retro.RetroUIFactory;
import x.mvmn.sonivm.ui.retro.RetroUIMainWindow;
import x.mvmn.sonivm.ui.retro.RetroUIPlaylistWindow;
import x.mvmn.sonivm.ui.retro.exception.WSZLoadingException;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.Tuple2;
import x.mvmn.sonivm.util.Tuple3;
import x.mvmn.sonivm.util.Tuple4;

//@RequiredArgsConstructor
public class SonivmUI implements SonivmUIController, Consumer<Tuple2<Boolean, String>>, PlaybackListener {

	private static final Logger LOGGER = Logger.getLogger(SonivmUI.class.getSimpleName());

	protected final String EMBEDDED_SKIN_NAME = "<Sonivm>";

	protected final SonivmMainWindow mainWindow;
	protected final EqualizerWindow eqWindow;

	protected final BufferedImage sonivmIcon;
	protected final SonivmTrayIconPopupMenu trayIconPopupMenu;
	protected final PreferencesService preferencesService;
	protected final PlaybackController playbackController;
	protected final LastFMScrobblingService lastFMScrobblingService;
	protected final PlaybackQueueService playbackQueueService;
	protected final WinAmpSkinsService winAmpSkinsService;
	protected final PlaybackQueueTableModel playQueueTableModel;
	protected final AbstractTableModel retroUIPlayQueueTableModel;
	protected final SonivmEqualizerService eqService;
	protected final EqualizerPresetService eqPresetService;

	protected final RetroUIFactory retroUIFactory = new RetroUIFactory(); // TODO: inject

	protected volatile TrayIcon sonivmTrayIcon;
	protected volatile Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> retroUIWindows;
	protected volatile boolean retroUIShowRemainingTime = false;
	private volatile List<Integer> retroUISearchMatchedRows = Collections.emptyList();
	private volatile int currentRetroUISearchMatch = -1;

	private volatile JFrame skinBrowser;

	public SonivmUI(SonivmMainWindow mainWindow,
			EqualizerWindow eqWindow,
			BufferedImage sonivmIcon,
			SonivmTrayIconPopupMenu trayIconPopupMenu,
			PreferencesService preferencesService,
			PlaybackController sonivmController,
			LastFMScrobblingService lastFMScrobblingService,
			PlaybackQueueService playbackQueueService,
			WinAmpSkinsService winAmpSkinsService,
			SonivmEqualizerService eqService,
			EqualizerPresetService eqPresetService,
			PlaybackQueueTableModel playQueueTableModel) {
		super();
		this.mainWindow = mainWindow;
		this.eqWindow = eqWindow;
		this.sonivmIcon = sonivmIcon;
		this.trayIconPopupMenu = trayIconPopupMenu;
		this.preferencesService = preferencesService;
		this.playbackController = sonivmController;
		this.lastFMScrobblingService = lastFMScrobblingService;
		this.playbackQueueService = playbackQueueService;
		this.winAmpSkinsService = winAmpSkinsService;
		this.eqService = eqService;
		this.eqPresetService = eqPresetService;
		this.playQueueTableModel = playQueueTableModel;
		this.retroUIPlayQueueTableModel = new AbstractTableModel() {
			private static final long serialVersionUID = 5628564877472125514L;

			private final String[] COLUMN_NAMES = { "#", "N", "Track", "Length" };

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
						if (retroUIWindows != null && retroUIWindows.getC()
								.getPlaylistTable()
								.getColumnModel()
								.getColumn(2)
								.getWidth() > (int) retroUIWindows.getC()
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
		};
	}

	public void showMainWindow() {
		SwingUtil.showAndBringToFront(mainWindow);
	}

	public void showEqWindow() {
		SwingUtil.showAndBringToFront(eqWindow);
	}

	public void toggleEqWindow() {
		if (!eqWindow.isVisible()) {
			SwingUtil.showAndBringToFront(eqWindow);
		} else {
			eqWindow.setVisible(false);
		}
	}

	protected void updateSkinsList() {
		List<String> skins = Stream.concat(Stream.of(EMBEDDED_SKIN_NAME), winAmpSkinsService.listSkins().stream())
				.collect(Collectors.toList());
		SwingUtil.runOnEDT(() -> trayIconPopupMenu.setSkinsList(skins), false);
	}

	@PostConstruct
	public void init() {
		trayIconPopupMenu.registerHandler(this);
		updateSkinsList();
		SwingUtil.registerQuitHandler(this::onQuit);

		playbackController.restorePlaybackState();
		SwingUtil.runOnEDT(() -> {
			mainWindow.setShuffleMode(playbackController.getShuffleMode());
			mainWindow.setRepeatMode(playbackController.getRepeatMode());
			mainWindow.setAutoStop(playbackController.isAutoStop());
			mainWindow.setVolumeSliderPosition(playbackController.getCurrentVolumePercentage());
		}, false);

		SwingUtil.runOnEDT(() -> {
			try {
				TrayIcon trayIcon = new TrayIcon(sonivmIcon);
				trayIcon.setImageAutoSize(true);
				trayIcon.setPopupMenu(trayIconPopupMenu.getUIComponent());

				sonivmTrayIcon = trayIcon;
				SystemTray.getSystemTray().add(trayIcon);
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "Failed to add system tray icon", t);
			}
		}, false);

		playbackQueueService.addChangeListener(new PlaybackQueueChangeListener() {

			@Override
			public void onTableRowsUpdate(int firstRow, int lastRow, boolean waitForUiUpdate) {}

			@Override
			public void onTableRowsInsert(int firstRow, int lastRow, boolean waitForUiUpdate) {
				SwingUtil.runOnEDT(() -> mainWindow.updatePlayQueueSizeLabel(), false);
				mainWindow.applySearch();
				if (SonivmUI.this.retroUIWindows != null) {
					SonivmUI.this.onRetroUISearchTextChange();
				}
			}

			@Override
			public void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate) {
				SwingUtil.runOnEDT(() -> mainWindow.updatePlayQueueSizeLabel(), false);
				mainWindow.applySearch();
				if (SonivmUI.this.retroUIWindows != null) {
					SonivmUI.this.onRetroUISearchTextChange();
				}
			}
		});
		playbackQueueService.addChangeListener(new PlaybackQueueChangeListener() {

			protected AbstractTableModel getTableModel() {
				return (AbstractTableModel) retroUIWindows.getC().getPlaylistTable().getModel();
			}

			@Override
			public void onTableRowsUpdate(int firstRow, int lastRow, boolean waitForUiUpdate) {
				if (retroUIWindows != null) {
					SwingUtil.runOnEDT(() -> getTableModel().fireTableRowsUpdated(firstRow, lastRow), waitForUiUpdate);
				}
			}

			@Override
			public void onTableRowsInsert(int firstRow, int lastRow, boolean waitForUiUpdate) {
				if (retroUIWindows != null) {
					SwingUtil.runOnEDT(() -> getTableModel().fireTableRowsInserted(firstRow, lastRow), waitForUiUpdate);
				}
			}

			@Override
			public void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate) {
				if (retroUIWindows != null) {
					SwingUtil.runOnEDT(() -> getTableModel().fireTableRowsDeleted(firstRow, lastRow), waitForUiUpdate);
				}
			}
		});

		eqWindow.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				eqWindow.setState(eqService.getCurrentState());
			}
		});

		eqWindow.registerHandler(this);
		mainWindow.registerHandler(this);
		playbackController.addPlaybackListener(this);
		lastFMScrobblingService.addStatusListener(this);

		restoreMainWindowPlaybackState();
	}

	protected void initRetroUI() {
		String retroUISkin = preferencesService.getRetroUISkin();
		File retroUISkinFile = retroUISkin != null ? winAmpSkinsService.getSkinFile(retroUISkin) : null;
		try {
			retroUIWindows = retroUIFactory.construct(retroUISkinFile, getRetroUIPlaylistTable());
			setIconImageOnRetroUIWindows();
		} catch (WSZLoadingException e) {
			LOGGER.log(Level.SEVERE, "Failed to cosntruct UI from WinAmp skin: " + retroUISkin, e);
		}
	}

	protected void setIconImageOnRetroUIWindows() {
		if (retroUIWindows != null) {
			retroUIWindows.getA().getWindow().setIconImage(sonivmIcon);
			retroUIWindows.getB().getWindow().setIconImage(sonivmIcon);
			retroUIWindows.getC().getWindow().setIconImage(sonivmIcon);
		}
	}

	protected JTable getRetroUIPlaylistTable() {
		JTable tblPlayQueue = new JTable(retroUIPlayQueueTableModel) {
			private static final long serialVersionUID = 4233667431815675829L;

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
		tblPlayQueue.setDragEnabled(true);
		tblPlayQueue.setDropMode(DropMode.USE_SELECTION);
		tblPlayQueue.setTransferHandler(new PlayQueueTableDnDTransferHandler(tblPlayQueue, this));

		tblPlayQueue.setDefaultRenderer(String.class, new DefaultTableCellRenderer() {
			private static final long serialVersionUID = 8456077630254148604L;
			private final JLabel renderJLabel = new JLabel();
			{
				renderJLabel.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
				renderJLabel.setOpaque(true);
			}

			@Override
			public boolean isOpaque() {
				return true;
			}

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
					int column) {
				// Component result = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				renderJLabel.setText(value != null ? value.toString() : "");

				boolean isHighlighted = row == playbackController.getTrackQueuePosition();
				renderJLabel.setFont(isHighlighted ? tblPlayQueue.getFont().deriveFont(Font.BOLD) : tblPlayQueue.getFont());

				int originalColumnIndex = table.convertColumnIndexToModel(column);
				renderJLabel.setHorizontalAlignment(originalColumnIndex == 2 ? JLabel.LEFT : JLabel.RIGHT);

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
					fgSelected = table.getSelectionForeground();
					bgSelected = table.getSelectionBackground();
				}

				// if (hasFocus) {
				// isSelected = true;
				//
				// fgSelected = lookupInUIDefaults("Table.focusCellForeground", fgSelected);
				// bgSelected = lookupInUIDefaults("Table.focusCellBackground", bgSelected);
				// }

				fgRegular = table.getForeground();
				fgSelected = table.getSelectionForeground();

				if (isHighlighted) {
					fgRegular = retroUIWindows.getC().getPlaylistColors().getCurrentTrackTextColor();
					fgSelected = fgRegular;
				}
				if (retroUISearchMatchedRows.contains(row)) {
					bgRegular = SwingUtil.getColorBrightness(bgRegular) < 300 ? SwingUtil.modifyColor(bgRegular, -10, 50, 10)
							: SwingUtil.modifyColor(bgRegular, -50, 10, -10);
				} else if (row % 2 == 0) {
					int delta = -10;
					if (SwingUtil.getColorBrightness(bgRegular) < 30) {
						delta = 20;
					}
					bgRegular = SwingUtil.modifyColor(bgRegular, delta, delta, delta);
				}

				renderJLabel.setForeground(isSelected ? fgSelected : fgRegular);
				renderJLabel.setBackground(isSelected ? bgSelected : bgRegular);

				// super.setForeground(isSelected ? fgSelected : fgRegular);
				// super.setBackground(isSelected ? bgSelected : bgRegular);

				return renderJLabel;
			}
		});

		return tblPlayQueue;
	}

	protected void restoreRetroUIWindowsState() {
		SwingUtil.runOnEDT(() -> {
			try {
				SwingUtil.restoreWindowState(retroUIWindows.getA().getWindow(), preferencesService.getRetroUIMainWindowState());
				SwingUtil.restoreWindowState(retroUIWindows.getB().getWindow(), preferencesService.getRetroUIEQWindowState());
				retroUIWindows.getC().getWindow().setScaleFactor(retroUIWindows.getA().getWindow().getScaleFactor());
				retroUIWindows.getC()
						.getWindow()
						.setSizeExtensions(preferencesService.getRetroUIPlaylistSizeExtX(),
								preferencesService.getRetroUIPlaylistSizeExtY());
				SwingUtil.restoreWindowState(retroUIWindows.getC().getWindow(), preferencesService.getRetroUIPlaylistWindowState());

				retroUIWindows.getC().setPlayQueueTableColumnPositions(preferencesService.getRetroUIPlayQueueColumnPositions());
				retroUIWindows.getC().setPlayQueueTableColumnWidths(preferencesService.getRetroUIPlayQueueColumnWidths());
				retroUIWindows.getC().getPlaylistTable().repaint();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Failed to restore Retro UI window states", e);
			}
		}, false);
	}

	protected void savePlayQueueColumnsState() {
		LOGGER.info("Saving UI state.");
		try {
			preferencesService.setPlayQueueColumnWidths(mainWindow.getPlayQueueTableColumnWidths());
			preferencesService.setPlayQueueColumnPositions(mainWindow.getPlayQueueTableColumnPositions());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store column width for playback queue table", e);
		}
	}

	protected void saveUIState() {
		LOGGER.info("Quit requested - shutting down");

		try {
			LOGGER.info("Storing window positions/sizes/visibility");
			this.preferencesService.saveMainWindowState(SwingUtil.getWindowState(mainWindow));
			this.preferencesService.saveEQWindowState(SwingUtil.getWindowState(eqWindow));
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to store main and EQ window states", t);
		}
	}

	protected void saveRetroUIState() {
		if (retroUIWindows != null) {
			this.preferencesService.saveRetroUIMainWindowState(SwingUtil.getWindowState(retroUIWindows.getA().getWindow()));
			this.preferencesService.saveRetroUIEqWindowState(SwingUtil.getWindowState(retroUIWindows.getB().getWindow()));
			this.preferencesService.saveRetroUIPlaylistWindowState(SwingUtil.getWindowState(retroUIWindows.getC().getWindow()));
			this.preferencesService.setRetroUIPlaylistSizeExtX(retroUIWindows.getC().getWindow().getWidthExtension());
			this.preferencesService.setRetroUIPlaylistSizeExtY(retroUIWindows.getC().getWindow().getHeightExtension());
			this.preferencesService.setRetroUIPlayQueueColumnWidths(retroUIWindows.getC().getPlayQueueTableColumnWidths());
			this.preferencesService.setRetroUIPlayQueueColumnPositions(retroUIWindows.getC().getPlayQueueTableColumnPositions());
		}
	}

	protected void destroyUI() {
		SwingUtil.runOnEDT(() -> {
			try {
				SystemTray.getSystemTray().remove(sonivmTrayIcon);
				LOGGER.info("Removed tray icon");
			} catch (Throwable t) {
				LOGGER.log(Level.SEVERE, "Failed to remove tray icon", t);
			}
			mainWindow.setVisible(false);
			mainWindow.dispose();
			LOGGER.info("Hid and disposed main window");
			eqWindow.setVisible(false);
			eqWindow.dispose();
			LOGGER.info("Hid and disposed equalizer window");
		}, true);
	}

	protected void destroyRetroUIWindows() {
		if (retroUIWindows != null) {
			retroUIWindows.getA().getWindow().setVisible(false);
			retroUIWindows.getA().getWindow().dispose();
			retroUIWindows.getB().getWindow().setVisible(false);
			retroUIWindows.getB().getWindow().dispose();
			retroUIWindows.getC().getWindow().setVisible(false);
			retroUIWindows.getC().getWindow().dispose();
			LOGGER.info("Hid and disposed retroUI windows");
		}
	}

	protected void restoreMainWindowPlaybackState() {
		mainWindow.setShuffleMode(playbackController.getShuffleMode());
		mainWindow.setRepeatMode(playbackController.getRepeatMode());
		mainWindow.setAutoStop(playbackController.isAutoStop());
		mainWindow.setVolumeSliderPosition(playbackController.getCurrentVolumePercentage());
	}

	public void onQuit() {
		playbackController.onStop();
		saveUIState();
		saveRetroUIState();
		savePlayQueueColumnsState();
		destroyUI();
		destroyRetroUIWindows();
		playbackController.onQuit();
		lastFMScrobblingService.shutdown();
	}

	public void show() {
		LOGGER.info("Restoring window positions/sizes/visibility");
		try {
			Tuple4<Boolean, String, Point, Dimension> mainWindowState = preferencesService.getMainWindowState();
			SwingUtil.runOnEDT(() -> {
				applyWindowState(mainWindow, mainWindowState, true);
				restorePlayQueueColumnsState();
			}, true);
			Tuple4<Boolean, String, Point, Dimension> eqWindowState = preferencesService.getEQWindowState();
			SwingUtil.runOnEDT(() -> applyWindowState(eqWindow, eqWindowState, false), true);
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to restore window states", t);
		}

		initRetroUI();
		restoreRetroUIWindowsState();
		retroUIRegHandlerAndUpdateState();
	}

	public static void applyWindowState(Window window, Tuple4<Boolean, String, Point, Dimension> windowState, boolean visibleByDefault) {
		if (windowState != null) {
			SwingUtil.restoreWindowState(window, windowState);
		} else {
			window.pack();
			SwingUtil.moveToScreenCenter(window);
			window.setVisible(visibleByDefault);
		}
	}

	private void restorePlayQueueColumnsState() {
		try {
			int[] playQueueColumnPositions = preferencesService.getPlayQueueColumnPositions();
			if (playQueueColumnPositions != null && playQueueColumnPositions.length > 0) {
				SwingUtil.runOnEDT(() -> mainWindow.setPlayQueueTableColumnPositions(playQueueColumnPositions), true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column positions for playback queue table", e);
		}

		try {
			int[] playQueueColumnWidths = preferencesService.getPlayQueueColumnWidths();
			if (playQueueColumnWidths != null && playQueueColumnWidths.length > 0) {
				SwingUtil.runOnEDT(() -> mainWindow.setPlayQueueTableColumnWidths(playQueueColumnWidths), true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column width for playback queue table", e);
		}
	}

	@Override
	public void onShowMainWindow() {
		this.showMainWindow();
	}

	@Override
	public void onShowEQWindow() {
		this.showEqWindow();
	}

	@Override
	public void onPlayPause() {
		playbackController.onPlayPause();
	}

	@Override
	public void onStop() {
		playbackController.onStop();
	}

	@Override
	public void onPreviousTrack() {
		playbackController.onPreviousTrack();
	}

	@Override
	public void onNextTrack() {
		playbackController.onNextTrack();
	}

	@Override
	public boolean onDropQueueRowsInsideQueue(int queuePosition, int firstRow, int lastRow) {
		boolean result = playbackController.onDropQueueRowsInsideQueue(queuePosition, firstRow, lastRow);
		if (result) {
			SwingUtil.runOnEDT(() -> {
				mainWindow.setSelectedPlayQueueRows(queuePosition, queuePosition + (lastRow - firstRow));
				if (retroUIWindows != null) {
					retroUIWindows.getC()
							.getPlaylistTable()
							.getSelectionModel()
							.setSelectionInterval(queuePosition, queuePosition + (lastRow - firstRow));
				}
			}, false);
		}
		return result;
	}

	@Override
	public void onDropFilesToQueue(int row, List<File> fileList) {
		playbackController.onDropFilesToQueue(row, fileList,
				importedTrack -> SwingUtil.runOnEDT(() -> mainWindow.setStatusDisplay("Loaded into queue: " + importedTrack), false));
	}

	@Override
	public void onAutoStopChange(boolean selected) {
		playbackController.onAutoStopChange(selected);
	}

	@Override
	public void onShuffleModeSwitch(ShuffleMode selectedItem) {
		mainWindow.setShuffleMode(selectedItem);
		if (retroUIWindows != null) {
			retroUIWindows.getA().setShuffleToggleState(selectedItem != ShuffleMode.OFF);
		}
		playbackController.onShuffleModeSwitch(selectedItem);
	}

	@Override
	public void onRepeatModeSwitch(RepeatMode selectedItem) {
		mainWindow.setRepeatMode(selectedItem);
		if (retroUIWindows != null) {
			retroUIWindows.getA().setRepeatToggleState(selectedItem != RepeatMode.OFF);
		}
		playbackController.onRepeatModeSwitch(selectedItem);
	}

	@Override
	public void onVolumeChange(int value) {
		playbackController.onVolumeChange(value);
		mainWindow.setVolumeSliderPosition(value);
		if (retroUIWindows != null) {
			retroUIWindows.getA().setVolumeSliderPos(value);
		}
	}

	@Override
	public void onSeek(double ratio) {
		this.onSeek((int) Math.round(ratio * playbackController.getCurrentTrackLengthSeconds() * 10));
	}

	@Override
	public void onSeek(int tenthOfSeconds) {
		playbackController.onSeek(tenthOfSeconds);
	}

	@Override
	public void toggleShowEqualizer() {
		SwingUtil.runOnEDT(() -> this.toggleEqWindow(), false);
	}

	@Override
	public void onTrackSelect(int row) {
		playbackController.onTrackSelect(row);
	}

	@Override
	public void onDeleteRowsFromQueue(int start, int end) {
		playbackController.onDeleteRowsFromQueue(start, end);
	}

	@Override
	public void scrollToTrack(int trackQueuePosition) {
		SwingUtil.runOnEDT(() -> {
			mainWindow.scrollToTrack(trackQueuePosition);
			if (retroUIWindows != null) {
				retroUIWindows.getC().scrollToTrack(trackQueuePosition);
			}
		}, false);
	}

	@Override
	public void onPlaybackStateChange(PlaybackState playbackState) {
		SwingUtil.runOnEDT(() -> {
			if (retroUIWindows != null) {
				retroUIWindows.getA().setPlybackIndicatorState(playbackState);
				retroUIWindows.getC().getPlaylistTable().repaint();
			}
			switch (playbackState) {
				case STOPPED:
					mainWindow.disallowSeek();
					mainWindow.setCurrentPlayTimeDisplay(0, 0);
					updateNowPlaying(null);
					mainWindow.setStatusDisplay("");
					if (retroUIWindows != null) {
						retroUIWindows.getA().setPlaybackNumbers(0, 0, false);
						retroUIWindows.getA().setSeekSliderEnabled(false);
					}
					mainWindow.setPlayPauseButtonState(false);
					trayIconPopupMenu.setPlayPauseButtonState(false);
				break;
				case PAUSED:
					mainWindow.setPlayPauseButtonState(false);
					trayIconPopupMenu.setPlayPauseButtonState(false);
				break;
				case PLAYING:
					mainWindow.setPlayPauseButtonState(true);
					trayIconPopupMenu.setPlayPauseButtonState(true);
				break;
				default:
					throw new IllegalArgumentException("Unsupported playback state " + playbackState);
			}
		}, false);
	}

	protected void updateNowPlaying(PlaybackQueueEntry nowPlaying) {
		mainWindow.updateNowPlaying(nowPlaying);
		trayIconPopupMenu.updateNowPlaying(nowPlaying);
		if (retroUIWindows != null) {
			if (nowPlaying != null) {
				retroUIWindows.getA().setNowPlayingText(nowPlaying.toDisplayStr().trim(), true);
			} else {
				retroUIWindows.getA().setNowPlayingText("Stopped", false);
			}
		}
	}

	@Override
	public void onPlaybackStart(AudioFileInfo audioInfo, PlaybackQueueEntry trackInfo) {
		SwingUtil.runOnEDT(() -> {
			mainWindow.updateSeekSliderPosition(0);
			int trackDurationSeconds = trackInfo.getDuration().intValue();
			if (audioInfo.isSeekable()) {
				mainWindow.allowSeek(trackDurationSeconds * 10);
			} else {
				mainWindow.disallowSeek();
			}
			updateNowPlaying(trackInfo);
			String audioInfoStr = audioInfo.getAudioFileFormat() != null
					? audioInfo.getAudioFileFormat().getFormat().toString().replaceAll(",\\s*$", "").replaceAll(",\\s*,", ",")
					: "";
			mainWindow.setStatusDisplay(audioInfoStr);
			scrollToTrack(playbackController.getTrackQueuePosition());

			if (retroUIWindows != null) {
				retroUIWindows.getA().setPlayTime(0, trackDurationSeconds, retroUIShowRemainingTime);
				if (audioInfo.isSeekable()) {
					retroUIWindows.getA().setSeekSliderEnabled(true);
					retroUIWindows.getA().setSeekSliderPosition(0.0d);
				} else {
					retroUIWindows.getA().setSeekSliderEnabled(false);
				}
			}
		}, false);
	}

	@Override
	public void onPlaybackProgress(long playTimeMillis, int totalDurationSeconds) {
		int playbackIndicatorPosition = (int) (playTimeMillis / 100);
		SwingUtil.runOnEDT(() -> {
			mainWindow.updateSeekSliderPosition(playbackIndicatorPosition);
			mainWindow.setCurrentPlayTimeDisplay(playbackIndicatorPosition / 10, totalDurationSeconds);
			if (retroUIWindows != null) {
				// retroUIWindows.getA().setSeekSliderEnabled(true);
				retroUIWindows.getA().advanceNowPlayingText(2);
				retroUIWindows.getA().setPlayTime(playbackIndicatorPosition / 10, totalDurationSeconds, retroUIShowRemainingTime);
				retroUIWindows.getA().setSeekSliderPosition((playTimeMillis / 1000.0d) / totalDurationSeconds);
			}
		}, false);
	}

	protected void updateLastFMStatus(Tuple2<Boolean, String> lastFMStatus) {
		SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(lastFMStatus.getA(), lastFMStatus.getB()), false);
	}

	@Override
	public void accept(Tuple2<Boolean, String> lastFMStatus) {
		updateLastFMStatus(lastFMStatus);
	}

	@Override
	public void onPlaybackError(String errorMessage) {
		SwingUtil.runOnEDT(() -> mainWindow.setStatusDisplay(errorMessage), false);
	}

	@Override
	public void onShowRetroUIMainWindow() {
		if (retroUIWindows != null) {
			SwingUtil.runOnEDT(() -> retroUIWindows.getA().getWindow().setVisible(true), false);
		}
	}

	@Override
	public void onShowRetroUIEQWindow() {
		if (retroUIWindows != null) {
			SwingUtil.runOnEDT(() -> {
				retroUIWindows.getB().getWindow().setVisible(true);
				retroUIWindows.getA().setEQToggleState(true);
			}, false);
		}
	}

	@Override
	public void onShowRetroUIPlaylistWindow() {
		if (retroUIWindows != null) {
			SwingUtil.runOnEDT(() -> {
				retroUIWindows.getC().getWindow().setVisible(true);
				retroUIWindows.getA().setPlaylistToggleState(true);
			}, false);
		}
	}

	@Override
	public void onRetroUiSkinChange(String skinFileName) {
		try {
			Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> newRetroUi;
			Point playlistScrollPosition = null;
			if (EMBEDDED_SKIN_NAME.equals(skinFileName)) {
				newRetroUi = retroUIFactory.construct(null, getRetroUIPlaylistTable());
			} else {
				newRetroUi = retroUIFactory.construct(winAmpSkinsService.getSkinFile(skinFileName), getRetroUIPlaylistTable());
			}
			setIconImageOnRetroUIWindows();
			if (this.retroUIWindows != null) {
				playlistScrollPosition = this.retroUIWindows.getC()
						.getWindow()
						.getWrappedComponentScrollPane()
						.getViewport()
						.getViewPosition();
				saveRetroUIState();
				destroyRetroUIWindows();
			}
			this.retroUIWindows = newRetroUi;
			restoreRetroUIWindowsState();
			retroUIRegHandlerAndUpdateState();
			if (playlistScrollPosition != null) {
				this.retroUIWindows.getC()
						.getWindow()
						.getWrappedComponentScrollPane()
						.getViewport()
						.setViewPosition(playlistScrollPosition);
			}
			preferencesService.setRetroUISkin(skinFileName);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to construct retroUI with skin " + (skinFileName != null ? skinFileName : "embedded"), e);
		}
	}

	protected void retroUIRegHandlerAndUpdateState() {
		this.retroUIWindows.getA().addListener(this);
		this.retroUIWindows.getB().addListener(this);
		this.retroUIWindows.getC().addListener(this);
		this.retroUIWindows.getC()
				.getWindow()
				.getWrappedComponentScrollPane()
				.setDropTarget(new PlaybackQueueDropTarget(this, this.retroUIWindows.getC().getPlaylistTable()));

		Map<KeyStroke, Runnable> playbackControlKeyActionsMap = new HashMap<>();
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), this::onPlayPause);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.META_DOWN_MASK), this::onPlayPause);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), this::onPlayPause);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.META_DOWN_MASK), this::onNextTrack);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, InputEvent.CTRL_DOWN_MASK), this::onNextTrack);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.META_DOWN_MASK),
				this::onPreviousTrack);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, InputEvent.CTRL_DOWN_MASK),
				this::onPreviousTrack);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.META_DOWN_MASK), this::onStop);
		playbackControlKeyActionsMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), this::onStop);

		this.retroUIWindows.getA().getWindow().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				Runnable action = playbackControlKeyActionsMap.get(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()));
				if (action != null) {
					action.run();
				}
			}
		});
		this.retroUIWindows.getB().getWindow().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				Runnable action = playbackControlKeyActionsMap.get(KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()));
				if (action != null) {
					action.run();
				}
			}
		});

		SwingUtil.runOnEDT(() -> {
			this.retroUIWindows.getB().setEQEnabled(eqService.getCurrentState().isEnabled());
			this.retroUIWindows.getB().setPreset(eqService.getCurrentState());
			this.retroUIWindows.getA().setSeekSliderEnabled(playbackController.getCurrentPlaybackState() != PlaybackState.STOPPED);
			this.retroUIWindows.getA().setEQToggleState(this.retroUIWindows.getB().getWindow().isVisible());
			this.retroUIWindows.getA().setPlaylistToggleState(this.retroUIWindows.getC().getWindow().isVisible());
			this.retroUIWindows.getA().setShuffleToggleState(playbackController.getShuffleMode() != ShuffleMode.OFF);
			this.retroUIWindows.getA().setRepeatToggleState(playbackController.getRepeatMode() != RepeatMode.OFF);
			this.retroUIWindows.getA().setVolumeSliderPos(playbackController.getCurrentVolumePercentage());
			int balance = playbackController.getBalance();
			this.retroUIWindows.getA().setBalanceSliderPos(balance);
			updateNowPlaying(playbackQueueService.getCurrentEntry());
		}, false);
	}

	@Override
	public void onRefreshSkinsList() {
		new Thread(this::updateSkinsList).start();
	}

	@Override
	public void onPlay() {
		playbackController.onPlay();
	}

	@Override
	public void onPause() {
		playbackController.onPause();
	}

	@Override
	public void onImportSkins() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		jfc.setMultiSelectionEnabled(true);
		jfc.setFileFilter(new FileFilter() {

			@Override
			public String getDescription() {
				return "WinAmp Skin Zip files";
			}

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().toLowerCase().endsWith(".wsz");
			}
		});
		if (JFileChooser.APPROVE_OPTION == jfc.showOpenDialog(null)) {
			File[] files = jfc.getSelectedFiles();
			new Thread(() -> {
				for (File file : files) {
					try {
						winAmpSkinsService.importSkin(file);
						List<String> skins = Stream.concat(Stream.of(EMBEDDED_SKIN_NAME), winAmpSkinsService.listSkins().stream())
								.collect(Collectors.toList());
						SwingUtil.runOnEDT(() -> trayIconPopupMenu.setSkinsList(skins), false);
					} catch (Exception e) {
						LOGGER.log(Level.WARNING, "Skin import error", e);
						onPlaybackError("Skin import error: " + e.getClass().getSimpleName() + " " + e.getMessage());
					}
				}
			}).start();
		}
	}

	protected JPopupMenu buildPresetsMenu() {
		JPopupMenu presetsMenu = new JPopupMenu("Presets");
		eqPresetService.listPresets()
				.stream()
				.map(presetName -> new JMenuItem(presetName))
				.peek(jMenuItem -> jMenuItem.addActionListener(actEvent -> onLoadPreset(jMenuItem.getText())))
				.forEach(presetsMenu::add);
		return presetsMenu;
	}

	public void onLoadPreset(String eqPreset) {
		new Thread(() -> {
			try {
				EqualizerPreset preset = eqPresetService.loadPreset(eqPreset);
				setEQPreset(preset);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to load preset " + eqPreset, t);
			}
		}).start();
	}

	@Override
	public void onEQSavePreset(String name, EqualizerPreset equalizerPreset) {
		new Thread(() -> {
			try {
				eqPresetService.savePreset(name, equalizerPreset);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to save preset " + name, t);
			}
		}).start();
	}

	@Override
	public void onEQExportPreset(File file, String name, EqualizerPreset equalizerPreset) {
		new Thread(() -> {
			try (FileOutputStream fos = new FileOutputStream(file)) {
				eqPresetService.exportWinAmpEqfPreset(name, equalizerPreset, fos);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to export WinAmp EQF preset to file " + file.getAbsolutePath(), t);
			}
		}).start();
	}

	@Override
	public void onEQImportPreset(File presetFile) {
		if (presetFile.exists() && presetFile.length() == 299) {
			new Thread(() -> {
				try (FileInputStream fis = new FileInputStream(presetFile)) {
					Tuple2<String, EqualizerPreset> presetWithName = eqPresetService.importWinAmpEqfPreset(fis);
					eqPresetService.savePreset(presetWithName.getA(), presetWithName.getB());
					setEQPreset(presetWithName.getB());
				} catch (Throwable t) {
					LOGGER.log(Level.WARNING, "Failed to import WinAmp EQF preset from file " + presetFile.getAbsolutePath(), t);
				}
			}).start();
		}
	}

	@Override
	public void setEQEnabled(boolean enabled) {
		eqService.setEQEnabled(enabled);
		SwingUtil.runOnEDT(() -> {
			eqWindow.setEQEnabled(enabled);
			if (retroUIWindows != null) {
				retroUIWindows.getB().setEQEnabled(enabled);
			}
		}, false);
	}

	protected void setEQPreset(EqualizerPreset preset) {
		eqService.setPreset(preset);
		SwingUtil.runOnEDT(() -> {
			eqWindow.setPreset(preset);
			if (retroUIWindows != null) {
				retroUIWindows.getB().setPreset(preset);
			}
		}, false);
	}

	@Override
	public void showPresetsMenu(Component parentComponent, int x, int y) {
		buildPresetsMenu().show(parentComponent, x, y);
	}

	@Override
	public void onEQGainChange(double value) {
		eqService.setGain((int) Math.round(value * 1000));
		SwingUtil.runOnEDT(() -> {
			eqWindow.setGainValue(value);
			if (retroUIWindows != null) {
				retroUIWindows.getB().setGain(value);
			}
		}, false);
	}

	@Override
	public void onEQBandChange(int bandNumber, double value) {
		eqService.setBand(bandNumber, (int) Math.round(value * 1000));
		SwingUtil.runOnEDT(() -> {
			eqWindow.setBandValue(bandNumber, value);
			if (retroUIWindows != null) {
				retroUIWindows.getB().setBand(bandNumber, value);
			}
		}, false);
	}

	@Override
	public void onEQReset() {
		int eqBandsNum = eqService.getCurrentState().getBands().length;
		for (int i = 0; i < eqBandsNum; i++) {
			eqService.setBand(i, 500);
		}
		SwingUtil.runOnEDT(() -> {
			for (int i = 0; i < eqBandsNum; i++) {
				eqWindow.setBandValue(i, 0.5d);
			}
			if (retroUIWindows != null) {
				for (int i = 0; i < eqBandsNum; i++) {
					retroUIWindows.getB().setBand(i, 0.5d);
				}
			}
		}, false);
	}

	@Override
	public void onBalanceChange(double sliderPositionRatio) {
		playbackController.setBalance((int) Math.round(100 * sliderPositionRatio));
	}

	@Override
	public void scrollToNowPlaying() {
		int currentTrackIdx = playbackController.getTrackQueuePosition();
		if (currentTrackIdx >= 0) {
			this.scrollToTrack(currentTrackIdx);
		}
	}

	@Override
	public void retroUISwitchTimeDisplay() {
		retroUIShowRemainingTime = !retroUIShowRemainingTime;
	}

	@Override
	public void onRetroUISearchTextChange() {
		new Thread(() -> {
			String text = this.retroUIWindows.getC().getRetroUISearchInput().getText();
			boolean fullPhrase = this.retroUIWindows.getC().getRetroUISerchCheckboxFullPhrase().isSelected();
			List<Integer> matchedRows;
			if (text == null || text.trim().isEmpty()) {
				matchedRows = Collections.emptyList();
			} else {
				matchedRows = playQueueTableModel.search(text, fullPhrase);
			}
			this.currentRetroUISearchMatch = -1;
			SwingUtil.runOnEDT(() -> {
				retroUIWindows.getC().getRetroUISearchMatchCountLabel().setText(String.valueOf(matchedRows.size()));
				// for (int i : this.retroUISearchMatchedRows) {
				// retroUIPlayQueueTableModel.fireTableRowsUpdated(i, i);
				// }
				this.retroUISearchMatchedRows = matchedRows;
				// for (int i : matchedRows) {
				// retroUIPlayQueueTableModel.fireTableRowsUpdated(i, i);
				// }
				retroUIWindows.getC().getPlaylistTable().repaint();
				onSearchNextMatch();
			}, false);
		}).start();
	}

	@Override
	public void onSearchNextMatch() {
		int matchesCount = this.retroUISearchMatchedRows.size();
		if (matchesCount > 0) {
			this.currentRetroUISearchMatch++;
			if (this.currentRetroUISearchMatch >= matchesCount) {
				this.currentRetroUISearchMatch = 0;
			}
		}
		gotoSearchMatch();
	}

	@Override
	public void onSearchPreviousMatch() {
		int matchesCount = this.retroUISearchMatchedRows.size();
		if (matchesCount > 0) {
			this.currentRetroUISearchMatch--;
			if (this.currentRetroUISearchMatch < 0) {
				this.currentRetroUISearchMatch = matchesCount - 1;
			}
		}
		gotoSearchMatch();
	}

	private void gotoSearchMatch() {
		if (this.currentRetroUISearchMatch > -1 && this.currentRetroUISearchMatch < this.retroUISearchMatchedRows.size()) {
			int row = this.retroUISearchMatchedRows.get(this.currentRetroUISearchMatch);
			retroUIWindows.getC().scrollToTrack(row);
			retroUIWindows.getC().getPlaylistTable().getSelectionModel().setSelectionInterval(row, row);
		}
	}

	@Override
	public void showSkinBrowser() {
		if (SonivmUI.this.skinBrowser == null) {
			JFrame skinBrowser = new JFrame("Sonivm RetroUI skin browser");
			skinBrowser.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			skinBrowser.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					SonivmUI.this.skinBrowser.dispose();
					SonivmUI.this.skinBrowser = null;
				}
			});
			skinBrowser.getContentPane().setLayout(new BorderLayout());
			JList<String> skinsList = new JList<String>(winAmpSkinsService.listSkins().toArray(new String[0]));
			skinBrowser.setIconImage(sonivmIcon);
			skinBrowser.getContentPane().add(new JScrollPane(skinsList));
			skinsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			skinsList.getSelectionModel().addListSelectionListener(e -> SonivmUI.this.onRetroUiSkinChange(skinsList.getSelectedValue()));
			skinsList.setSelectedValue(preferencesService.getRetroUISkin(), true);
			skinBrowser.pack();
			if (SonivmUI.this.skinBrowser == null) {
				SonivmUI.this.skinBrowser = skinBrowser;
				SwingUtil.moveToScreenCenter(skinBrowser);
				skinBrowser.setVisible(true);
			}
		}
	}
}
