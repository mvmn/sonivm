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

import javax.annotation.PostConstruct;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.PlaybackListener;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;
import x.mvmn.sonivm.lastfm.LastFMScrobblingService;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.Tuple2;
import x.mvmn.sonivm.util.Tuple4;

@RequiredArgsConstructor
public class SonivmUI implements SonivmUIController, Consumer<Tuple2<Boolean, String>>, PlaybackListener {

	private static final Logger LOGGER = Logger.getLogger(SonivmUI.class.getSimpleName());

	@Getter
	protected final SonivmMainWindow mainWindow;
	@Getter
	protected final EqualizerWindow eqWindow;

	protected final BufferedImage sonivmIcon;
	protected final SonivmTrayIconPopupMenu trayIconPopupMenu;
	protected final PreferencesService preferencesService;
	protected final PlaybackController sonivmController;
	protected final LastFMScrobblingService lastFMScrobblingService;

	protected volatile TrayIcon sonivmTrayIcon;

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

	@PostConstruct
	public void init() {
		trayIconPopupMenu.registerHandler(this);
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

		mainWindow.registerHandler(this);
		sonivmController.addPlaybackListener(this);
		lastFMScrobblingService.addStatusListener(this);

		restoreMainWindowState();
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

	protected void restoreMainWindowState() {
		mainWindow.setShuffleMode(sonivmController.getShuffleMode());
		mainWindow.setRepeatMode(sonivmController.getRepeatMode());
		mainWindow.setAutoStop(sonivmController.isAutoStop());
	}

	public void onQuit() {
		sonivmController.onStop();
		saveUIState();
		savePlayQueueColumnsState();
		destroyUI();
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
	public void onShowRetroUIMainWindow() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onShowRetroUIEQWinwod() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onShowRetroUIPlaylistWindow() {
		// TODO Auto-generated method stub
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
		sonivmController.onShuffleModeSwitch(selectedItem);
	}

	@Override
	public void onRepeatModeSwitch(RepeatMode selectedItem) {
		sonivmController.onRepeatModeSwitch(selectedItem);
	}

	@Override
	public void onVolumeChange(int value) {
		sonivmController.onVolumeChange(value);
	}

	@Override
	public void onSeek(int val) {
		sonivmController.onSeek(val);
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
			switch (playbackState) {
				case STOPPED:
					mainWindow.disallowSeek();
					mainWindow.setCurrentPlayTimeDisplay(0, 0);
					mainWindow.updateNowPlaying(null);
					trayIconPopupMenu.updateNowPlaying(null);
					mainWindow.setStatusDisplay("");
				break;
				case PAUSED:
					trayIconPopupMenu.setPlayPauseButtonState(false);
				break;
				case PLAYING:
					trayIconPopupMenu.setPlayPauseButtonState(true);
				break;
				default:
					throw new IllegalArgumentException("Unsupported playback state " + playbackState);
			}
		}, false);
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
			mainWindow.updateNowPlaying(trackInfo);
			trayIconPopupMenu.updateNowPlaying(trackInfo);
			String audioInfoStr = audioInfo.getAudioFileFormat() != null
					? audioInfo.getAudioFileFormat().getFormat().toString().replaceAll(",\\s*$", "").replaceAll(",\\s*,", ",")
					: "";
			mainWindow.setStatusDisplay(audioInfoStr);
			mainWindow.scrollToTrack(sonivmController.getTrackQueuePosition());
		}, false);
	}

	@Override
	public void onPlaybackProgress(long playTimeMillis, int totalDurationSeconds) {
		int playbackIndicatorPosition = (int) (playTimeMillis / 100);
		SwingUtil.runOnEDT(() -> {
			mainWindow.updateSeekSliderPosition(playbackIndicatorPosition);
			mainWindow.setCurrentPlayTimeDisplay(playbackIndicatorPosition / 10, totalDurationSeconds);
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
}
