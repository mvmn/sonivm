package x.mvmn.sonivm.impl;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import x.mvmn.sonivm.PlaybackController;
import x.mvmn.sonivm.PlaybackListener;
import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.AudioServiceEvent;
import x.mvmn.sonivm.audio.AudioServiceEvent.ErrorType;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.lastfm.LastFMScrobblingService;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntryCompareBiPredicate;
import x.mvmn.sonivm.playqueue.PlaybackQueueFileImportService;
import x.mvmn.sonivm.playqueue.PlaybackQueueService;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.IntRange;

@Component
public class PlaybackControllerImpl implements PlaybackController {

	private static final Logger LOGGER = Logger.getLogger(PlaybackControllerImpl.class.getSimpleName());

	@Autowired
	private AudioService audioService;

	@Autowired
	private PlaybackQueueService playbackQueueService;

	@Autowired
	private PlaybackQueueFileImportService playbackQueueFileImportService;

	@Autowired
	private PreferencesService preferencesService;

	@Autowired
	private LastFMScrobblingService lastFMScrobblingService;

	@Autowired
	private SonivmEqualizerService equalizerService;

	// State
	private volatile AudioFileInfo currentAudioFileInfo;
	private volatile PlaybackQueueEntry currentTrackInfo;

	@Getter
	private volatile ShuffleMode shuffleMode = ShuffleMode.OFF;
	@Getter
	private volatile RepeatMode repeatMode = RepeatMode.OFF;
	@Getter
	private volatile boolean autoStop = false;

	private volatile AtomicLong currentTrackTotalListeningTimeMillisec = new AtomicLong(0L);
	private volatile int scrobbleThresholdPercent = 70;
	private volatile boolean scrobblingEnabled = false;
	private volatile boolean currentTrackScrobbled = true;
	private volatile PlaybackState currentPlaybackState = PlaybackState.STOPPED;

	private final CopyOnWriteArrayList<PlaybackListener> listeners = new CopyOnWriteArrayList<>();

	@PostConstruct
	public void initPostConstruct() {
		audioService.addPlaybackEventListener(this);
	}

	@Override
	public void onVolumeChange(int value) {
		audioService.setVolumePercentage(value);
	}

	@Override
	public void onSeek(int tenthOfSeconds) {
		if (currentTrackInfo != null) {
			if (currentTrackInfo.isCueSheetTrack()) {
				audioService.seek(currentTrackInfo.getCueSheetTrackStartTimeMillis().intValue() + tenthOfSeconds * 100);
			} else {
				audioService.seek(tenthOfSeconds * 100);
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
		if (autoStop) {
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
		ShuffleMode shuffleState = this.shuffleMode;
		RepeatMode repeatState = this.repeatMode;

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
		this.currentPlaybackState = PlaybackState.STOPPED;
		audioService.stop();
		this.currentAudioFileInfo = null;
		this.currentTrackInfo = null;
		playbackQueueService.setCurrentQueuePosition(-1);
		listeners.forEach(listener -> listener.onPlaybackStateChange(PlaybackState.STOPPED));
	}

	@Override
	public void onDropFilesToQueue(int queuePosition, List<File> files, Consumer<String> importProgressListener) {
		new Thread(() -> playbackQueueFileImportService.importFilesIntoPlayQueue(queuePosition, files, importProgressListener)).start();
	}

	@Override
	public boolean onDropQueueRowsInsideQueue(int insertPosition, int firstRow, int lastRow) {
		int rowCount = lastRow - firstRow + 1;
		if (insertPosition > lastRow || insertPosition < firstRow) {
			playbackQueueService.moveRows(insertPosition, firstRow, lastRow);

			if (lastRow < insertPosition) {
				insertPosition -= rowCount;
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onQuit() {
		LOGGER.info("Shutting down audio service.");
		audioService.stop();
		audioService.shutdown();

		savePlaybackState();
		savePlayQueueContents();
		saveEqState();
	}

	protected void savePlaybackState() {
		playbackQueueFileImportService.shutdown();

		try {
			this.preferencesService.setShuffleMode(shuffleMode);
			this.preferencesService.setRepeatMode(repeatMode);
			this.preferencesService.setAutoStop(autoStop);
			this.preferencesService.setVolume(audioService.getVolumePercentage());
			this.preferencesService.setBalance(audioService.getBalanceLR());
		} catch (Throwable t) {
			LOGGER.log(Level.WARNING, "Failed to store shuffle/repeat preferences", t);
		}
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

	@Override
	public void handleEvent(AudioServiceEvent event) {
		switch (event.getType()) {
			case ERROR:
				LOGGER.log(Level.WARNING, "Playback error occurred: " + event.getErrorType() + " " + event.getError());
				listeners.forEach(listener -> listener.onPlaybackError("Playback error " + event.getErrorType() + " " + event.getError()));
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
					long playbackPositionMillis;
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

					listeners.forEach(listener -> listener.onPlaybackProgress(playbackPositionMillis, totalDurationSeconds));
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
			default:
		}
	}

	private void handleStartTrackPlay(AudioFileInfo audioInfo) {
		this.currentTrackTotalListeningTimeMillisec.set(0L);
		this.currentTrackScrobbled = false;
		this.currentAudioFileInfo = audioInfo;
		PlaybackQueueEntry currentEntry = playbackQueueService.getCurrentEntry();
		this.currentTrackInfo = currentEntry;
		if (!currentEntry.isCueSheetTrack()) {
			currentEntry.setDuration(audioInfo.getDurationSeconds());
			playbackQueueService.signalUpdateInRow(playbackQueueService.getCurrentQueuePosition());
		}
		listeners.forEach(listener -> listener.onPlaybackStart(audioInfo, currentEntry));
		if (scrobblingEnabled) {
			lastFmSetNowPlaying(this.currentTrackInfo);
		}
	}

	private void lastFmSetNowPlaying(PlaybackQueueEntry trackInfo) {
		lastFMScrobblingService.setNowPlayingTrack(trackInfo);
	}

	private void lastFmScrobble(PlaybackQueueEntry trackInfo) {
		lastFMScrobblingService.scrobbleTrack(trackInfo);
	}

	@Override
	public void onDeleteRowsFromQueue(int firstRow, int lastRow) {
		playbackQueueService.deleteRows(firstRow, lastRow + 1);
	}

	private void updatePlayingState(boolean playing) {
		currentPlaybackState = playing ? PlaybackState.PLAYING : PlaybackState.PAUSED;
		listeners.forEach(listener -> listener.onPlaybackStateChange(playing ? PlaybackState.PLAYING : PlaybackState.PAUSED));
	}

	@Override
	public void restorePlaybackState() {
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
			this.shuffleMode = shuffleMode;

			RepeatMode repeatMode = this.preferencesService.getRepeatMode();
			this.repeatMode = repeatMode;

			this.audioService.setVolumePercentage(this.preferencesService.getVolume());
			this.audioService.setBalanceLR(this.preferencesService.getBalance());
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to read shuffle/repeat/autostop preferences", e);
		}
		restoreEqState();
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
		this.repeatMode = repeatMode;
	}

	@Override
	public void onShuffleModeSwitch(ShuffleMode shuffleMode) {
		this.shuffleMode = shuffleMode;
	}

	@Override
	public void onAutoStopChange(boolean autoStop) {
		this.autoStop = autoStop;
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
		this.lastFMScrobblingService.onLastFMPreferencesChange();
	}

	private void restoreEqState() {
		try {
			LOGGER.info("Restoring EQ state");
			EqualizerState eqState = EqualizerState.builder()
					.enabled(preferencesService.isEqEnabled())
					.gain(preferencesService.getEqGain())
					.bands(preferencesService.getEqBands())
					.build();
			equalizerService.setState(eqState);
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

	@Override
	public int getTrackQueuePosition() {
		return playbackQueueService.getCurrentQueuePosition();
	}

	@Override
	public void addPlaybackListener(PlaybackListener listener) {
		listeners.add(listener);
	}

	@Override
	public int getCurrentTrackLengthSeconds() {
		return currentTrackInfo != null ? currentTrackInfo.getDuration() : 0;
	}

	@Override
	public void onPlay() {
		if (audioService.isPaused()) {
			audioService.resume();
			updatePlayingState(true);
		} else if (audioService.isStopped()) {
			tryPlayFromStartOfQueue();
		}
	}

	@Override
	public void onPause() {
		if (audioService.isPlaying() && !audioService.isPaused()) {
			audioService.pause();
			updatePlayingState(false);
		}
	}

	@Override
	public PlaybackState getCurrentPlaybackState() {
		return currentPlaybackState;
	}

	@Override
	public int getCurrentVolumePercentage() {
		return audioService.getVolumePercentage();
	}

	@Override
	public void setBalance(int zeroToHundred) {
		audioService.setBalanceLR(zeroToHundred);
	}

	@Override
	public int getBalance() {
		return audioService.getBalanceLR();
	}
}
