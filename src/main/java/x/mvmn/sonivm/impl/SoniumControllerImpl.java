package x.mvmn.sonivm.impl;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import x.mvmn.sonivm.SonivmShutdownListener;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.audio.PlaybackEvent.ErrorType;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.lastfm.LastFMQueueService;
import x.mvmn.sonivm.model.IntRange;
import x.mvmn.sonivm.playqueue.PlaybackQueueFileImportService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.EqualizerWindow;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.SonivmTrayIconPopupMenu;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntryCompareBiPredicate;
import x.mvmn.sonivm.ui.model.RepeatMode;
import x.mvmn.sonivm.ui.model.ShuffleMode;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.StringUtil;
import x.mvmn.sonivm.util.Tuple2;
import x.mvmn.sonivm.util.Tuple4;

@Component
public class SoniumControllerImpl implements SonivmController {

	private static final Logger LOGGER = Logger.getLogger(SoniumControllerImpl.class.getSimpleName());

	@Autowired
	private AudioService audioService;

	@Autowired
	private PlaybackQueueService playbackQueueService;

	@Autowired
	private PlaybackQueueFileImportService playbackQueueFileImportService;

	@Autowired
	private SonivmMainWindow mainWindow;

	@Autowired
	private EqualizerWindow eqWindow;

	@Autowired
	private PreferencesService preferencesService;

	@Autowired
	private LastFMQueueService lastFMQueueService;

	@Autowired(required = false)
	private List<SonivmShutdownListener> shutdownListeners;

	@Autowired
	private TrayIcon sonivmTrayIcon;

	@Autowired
	private SonivmTrayIconPopupMenu trayIconPopupMenu;

	@Autowired
	private SonivmEqualizerService equalizerService;

	private volatile AudioFileInfo currentAudioFileInfo;
	private volatile PlaybackQueueEntry currentTrackInfo;
	private volatile ShuffleMode shuffleState = ShuffleMode.OFF;
	private volatile RepeatMode repeatState = RepeatMode.OFF;

	private volatile AtomicLong currentTrackTotalListeningTimeMillisec = new AtomicLong(0L);
	private volatile int scrobbleThresholdPercent = 70;
	private volatile boolean scrobblingEnabled = false;
	private volatile boolean currentTrackScrobbled = true;

	private final ExecutorService lastFmScrobbleTaskExecutor = Executors.newFixedThreadPool(1);
	private final AtomicReference<Session> lastFMSession = new AtomicReference<>();

	private volatile List<Integer> searchMatches = Collections.emptyList();
	private volatile int currentSearchMatch = -1;

	@PostConstruct
	public void initPostConstruct() {
		audioService.addPlaybackEventListener(this);
	}

	@Override
	public void onVolumeChange(int value) {
		audioService.setVolumePercentage(value);
	}

	@Override
	public void onSeek(int value) {
		if (currentTrackInfo != null) {
			if (currentTrackInfo.isCueSheetTrack()) {
				audioService.seek(currentTrackInfo.getCueSheetTrackStartTimeMillis().intValue() + value * 100);
			} else {
				audioService.seek(value * 100);
			}
		}
	}

	@Override
	public void onPlayPause() {
		if (audioService.isPaused()) {
			audioService.resume();
			updatePlayingState(true);
		} else if (audioService.isStopped()) {
			tryPlayFromStartOfQueue();
		} else {
			audioService.pause();
			updatePlayingState(false);
		}
	}

	@Override
	public void onStop() {
		doStop();
	}

	@Override
	public void onTrackSelect(int index) {
		switchToTrack(index);
	}

	private void onTrackFinished() {
		if (mainWindow.isAutoStop()) {
			doStop();
		} else {
			doNextTrack(false);
		}
	}

	@Override
	public void onNextTrack() {
		doNextTrack(true);
	}

	private void doNextTrack(boolean userRequested) {
		int trackCount = playbackQueueService.getQueueSize();
		int currentTrackQueuePos = playbackQueueService.getCurrentQueuePosition();
		ShuffleMode shuffleState = this.shuffleState;
		RepeatMode repeatState = this.repeatState;

		if (!userRequested && RepeatMode.TRACK == repeatState) {
			repeatCurrentTrack();
			return;
		}

		if (currentTrackQueuePos < 0) {
			tryPlayFromStartOfQueue();
			return;
		}

		if (trackCount > 0) {
			if (ShuffleMode.OFF != shuffleState) {
				PlaybackQueueEntry currentTrack = playbackQueueService.getEntryByIndex(currentTrackQueuePos);
				switch (shuffleState) {
					case ALBUM: {
						int[] matchingTrackIndexes = playbackQueueService
								.findTracks(track -> currentTrack.trackPropertiesEqual(track, PlaybackQueueEntry::getAlbum)
										&& currentTrack.trackPropertiesEqual(track, PlaybackQueueEntry::getArtist));
						if (matchingTrackIndexes.length < 1) {
							tryPlayFromStartOfQueue();
						} else {
							switchToTrack(
									matchingTrackIndexes[new Random(System.currentTimeMillis()).nextInt(matchingTrackIndexes.length)]);
						}
						return;
					}
					case ARTIST: {
						int[] matchingTrackIndexes = playbackQueueService
								.findTracks(track -> currentTrack.trackPropertiesEqual(track, PlaybackQueueEntry::getArtist));
						if (matchingTrackIndexes.length < 1) {
							tryPlayFromStartOfQueue();
						} else {
							switchToTrack(
									matchingTrackIndexes[new Random(System.currentTimeMillis()).nextInt(matchingTrackIndexes.length)]);
						}
						return;
					}
					case GENRE: {
						int[] matchingTrackIndexes = playbackQueueService
								.findTracks(track -> currentTrack.trackPropertiesEqual(track, PlaybackQueueEntry::getGenre));
						if (matchingTrackIndexes.length < 1) {
							tryPlayFromStartOfQueue();
						} else {
							switchToTrack(
									matchingTrackIndexes[new Random(System.currentTimeMillis()).nextInt(matchingTrackIndexes.length)]);
						}
						return;
					}
					case OFF:
					// Won't happen because of enclosing if condition
					break;
					default:
					case PLAYLIST: {
						switchToTrack(new Random(System.currentTimeMillis()).nextInt(trackCount));
						return;
					}
				}
			} else {
				if (RepeatMode.OFF != repeatState) {
					int endOfRange = trackCount - 1;
					int startOfRange = 0;
					switch (repeatState) {
						case ALBUM:
							IntRange albumRange = playbackQueueService.detectTrackRange(currentTrackQueuePos,
									PlaybackQueueEntryCompareBiPredicate.BY_ARTIST_AND_ALBUM);
							startOfRange = albumRange.getFrom();
							endOfRange = albumRange.getTo();
						break;
						case ARTIST:
							IntRange artistRange = playbackQueueService.detectTrackRange(currentTrackQueuePos,
									PlaybackQueueEntryCompareBiPredicate.BY_ARTIST);
							startOfRange = artistRange.getFrom();
							endOfRange = artistRange.getTo();
						break;
						case PLAYLIST:
						break;
						case TRACK:
						case OFF:
						default:
						// Won't happen
						break;
					}
					if (currentTrackQueuePos == endOfRange) {
						switchToTrack(startOfRange);
						return;
					}
				}

				if (currentTrackQueuePos >= trackCount - 1) {
					doStop();
				} else if (currentTrackQueuePos < 0) {
					tryPlayFromStartOfQueue();
				} else {
					switchToTrack(++currentTrackQueuePos, userRequested);
				}
			}
		} else {
			doStop();
		}
	}

	@Override
	public void onPreviousTrack() {
		int currentTrack = playbackQueueService.getCurrentQueuePosition();
		if (currentTrack > 0) {
			switchToTrack(--currentTrack);
		}
	}

	private void tryPlayFromStartOfQueue() {
		if (playbackQueueService.getQueueSize() > 0) {
			switchToTrack(0);
		} else {
			doStop();
		}
	}

	private void switchToTrack(int trackQueuePosition) {
		switchToTrack(trackQueuePosition, true);
	}

	private void switchToTrack(int trackQueuePosition, boolean stopCurrent) {
		int queueSize = playbackQueueService.getQueueSize();
		if (trackQueuePosition >= queueSize) {
			doStop();
		} else {
			PlaybackQueueEntry currentTrack = !audioService.isStopped() ? currentTrackInfo : null;
			PlaybackQueueEntry newTrack = playbackQueueService.getEntryByIndex(trackQueuePosition);
			playbackQueueService.setCurrentQueuePosition(trackQueuePosition);
			if (!stopCurrent && newTrack.isCueSheetTrack() && currentTrack != null && currentTrack.isCueSheetTrack()
					&& currentTrack.getTargetFileFullPath().equals(newTrack.getTargetFileFullPath())
					&& currentTrack.getCueSheetTrackFinishTimeMillis().equals(newTrack.getCueSheetTrackStartTimeMillis())) {
				// Next track is continuation of current track in an audio file, just a next cue index
				LOGGER.fine("Next CUE track is a continuation of same auido file");
				handleStartTrackPlay(currentAudioFileInfo);
			} else {
				File track = new File(playbackQueueService.getEntryByIndex(trackQueuePosition).getTargetFileFullPath());
				if (stopCurrent || currentTrack != null && currentTrack.isCueSheetTrack()) {
					audioService.stop();
				}
				if (newTrack.isCueSheetTrack()) {
					audioService.play(track, newTrack.getCueSheetTrackStartTimeMillis().intValue());
				} else {
					audioService.play(track);
				}
				updatePlayingState(true);
			}
			SwingUtil.runOnEDT(() -> mainWindow.scrollToTrack(trackQueuePosition), false);
		}
	}

	private void repeatCurrentTrack() {
		if (currentAudioFileInfo != null) {
			File track = new File(currentAudioFileInfo.getFilePath());
			audioService.stop();
			audioService.play(track);
			updatePlayingState(true);
		} else {
			doStop();
		}
	}

	private void doStop() {
		audioService.stop();
		this.currentAudioFileInfo = null;
		this.currentTrackInfo = null;
		playbackQueueService.setCurrentQueuePosition(-1);
		updatePlayingState(false);
		SwingUtil.runOnEDT(() -> {
			mainWindow.disallowSeek();
			mainWindow.setCurrentPlayTimeDisplay(0, 0);
			mainWindow.updateNowPlaying(null);
			trayIconPopupMenu.updateNowPlaying(null);
			mainWindow.updateStatus("");
		}, false);
	}

	@Override
	public void onDropFilesToQueue(int queuePosition, List<File> files) {
		new Thread(() -> playbackQueueFileImportService.importFilesIntoPlayQueue(queuePosition, files,
				importedTrack -> SwingUtil.runOnEDT(() -> updateStaus("Loaded into queue: " + importedTrack), false))).start();
	}

	@Override
	public boolean onDropQueueRowsInsideQueue(int insertPosition, int firstRow, int lastRow) {
		int rowCount = lastRow - firstRow + 1;
		if (insertPosition > lastRow || insertPosition < firstRow) {
			playbackQueueService.moveRows(insertPosition, firstRow, lastRow);

			if (lastRow < insertPosition) {
				insertPosition -= rowCount;
			}

			mainWindow.getPlayQueueTable().getSelectionModel().setSelectionInterval(insertPosition, insertPosition + rowCount - 1);

			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onQuit() {
		LOGGER.info("Quit requested - shutting down");

		try {
			LOGGER.info("Storing window positions/sizes/visibility");
			this.preferencesService.saveMainWindowState(SwingUtil.getWindowState(this.mainWindow));
			this.preferencesService.saveEQWindowState(SwingUtil.getWindowState(this.eqWindow));
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to store main and EQ window states", t);
		}

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

		LOGGER.info("Shutting down audio service.");
		audioService.stop();
		audioService.shutdown();

		LOGGER.info("Shutting down LastFM task executor.");
		lastFmScrobbleTaskExecutor.shutdown();

		if (shutdownListeners != null && !shutdownListeners.isEmpty()) {
			for (SonivmShutdownListener shutdownListener : shutdownListeners) {
				try {
					shutdownListener.onSonivmShutdown();
				} catch (Throwable t) {
					LOGGER.log(Level.SEVERE, "Failed on calling shutdown listener " + shutdownListener.getClass().getSimpleName(), t);
				}
			}
		}

		try {
			this.preferencesService.setShuffleMode(shuffleState);
			this.preferencesService.setRepeatMode(repeatState);
			this.preferencesService.setAutoStop(this.mainWindow.isAutoStop());
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to store shuffle/repeat preferences", t);
		}

		savePlayQueueColumnsState();
		savePlayQueueContents();
		saveEqState();

		// System.exit(0);
	}

	private void savePlayQueueContents() {
		try {
			LOGGER.info("Storing play queue.");
			new ObjectMapper().writeValue(getPlayQueueStorageFile(), this.playbackQueueService.getCopyOfQueue());
			LOGGER.info("Storing play queue succeeded.");
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store the playback queue", e);
		}
	}

	private void restorePlayQueueContents() {
		try {
			LOGGER.info("Restoring play queue.");
			File queueFile = getPlayQueueStorageFile();
			if (queueFile.exists()) {
				List<PlaybackQueueEntry> queueEntries = new ObjectMapper().readValue(queueFile,
						new TypeReference<List<PlaybackQueueEntry>>() {});
				this.playbackQueueService.clearQueue();
				this.playbackQueueService.addRows(queueEntries);
				LOGGER.info("Restoring play queue succeeded.");
			} else {
				LOGGER.info("Restoring play queue not needed - no queue stored yet.");
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to retrieve and restore the playback queue", e);
		}
	}

	private File getPlayQueueStorageFile() {
		return new File(new File(System.getProperty("sonivm_home_folder")), "queue.json");
	}

	private void savePlayQueueColumnsState() {
		LOGGER.info("Saving UI state.");
		try {
			JTable tblPlayQueue = mainWindow.getPlayQueueTable();
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
			preferencesService.setPlayQueueColumnWidths(columnWidths);

			int[] columnPositions = new int[columnModel.getColumnCount()];
			for (int i = 0; i < columnModel.getColumnCount(); i++) {
				columnPositions[i] = tblPlayQueue.convertColumnIndexToView(i);
			}
			preferencesService.setPlayQueueColumnPositions(columnPositions);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store column width for playback queue table", e);
		}
	}

	@Override
	public void handleEvent(PlaybackEvent event) {
		switch (event.getType()) {
			case ERROR:
				LOGGER.log(Level.WARNING, "Playback error occurred: " + event.getErrorType() + " " + event.getError());
				SwingUtil.runOnEDT(() -> mainWindow.updateStatus("Playback error " + event.getErrorType() + " " + event.getError()), false);
				if (event.getErrorType() == ErrorType.FILE_NOT_FOUND || event.getErrorType() == ErrorType.FILE_FORMAT_ERROR) {
					onTrackFinished();
				}
			break;
			case FINISH:
				// this.currentAudioFileInfo = null;
				// this.currentTrackInfo = null;
				onTrackFinished();
			break;
			case PROGRESS:
				if (currentAudioFileInfo != null) {
					Long playbackPositionMillis;
					int totalDurationSeconds;
					PlaybackQueueEntry currentTrackInfo = this.currentTrackInfo;
					long playbackPositionFromEvent = event.getPlaybackPositionMilliseconds();
					if (currentTrackInfo != null && currentTrackInfo.isCueSheetTrack()) {
						if (playbackPositionFromEvent >= currentTrackInfo.getCueSheetTrackFinishTimeMillis()) {
							// End of cue sheet track
							// Skip if we have already switched to next track - and these are just training events from previous track
							if (currentTrackInfo == playbackQueueService.getCurrentEntry()) {
								if (LOGGER.isLoggable(Level.FINE)) {
									LOGGER.fine("End of cue sheet track: " + currentTrackInfo.getTitle());
								}
								onTrackFinished();
							}
							// handleStartTrackPlay(currentAudioFileInfo);
							break;
						}

						playbackPositionMillis = playbackPositionFromEvent - currentTrackInfo.getCueSheetTrackStartTimeMillis();
						totalDurationSeconds = currentTrackInfo.getDuration();
					} else {
						playbackPositionMillis = playbackPositionFromEvent;
						totalDurationSeconds = currentAudioFileInfo.getDurationSeconds();
					}
					int seekSliderNewPosition = (int) (playbackPositionMillis / 100);

					SwingUtil.runOnEDT(() -> {
						mainWindow.updateSeekSliderPosition(seekSliderNewPosition);
						mainWindow.setCurrentPlayTimeDisplay((int) (playbackPositionMillis / 1000), totalDurationSeconds);
					}, false);
					long totalListenTimeSeconds = this.currentTrackTotalListeningTimeMillisec
							.addAndGet(event.getPlaybackDeltaMilliseconds()) / 1000;
					if (scrobblingEnabled && !currentTrackScrobbled
							&& totalListenTimeSeconds * 100 / totalDurationSeconds > scrobbleThresholdPercent && currentTrackInfo != null) {
						lastFmScrobble(currentTrackInfo);
						currentTrackScrobbled = true;
					}
				}
			break;
			case START:
				LOGGER.info("On new track start: " + event.getAudioMetadata());
				handleStartTrackPlay(event.getAudioMetadata());
				if (event.getEqControls() != null) {
					equalizerService.setEqControls(event.getEqControls(),
							event.getAudioMetadata().getAudioFileFormat().getFormat().getChannels());
				} else {
					equalizerService.setEqControls(null, 0);
				}
			break;
			case DATALINE_CHANGE:
			// Control[] controls = event.getDataLineControls();
			// for (Control dataLineControl : controls) {
			// System.out.println(dataLineControl.getType() + ": " + dataLineControl);
			// }
			break;
		}
	}

	private void handleStartTrackPlay(AudioFileInfo audioInfo) {
		this.currentTrackTotalListeningTimeMillisec.set(0L);
		this.currentTrackScrobbled = false;
		this.currentAudioFileInfo = audioInfo;
		PlaybackQueueEntry currentEntry = playbackQueueService.getCurrentEntry();
		if (currentEntry != null && !currentEntry.isCueSheetTrack()) {
			currentEntry.setDuration(audioInfo.getDurationSeconds());
		}
		int trackDurationSeconds = currentEntry.getDuration().intValue();
		int queuePos = playbackQueueService.getCurrentQueuePosition();
		PlaybackQueueEntry trackInfo = playbackQueueService.getEntryByIndex(queuePos);
		this.currentTrackInfo = trackInfo;
		playbackQueueService.signalUpdateInRow(queuePos);
		SwingUtil.runOnEDT(() -> {
			mainWindow.updateSeekSliderPosition(0);
			if (audioInfo.isSeekable()) {
				mainWindow.allowSeek(trackDurationSeconds * 10);
			} else {
				mainWindow.disallowSeek();
			}
			mainWindow.updateNowPlaying(trackInfo);
			trayIconPopupMenu.updateNowPlaying(trackInfo);
			mainWindow.updateStatus(audioInfo.getAudioFileFormat() != null
					? audioInfo.getAudioFileFormat().getFormat().toString().replaceAll(",\\s*$", "").replaceAll(",\\s*,", ",")
					: "");
		}, false);
		if (scrobblingEnabled) {
			lastFmSetNowPlaying(this.currentTrackInfo);
		}
	}

	private void lastFmSetNowPlaying(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			ScrobbleData scrobbleData = toScrobbleData(trackInfo);
			boolean success = false;
			String status = "";
			try {
				Session session = getLastFMSession();
				if (session != null) {
					LOGGER.info("Setting LastFM now playing state to " + scrobbleData);
					ScrobbleResult scrobbleResult = Track.updateNowPlaying(scrobbleData, session);
					if (!scrobbleResult.isSuccessful()) {
						status = "LastFM update now playing failed: " + scrobbleResult.getErrorCode() + " "
								+ scrobbleResult.getErrorMessage();
						LOGGER.info(status);
					} else {
						success = true;
						status = "Set LastFM now playing to " + trackInfo.toDisplayStr();
					}
				} else {
					LOGGER.info("Skipping update now playing in LastFM - no session.");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to update now playing in LastFM", e);
				status = "Failed to update now playing in LastFM: " + e.getClass().getSimpleName() + " "
						+ StringUtil.blankForNull(e.getMessage());
			}
			boolean finalSuccess = success;
			String finalStatus = status;
			SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(finalSuccess, finalStatus), false);

			if (success) {
				reSubmitFailedLastFMSubmissions();
			}
		});
	}

	private void lastFmScrobble(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			ScrobbleData scrobbleData = toScrobbleData(trackInfo);
			Tuple2<Boolean, String> result = doScrobbleTrack(scrobbleData);
			if (!result.getA()) {
				lastFMQueueService.queueTrack(scrobbleData);
			}
			SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(result.getA(), result.getB()), false);
		});
	}

	private void reSubmitFailedLastFMSubmissions() {
		try {
			lastFMQueueService.processQueuedTracks(tracks -> {
				for (ScrobbleData track : tracks) {
					Tuple2<Boolean, String> result = this.doScrobbleTrack(track);
					if (!result.getA()) {
						throw new RuntimeException("Failed to re-scrobble track " + track + ": " + result.getB());
					}
				}
			}, 100);
		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE, "Failed to re-submit LastFM tracks", t);
		}
	}

	private Tuple2<Boolean, String> doScrobbleTrack(ScrobbleData scrobbleData) {
		boolean success = false;
		String status = "";
		try {
			Session session = getLastFMSession();
			if (session != null) {
				LOGGER.info("Scrobbling LastFM track played " + scrobbleData);
				ScrobbleResult scrobbleResult = Track.scrobble(scrobbleData, session);
				if (!scrobbleResult.isSuccessful()) {
					status = "LastFM scrobbling failed: " + scrobbleResult.getErrorCode() + " " + scrobbleResult.getErrorMessage();
					LOGGER.info(status);
				} else {
					success = true;
					status = "Scrobbled to LastFM " + PlaybackQueueEntry.toDisplayStr(scrobbleData);
				}
			} else {
				LOGGER.info("Skipping scrobbling track in LastFM - no session.");
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to scrobble track in LastFM", e);
			status = "Failed to scrobble track in LastFM: " + e.getClass().getSimpleName() + " " + StringUtil.blankForNull(e.getMessage());
		}
		return Tuple2.<Boolean, String> builder().a(success).b(status).build();
	}

	private Session getLastFMSession() throws Exception {
		Session result = lastFMSession.get();
		if (result == null) {
			String user = this.preferencesService.getUsername();
			String password = this.preferencesService.getPassword();
			String apiKey = this.preferencesService.getApiKey();
			String secret = this.preferencesService.getApiSecret();

			if (user != null && password != null) {
				try {
					LOGGER.info("Trying to establish LastFM session");
					result = Authenticator.getMobileSession(user, password, apiKey, secret);
					if (result == null) {
						throw new Exception("Failed to connect");
					}
					lastFMSession.set(result);
					LOGGER.info("Successfully established LastFM session");
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(true, "Established LastFM session"), false);
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to establish LastFM session", e);
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(false, "Failed to establish LastFM session: "
							+ e.getClass().getSimpleName() + " " + StringUtil.blankForNull(e.getMessage())), false);
				}
			}
		}
		return result;
	}

	private ScrobbleData toScrobbleData(PlaybackQueueEntry trackInfo) {
		ScrobbleData result = new ScrobbleData(trackInfo.getArtist(), trackInfo.getTitle(), (int) (System.currentTimeMillis() / 1000));
		if (trackInfo.getDuration() != null) {
			result.setDuration(trackInfo.getDuration().intValue());
		}

		if (trackInfo.getTrackMetadata() != null) {
			result.setAlbum(trackInfo.getAlbum());
			try {
				result.setTrackNumber(Integer.parseInt(trackInfo.getTrackNumber()));
			} catch (NumberFormatException nfe) {
				LOGGER.finest("Can't parse track number as integer for LastFM: " + trackInfo.getTrackNumber());
			}
		}
		return result;
	}

	@Override
	public void onDeleteRowsFromQueue(int firstRow, int lastRow) {
		playbackQueueService.deleteRows(firstRow, lastRow + 1);
	}

	private void updatePlayingState(boolean playing) {
		SwingUtil.runOnEDT(() -> {
			mainWindow.setPlayPauseButtonState(playing);
			trayIconPopupMenu.setPlayPauseButtonState(playing);
		}, false);
	}

	private void updateStaus(String value) {
		SwingUtil.runOnEDT(() -> mainWindow.updateStatus(value), false);
	}

	@Override
	public void onBeforeUiPack() {
		new Thread(() -> {
			restorePlayQueueContents();

			try {
				this.scrobblingEnabled = this.preferencesService.getPassword() != null;
				int scrobblePercent = this.preferencesService.getPercentageToScrobbleAt(70);
				this.scrobbleThresholdPercent = scrobblePercent;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to read LastFM preferences", e);
			}

			try {
				ShuffleMode shuffleMode = this.preferencesService.getShuffleMode();
				this.shuffleState = shuffleMode;
				this.mainWindow.setShuffleMode(shuffleMode);

				RepeatMode repeatMode = this.preferencesService.getRepeatMode();
				this.repeatState = repeatMode;
				this.mainWindow.setRepeatMode(repeatMode);

				this.mainWindow.setAutoStop(this.preferencesService.isAutoStop());
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to read shuffle/repeat/autostop preferences", e);
			}
		}).start();
	}

	@Override
	public void onBeforeUiSetVisible() {
		restoreEqState();
		restoreWindowsState();
		restorePlayQueueColumnsState();
		lastFmScrobbleTaskExecutor.execute(() -> reSubmitFailedLastFMSubmissions());
	}

	private void restoreWindowsState() {
		LOGGER.info("Restoring window positions/sizes/visibility");
		try {
			Tuple4<Boolean, String, Point, Dimension> mainWindowState = preferencesService.getMainWindowState();
			SwingUtil.runOnEDT(() -> applyWindowState(mainWindow, mainWindowState, true), true);
			Tuple4<Boolean, String, Point, Dimension> eqWindowState = preferencesService.getEQWindowState();
			SwingUtil.runOnEDT(() -> applyWindowState(eqWindow, eqWindowState, false), true);
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to restore window states", t);
		}
	}

	private void applyWindowState(Window window, Tuple4<Boolean, String, Point, Dimension> windowState, boolean visibleByDefault) {
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
				SwingUtil.runOnEDT(() -> {
					JTable tblPlayQueue = mainWindow.getPlayQueueTable();
					TableColumnModel columnModel = tblPlayQueue.getColumnModel();

					for (int i = 0; i < columnModel.getColumnCount() && i < playQueueColumnPositions.length; i++) {
						columnModel.moveColumn(tblPlayQueue.convertColumnIndexToView(i), playQueueColumnPositions[i]);
					}
				}, true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column positions for playback queue table", e);
		}

		try {
			int[] playQueueColumnWidths = preferencesService.getPlayQueueColumnWidths();
			if (playQueueColumnWidths != null && playQueueColumnWidths.length > 0) {
				SwingUtil.runOnEDT(() -> {
					TableColumnModel columnModel = mainWindow.getPlayQueueTable().getColumnModel();

					int totalWidth = 0;
					for (int i = 0; i < columnModel.getColumnCount(); i++) {
						totalWidth += columnModel.getColumn(i).getWidth();
					}

					for (int i = 0; i < columnModel.getColumnCount() && i < playQueueColumnWidths.length; i++) {
						long width10k = playQueueColumnWidths[i] * totalWidth;
						columnModel.getColumn(i).setPreferredWidth((int) (width10k / 10000));
					}
				}, true);
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read+apply column width for playback queue table", e);
		}
	}

	@Override
	public void onSetAudioDevice(AudioDeviceOption audioDeviceOption) {
		audioService
				.setAudioDevice(audioDeviceOption.getAudioDeviceInfo() != null ? audioDeviceOption.getAudioDeviceInfo().getName() : null);
	}

	@Override
	public void onSetLookAndFeel(String lookAndFeelId) {
		SwingUtil.setLookAndFeel(lookAndFeelId, false);
		new Thread(() -> {
			try {
				preferencesService.setLookAndFeel(lookAndFeelId);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to save look and feel preference", e);
			}
		}).start();
	}

	@Override
	public void onRepeatModeSwitch(RepeatMode repeatMode) {
		this.repeatState = repeatMode;
	}

	@Override
	public void onShuffleModeSwitch(ShuffleMode shuffleMode) {
		this.shuffleState = shuffleMode;
	}

	@Override
	public void onLastFMScrobblePercentageChange(int scrobblePercentageOption) {
		this.scrobbleThresholdPercent = scrobblePercentageOption;
		new Thread(() -> {
			try {
				this.preferencesService.setPercentageToScrobbleAt(scrobblePercentageOption);
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to save scrobble percentage preference", e);
			}
		}).start();
	}

	@Override
	public void onLastFMCredsOrKeysUpdate() {
		try {
			this.scrobblingEnabled = this.preferencesService.getPassword() != null;
		} catch (GeneralSecurityException e) {
			LOGGER.log(Level.WARNING, "Security exception on getting LastFM password from prefs", e);
		}
		this.lastFMSession.set(null);
	}

	@Override
	public void onSearchValueChange() {
		String text = mainWindow.getSearchText();
		boolean fullPhrase = mainWindow.isSearchFullPhrase();
		new Thread(() -> {
			if (text == null || text.trim().isEmpty()) {
				searchMatches = Collections.emptyList();
			} else {
				searchMatches = IntStream.of(playbackQueueService.findTracks(searchPredicate(text, fullPhrase)))
						.mapToObj(Integer::valueOf)
						.collect(Collectors.toList());
			}
			currentSearchMatch = -1;
			SwingUtil.runOnEDT(() -> {
				mainWindow.setSearchMatchedRows(searchMatches);
				onSearchNextMatch();
			}, false);
		}).start();
	}

	private Predicate<PlaybackQueueEntry> searchPredicate(String searchText, boolean searchFullPhrase) {
		searchText = StringUtil.stripAccents(searchText.toLowerCase()).replaceAll("\\s+", " ").trim();
		String[] searchTerms;
		if (searchFullPhrase) {
			searchTerms = new String[] { searchText };
		} else {
			searchTerms = searchText.split(" ");
		}
		return queueEntry -> queueEntryMatchesSearch(queueEntry, searchTerms);
	}

	private static final List<Function<PlaybackQueueEntry, String>> QUEUE_ENTRY_TO_SEARCH_FIELDS = Arrays.asList(
			PlaybackQueueEntry::getArtist, PlaybackQueueEntry::getAlbum, PlaybackQueueEntry::getTitle, PlaybackQueueEntry::getDate,
			PlaybackQueueEntry::getGenre);

	private boolean queueEntryMatchesSearch(PlaybackQueueEntry queueEntry, String[] searchTerms) {
		for (String term : searchTerms) {
			boolean match = false;
			for (Function<PlaybackQueueEntry, String> getFieldFunct : QUEUE_ENTRY_TO_SEARCH_FIELDS) {
				if (normalizeForSearch(getFieldFunct.apply(queueEntry)).contains(term)) {
					match = true;
					break;
				}
			}
			if (!match) {
				return false;
			}
		}
		return true;
	}

	private String normalizeForSearch(String val) {
		return StringUtil.stripAccents(StringUtil.blankForNull(val).toLowerCase()).replaceAll("\\s+", " ").trim();
	}

	@Override
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

	@Override
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
			mainWindow.scrollToTrack(row);
			mainWindow.selectTrack(row);
		}
	}

	@Override
	public void toggleShowEqualizer() {
		if (!eqWindow.isVisible()) {
			SwingUtil.showAndBringToFront(eqWindow);
		} else {
			eqWindow.setVisible(false);
		}
	}

	private void restoreEqState() {
		try {
			LOGGER.info("Restoring EQ state");
			EqualizerState eqState = EqualizerState.builder()
					.enabled(preferencesService.isEqEnabled())
					.gain(preferencesService.getEqGain())
					.bands(preferencesService.getEqBands())
					.build();
			// equalizerService.setState(eqState);
			eqWindow.setState(eqState);
			LOGGER.info("Restoring EQ state succeeded");
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to restore EQ state", t);
		}
	}

	private void saveEqState() {
		try {
			LOGGER.info("Storing EQ state");
			EqualizerState eqState = equalizerService.getCurrentState();
			preferencesService.setEqEnabled(eqState.isEnabled());
			preferencesService.setEqGain(eqState.getGain());
			preferencesService.setEqBands(eqState.getBands());
			LOGGER.info("Storing EQ state succeeded");
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to save EQ state", t);
		}
	}
}
