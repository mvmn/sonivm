package x.mvmn.sonivm.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.PlaybackListener;
import x.mvmn.sonivm.WinAmpSkinsService;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.PlaybackState;
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
	protected final PlaybackController sonivmController;
	protected final LastFMScrobblingService lastFMScrobblingService;
	protected final PlaybackQueueService playbackQueueService;
	protected final WinAmpSkinsService winAmpSkinsService;
	protected final TableModel playQueueTableModel;

	protected final RetroUIFactory retroUIFactory = new RetroUIFactory(); // TODO: inject

	protected volatile TrayIcon sonivmTrayIcon;

	protected volatile Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> retroUIWindows;

	public SonivmUI(SonivmMainWindow mainWindow,
			EqualizerWindow eqWindow,
			BufferedImage sonivmIcon,
			SonivmTrayIconPopupMenu trayIconPopupMenu,
			PreferencesService preferencesService,
			PlaybackController sonivmController,
			LastFMScrobblingService lastFMScrobblingService,
			PlaybackQueueService playbackQueueService,
			WinAmpSkinsService winAmpSkinsService,
			PlaybackQueueTableModel playQueueTableModel) {
		super();
		this.mainWindow = mainWindow;
		this.eqWindow = eqWindow;
		this.sonivmIcon = sonivmIcon;
		this.trayIconPopupMenu = trayIconPopupMenu;
		this.preferencesService = preferencesService;
		this.sonivmController = sonivmController;
		this.lastFMScrobblingService = lastFMScrobblingService;
		this.playbackQueueService = playbackQueueService;
		this.winAmpSkinsService = winAmpSkinsService;
		this.playQueueTableModel = new AbstractTableModel() {
			private static final long serialVersionUID = 5628564877472125514L;

			private final String[] COLUMN_NAMES = { "#", "Track", "Length" };

			@Override
			public Object getValueAt(int rowIndex, int columnIndex) {
				if (columnIndex == 0) {
					return playQueueTableModel.getValueAt(rowIndex, 0);
				} else if (columnIndex == 1) {
					String artist = playQueueTableModel.getValueAt(rowIndex, 3);
					String title = playQueueTableModel.getValueAt(rowIndex, 2);
					return artist + " - " + title;
				} else if (columnIndex == 2) {
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
				return 3;
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

		new Thread(() -> {
			sonivmController.restorePlaybackState();
			SwingUtil.runOnEDT(() -> {
				mainWindow.setShuffleMode(sonivmController.getShuffleMode());
				mainWindow.setRepeatMode(sonivmController.getRepeatMode());
				mainWindow.setAutoStop(sonivmController.isAutoStop());
			}, false);
		}).start();

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
			}

			@Override
			public void onTableRowsDelete(int firstRow, int lastRow, boolean waitForUiUpdate) {
				SwingUtil.runOnEDT(() -> mainWindow.updatePlayQueueSizeLabel(), false);
				mainWindow.applySearch();
			}
		});

		mainWindow.registerHandler(this);
		sonivmController.addPlaybackListener(this);
		lastFMScrobblingService.addStatusListener(this);

		restoreMainWindowPlaybackState();
	}

	protected void initRetroUI() {
		String retroUISkin = preferencesService.getRetroUISkin();
		File retroUISkinFile = retroUISkin != null ? winAmpSkinsService.getSkinFile(retroUISkin) : null;
		try {
			retroUIWindows = retroUIFactory.construct(retroUISkinFile, new JTable(playQueueTableModel));
		} catch (WSZLoadingException e) {
			LOGGER.log(Level.SEVERE, "Failed to cosntruct UI from WinAmp skin: " + retroUISkin, e);
		}
	}

	protected void restoreRetroUIWindowsState() {
		SwingUtil.runOnEDT(() -> {
			SwingUtil.restoreWindowState(retroUIWindows.getA().getWindow(), preferencesService.getRetroUIMainWindowState());
			SwingUtil.restoreWindowState(retroUIWindows.getB().getWindow(), preferencesService.getRetroUIEQWindowState());
			retroUIWindows.getC().getWindow().setScaleFactor(retroUIWindows.getA().getWindow().getScaleFactor());
			retroUIWindows.getC()
					.getWindow()
					.setSizeExtensions(preferencesService.getRetroUIPlaylistSizeExtX(), preferencesService.getRetroUIPlaylistSizeExtY());
			SwingUtil.restoreWindowState(retroUIWindows.getC().getWindow(), preferencesService.getRetroUIPlaylistWindowState());
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
		mainWindow.setShuffleMode(sonivmController.getShuffleMode());
		mainWindow.setRepeatMode(sonivmController.getRepeatMode());
		mainWindow.setAutoStop(sonivmController.isAutoStop());
	}

	public void onQuit() {
		sonivmController.onStop();
		saveUIState();
		saveRetroUIState();
		savePlayQueueColumnsState();
		destroyUI();
		destroyRetroUIWindows();
		sonivmController.onQuit();
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
		sonivmController.onPlayPause();
	}

	@Override
	public void onStop() {
		sonivmController.onStop();
	}

	@Override
	public void onPreviousTrack() {
		sonivmController.onNextTrack();
	}

	@Override
	public void onNextTrack() {
		sonivmController.onNextTrack();
	}

	@Override
	public boolean onDropQueueRowsInsideQueue(int queuePosition, int firstRow, int lastRow) {
		boolean result = sonivmController.onDropQueueRowsInsideQueue(queuePosition, firstRow, lastRow);
		if (result) {
			SwingUtil.runOnEDT(() -> mainWindow.setSelectedPlayQueueRows(queuePosition, queuePosition + (lastRow - firstRow)), false);
		}
		return result;
	}

	@Override
	public void onDropFilesToQueue(int row, List<File> fileList) {
		sonivmController.onDropFilesToQueue(row, fileList,
				importedTrack -> SwingUtil.runOnEDT(() -> mainWindow.setStatusDisplay("Loaded into queue: " + importedTrack), false));
	}

	@Override
	public void onAutoStopChange(boolean selected) {
		sonivmController.onAutoStopChange(selected);
	}

	@Override
	public void onShuffleModeSwitch(ShuffleMode selectedItem) {
		mainWindow.setShuffleMode(selectedItem);
		if (retroUIWindows != null) {
			retroUIWindows.getA().setShuffleToggleState(selectedItem != ShuffleMode.OFF);
		}
		sonivmController.onShuffleModeSwitch(selectedItem);
	}

	@Override
	public void onRepeatModeSwitch(RepeatMode selectedItem) {
		mainWindow.setRepeatMode(selectedItem);
		if (retroUIWindows != null) {
			retroUIWindows.getA().setRepeatToggleState(selectedItem != RepeatMode.OFF);
		}
		sonivmController.onRepeatModeSwitch(selectedItem);
	}

	@Override
	public void onVolumeChange(int value) {
		sonivmController.onVolumeChange(value);
	}

	@Override
	public void onSeek(double ratio) {
		this.onSeek((int) Math.round(ratio * sonivmController.getCurrentTrackLengthSeconds() * 10));
	}

	@Override
	public void onSeek(int tenthOfSeconds) {
		sonivmController.onSeek(tenthOfSeconds);
	}

	@Override
	public void toggleShowEqualizer() {
		SwingUtil.runOnEDT(() -> this.toggleEqWindow(), false);
	}

	@Override
	public void onTrackSelect(int row) {
		sonivmController.onTrackSelect(row);
	}

	@Override
	public void onDeleteRowsFromQueue(int start, int end) {
		sonivmController.onDeleteRowsFromQueue(start, end);
	}

	@Override
	public void scrollToTrack(int trackQueuePosition) {
		SwingUtil.runOnEDT(() -> mainWindow.scrollToTrack(trackQueuePosition), false);
	}

	@Override
	public void onPlaybackStateChange(PlaybackState playbackState) {
		SwingUtil.runOnEDT(() -> {
			if (retroUIWindows != null) {
				retroUIWindows.getA().setPlybackIndicatorState(playbackState);
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
			retroUIWindows.getA().setNowPlayingText(nowPlaying != null ? nowPlaying.toDisplayStr().trim() : "Stopped");
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
			mainWindow.scrollToTrack(sonivmController.getTrackQueuePosition());

			if (retroUIWindows != null) {
				retroUIWindows.getA().setPlayTime(0, false);
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
				retroUIWindows.getA().setPlayTime(playbackIndicatorPosition / 10, false);
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
			SwingUtil.runOnEDT(() -> retroUIWindows.getB().getWindow().setVisible(true), false);
		}
	}

	@Override
	public void onShowRetroUIPlaylistWindow() {
		if (retroUIWindows != null) {
			SwingUtil.runOnEDT(() -> retroUIWindows.getC().getWindow().setVisible(true), false);
		}
	}

	@Override
	public void onRetroUiSkinChange(String skinFileName) {
		try {
			Tuple3<RetroUIMainWindow, RetroUIEqualizerWindow, RetroUIPlaylistWindow> newRetroUi;
			if (EMBEDDED_SKIN_NAME.equals(skinFileName)) {
				newRetroUi = retroUIFactory.construct(null, new JTable(playQueueTableModel));
			} else {
				newRetroUi = retroUIFactory.construct(winAmpSkinsService.getSkinFile(skinFileName), new JTable(playQueueTableModel));
			}
			if (this.retroUIWindows != null) {
				saveRetroUIState();
				destroyRetroUIWindows();
			}
			this.retroUIWindows = newRetroUi;
			restoreRetroUIWindowsState();
			retroUIRegHandlerAndUpdateState();
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to construct retroUI with skin " + (skinFileName != null ? skinFileName : "embedded"), e);
		}
	}

	protected void retroUIRegHandlerAndUpdateState() {
		this.retroUIWindows.getA().addListener(this);
		this.retroUIWindows.getA().setEQToggleState(this.retroUIWindows.getB().getWindow().isVisible());
		this.retroUIWindows.getA().setPlaylistToggleState(this.retroUIWindows.getC().getWindow().isVisible());
		this.retroUIWindows.getA().setShuffleToggleState(sonivmController.getShuffleMode() != ShuffleMode.OFF);
		this.retroUIWindows.getA().setRepeatToggleState(sonivmController.getRepeatMode() != RepeatMode.OFF);
	}

	@Override
	public void onRefreshSkinsList() {
		new Thread(this::updateSkinsList).start();
	}

	@Override
	public void onPlay() {
		sonivmController.onPlay();
	}

	@Override
	public void onPause() {
		sonivmController.onPause();
	}
}
