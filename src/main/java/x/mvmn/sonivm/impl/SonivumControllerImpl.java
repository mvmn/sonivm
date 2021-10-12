package x.mvmn.sonivm.impl;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.swing.table.TableColumnModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.model.IntRange;
import x.mvmn.sonivm.playqueue.PlaybackQueueFileImportService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.RepeatMode;
import x.mvmn.sonivm.ui.model.ShuffleMode;
import x.mvmn.sonivm.util.SonivmShutdownListener;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

@Component
public class SonivumControllerImpl implements SonivmController {

	private static final Logger LOGGER = Logger.getLogger(SonivumControllerImpl.class.getSimpleName());

	@Autowired
	private AudioService audioService;

	@Autowired
	private PlaybackQueueService playbackQueueService;

	@Autowired
	private PlaybackQueueFileImportService playbackQueueFileImportService;

	@Autowired
	private SonivmMainWindow mainWindow;

	@Autowired
	private PreferencesService preferencesService;

	@Autowired(required = false)
	private List<SonivmShutdownListener> shutdownListeners;

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
		if (currentTrackInfo.isCueSheetTrack()) {
			audioService.seek(currentTrackInfo.getCueSheetTrackStartTimeMillis().intValue() + value * 100);
		} else {
			audioService.seek(value * 100);
		}
	}

	@Override
	public void onPlayPause() {
		if (audioService.isPaused()) {
			audioService.resume();
			updateStaus("Playing");
			updatePlayingState(true);
		} else if (audioService.isStopped()) {
			tryPlayFromStartOfQueue();
		} else {
			audioService.pause();
			updateStaus("Paused");
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
		doNextTrack(false);
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
						int[] matchingTrackIndexes = playbackQueueService.findTracksByProperty(currentTrack.getAlbum(), false);
						if (matchingTrackIndexes.length < 1) {
							tryPlayFromStartOfQueue();
						} else {
							switchToTrack(
									matchingTrackIndexes[new Random(System.currentTimeMillis()).nextInt(matchingTrackIndexes.length)]);
						}
						return;
					}
					case ARTIST: {
						int[] matchingTrackIndexes = playbackQueueService.findTracksByProperty(currentTrack.getArtist(), true);
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
							IntRange albumRange = playbackQueueService.detectTrackRange(currentTrackQueuePos, false);
							startOfRange = albumRange.getFrom();
							endOfRange = albumRange.getTo();
						break;
						case ARTIST:
							IntRange artistRange = playbackQueueService.detectTrackRange(currentTrackQueuePos, true);
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
				LOGGER.info("Next CUE track is a continuation of same auido file");
				handleStartTrackPlay(currentAudioFileInfo);
			} else {
				File track = new File(playbackQueueService.getEntryByIndex(trackQueuePosition).getTargetFileFullPath());
				if (stopCurrent || currentTrack.isCueSheetTrack()) {
					audioService.stop();
				}
				if (newTrack.isCueSheetTrack()) {
					audioService.play(track, newTrack.getCueSheetTrackStartTimeMillis().intValue());
				} else {
					audioService.play(track);
				}
				updateStaus("Playing");
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
			updateStaus("Playing");
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
		updateStaus("Stopped");
		updatePlayingState(false);
		SwingUtil.runOnEDT(() -> {
			mainWindow.disallowSeek();
			mainWindow.setCurrentPlayTimeDisplay(0, 0);
			mainWindow.updateNowPlaying(null);
		}, false);
	}

	@Override
	public void onDropFilesToQueue(int queuePosition, List<File> files) {
		new Thread(() -> playbackQueueFileImportService.importFilesIntoPlayQueue(queuePosition, files)).start();
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
	public void onWindowClose() {
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

		savePlayQueueColumnWidths();
		savePlayQueueContents();
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

	private void savePlayQueueColumnWidths() {
		LOGGER.info("Saving UI state.");
		try {
			TableColumnModel columnModel = mainWindow.getPlayQueueTable().getColumnModel();
			int[] columnWidths = new int[columnModel.getColumnCount()];
			for (int i = 0; i < columnModel.getColumnCount(); i++) {
				columnWidths[i] = columnModel.getColumn(i).getWidth();
			}
			preferencesService.setPlayQueueColumnWidths(columnWidths);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to store column width for playback queue table", e);
		}
	}

	@Override
	public void handleEvent(PlaybackEvent event) {
		switch (event.getType()) {
			case ERROR:
				LOGGER.log(Level.WARNING, "Playback error occurred: " + event.getErrorType() + " " + event.getError());
			break;
			case FINISH:
				// this.currentAudioFileInfo = null;
				// this.currentTrackInfo = null;
				onTrackFinished();
			break;
			case PROGRESS:
				if (currentAudioFileInfo != null) {
					Long playbackPositionMillis;
					long totalDurationSeconds;
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
						mainWindow.setCurrentPlayTimeDisplay(playbackPositionMillis / 1000, totalDurationSeconds);
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
			if (audioInfo.isSeekable()) {
				mainWindow.allowSeek(trackDurationSeconds * 10);
			} else {
				mainWindow.disallowSeek();
			}
			mainWindow.updateNowPlaying(trackInfo);
		}, false);
		if (scrobblingEnabled) {
			lastFmSetNowPlaying(this.currentTrackInfo);
		}
	}

	private void lastFmSetNowPlaying(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			try {
				Session session = getLastFMSession();
				if (session != null) {
					ScrobbleData scrobbleData = toScrobbleData(trackInfo);
					LOGGER.info("Setting LastFM now playing state to " + scrobbleData);
					Track.updateNowPlaying(scrobbleData, session);
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(true), false);
				} else {
					LOGGER.info("Skipping update now playing in LastFM - no session.");
					// TODO: enqueue for retry
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(false), false);
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to update now playing in LastFM", e);
				// TODO: enqueue for retry
				SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(false), false);
			}
		});
	}

	private void lastFmScrobble(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			try {
				Session session = getLastFMSession();
				if (session != null) {
					ScrobbleData scrobbleData = toScrobbleData(trackInfo);
					LOGGER.info("Scrobbling LastFM track played " + scrobbleData);
					Track.scrobble(scrobbleData, session);
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(true), false);
				} else {
					LOGGER.info("Skipping scrobbling track in LastFM - no session.");
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(false), false);
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to scrobble track in LastFM", e);
				SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(false), false);
			}
		});
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
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(true), false);
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to establish LastFM session", e);
					SwingUtil.runOnEDT(() -> mainWindow.updateLastFMStatus(false), false);
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
		SwingUtil.runOnEDT(() -> mainWindow.setPlayPauseButtonState(playing), false);
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
				LOGGER.log(Level.WARNING, "asd", e);
			}
		}).start();
	}

	@Override
	public void onBeforeUiSetVisible() {
		restorePlayQueueColumnWidths();
	}

	private void restorePlayQueueColumnWidths() {
		try {
			int[] playQueueColumnWidths = preferencesService.getPlayQueueColumnWidths();
			if (playQueueColumnWidths != null) {
				SwingUtil.runOnEDT(() -> {
					TableColumnModel columnModel = mainWindow.getPlayQueueTable().getColumnModel();
					for (int i = 0; i < columnModel.getColumnCount() && i < playQueueColumnWidths.length; i++) {
						columnModel.getColumn(i).setPreferredWidth(playQueueColumnWidths[i]);
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
}
