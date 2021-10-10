package x.mvmn.sonivm.impl;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.tag.TagRetrievalService;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry.TrackMetadata;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
import x.mvmn.sonivm.ui.model.RepeatMode;
import x.mvmn.sonivm.ui.model.ShuffleMode;
import x.mvmn.sonivm.util.ui.swing.SwingUtil;

@Component
public class SonivumControllerImpl implements SonivmController {

	private static final Logger LOGGER = Logger.getLogger(SonivumControllerImpl.class.getSimpleName());

	private static final Set<String> supportedExtensions = Stream.of("flac", "ogg", "mp3", "m4a", "cue").collect(Collectors.toSet());

	@Autowired
	private AudioService audioService;

	@Autowired
	private PlaybackQueueTableModel playbackQueueTableModel;

	@Autowired
	private SonivmMainWindow mainWindow;

	@Autowired
	private PreferencesService preferencesService;

	@Autowired
	private TagRetrievalService tagRetrievalService;

	private volatile AudioFileInfo currentAudioFileInfo;
	private volatile PlaybackQueueEntry currentTrackInfo;
	private volatile ShuffleMode shuffleState = ShuffleMode.OFF;
	private volatile RepeatMode repeatState = RepeatMode.OFF;

	private volatile AtomicLong currentTrackTotalListeningTimeSeconds = new AtomicLong(0L);
	private volatile double scrobbleThreshold = 0.7d;
	private volatile boolean scrobblingEnabled = false;
	private volatile boolean currentTrackScrobbled = true;

	private final ExecutorService tagReadingTaskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
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
		audioService.seek(value * 100);
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
		int trackCount = playbackQueueTableModel.getRowCount();
		int currentTrackQueuePos = playbackQueueTableModel.getCurrentQueuePosition();
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
				PlaybackQueueEntry currentTrack = playbackQueueTableModel.getRowValue(currentTrackQueuePos);
				switch (shuffleState) {
					case ALBUM: {
						int[] matchingTrackIndexes = findAllTracksByProperty(currentTrack.getAlbum(), false);
						if (matchingTrackIndexes.length < 1) {
							tryPlayFromStartOfQueue();
						} else {
							switchToTrack(
									matchingTrackIndexes[new Random(System.currentTimeMillis()).nextInt(matchingTrackIndexes.length)]);
						}
						return;
					}
					case ARTIST: {
						int[] matchingTrackIndexes = findAllTracksByProperty(currentTrack.getArtist(), true);
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
						int shuffleRangeStart = 0;
						int shuffleRangeEnd = trackCount;
						int positionToPlay = shuffleRangeStart
								+ new Random(System.currentTimeMillis()).nextInt(shuffleRangeEnd - shuffleRangeStart + 1);
						switchToTrack(positionToPlay);
						return;
					}
				}
			} else {
				if (RepeatMode.OFF != repeatState) {
					int endOfRange = trackCount - 1;
					int startOfRange = 0;
					switch (repeatState) {
						case ALBUM:
							IntRange albumRange = detectTrackRange(currentTrackQueuePos, false);
							startOfRange = albumRange.getFrom();
							endOfRange = albumRange.getTo();
						break;
						case ARTIST:
							IntRange artistRange = detectTrackRange(currentTrackQueuePos, true);
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
					switchToTrack(++currentTrackQueuePos, false);
				}
			}
		} else {
			doStop();
		}
	}

	private int[] findAllTracksByProperty(String value, boolean useArtist) {
		if (value == null) {
			value = "";
		} else {
			value = value.trim();
		}
		List<Integer> result = new ArrayList<>();
		int rows = playbackQueueTableModel.getRowCount();
		for (int i = 0; i < rows; i++) {
			PlaybackQueueEntry queueEntry = playbackQueueTableModel.getRowValue(i);
			String valB = useArtist ? queueEntry.getArtist() : queueEntry.getAlbum();
			if (valB == null) {
				valB = "";
			} else {
				valB = valB.trim();
			}
			if (trackPropertiesEqual(value, valB)) {
				result.add(i);
			}
		}
		return result.stream().mapToInt(Integer::intValue).toArray();
	}

	@Override
	public void onPreviousTrack() {
		int currentTrack = playbackQueueTableModel.getCurrentQueuePosition();
		if (currentTrack > 0) {
			switchToTrack(--currentTrack);
		}
	}

	private IntRange detectTrackRange(int currentPosition, boolean byArtist) {
		int trackCount = playbackQueueTableModel.getRowCount();
		if (trackCount > 0) {
			PlaybackQueueEntry currentTrack = playbackQueueTableModel.getRowValue(currentPosition);
			int start = currentPosition;
			while (start > 0) {
				PlaybackQueueEntry prevTrack = playbackQueueTableModel.getRowValue(start - 1);
				if (!propertyEquals(currentTrack, prevTrack, byArtist)) {
					break;
				} else {
					start--;
				}
			}
			int end = currentPosition;
			while (end < trackCount - 1) {
				PlaybackQueueEntry nextTrack = playbackQueueTableModel.getRowValue(end + 1);
				if (!propertyEquals(currentTrack, nextTrack, byArtist)) {
					break;
				} else {
					end++;
				}
			}
			return new IntRange(start, end);
		} else {
			return new IntRange(-1, -1);
		}
	}

	private boolean propertyEquals(PlaybackQueueEntry entryA, PlaybackQueueEntry entryB, boolean byArtist) {
		String valA = byArtist ? entryA.getArtist() : entryA.getAlbum();
		String valB = byArtist ? entryB.getArtist() : entryB.getAlbum();
		return trackPropertiesEqual(valA, valB);
	}

	private boolean trackPropertiesEqual(String valA, String valB) {
		if (valA == null) {
			valA = "";
		}
		if (valB == null) {
			valB = "";
		}
		return valA.trim().equalsIgnoreCase(valB.trim());
	}

	private void tryPlayFromStartOfQueue() {
		if (playbackQueueTableModel.getRowCount() > 0) {
			switchToTrack(0);
		} else {
			doStop();
		}
	}

	private void switchToTrack(int trackQueuePosition) {
		switchToTrack(trackQueuePosition, true);
	}

	private void switchToTrack(int trackQueuePosition, boolean stopCurrent) {
		int queueSize = playbackQueueTableModel.getRowCount();
		if (trackQueuePosition >= queueSize) {
			doStop();
		} else {
			playbackQueueTableModel.setCurrentQueuePosition(trackQueuePosition);
			File track = new File(playbackQueueTableModel.getRowValue(trackQueuePosition).getTargetFileFullPath());
			audioService.stop();
			audioService.play(track);
			updateStaus("Playing");
			updatePlayingState(true);
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
		playbackQueueTableModel.setCurrentQueuePosition(-1);
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
		addFilesToQueue(queuePosition, files);
	}

	private void addFilesToQueue(int queuePosition, List<File> files) {
		// FIXME: move off EDT
		for (File track : files) {
			if (!track.exists()) {
				continue;
			}
			if (track.isDirectory()) {
				onDropFilesToQueue(queuePosition, Stream.of(track.listFiles())
						.sorted(queuePosition >= 0 ? Comparator.comparing(File::getName).reversed() : Comparator.comparing(File::getName))
						.filter(file -> file.isDirectory() || (file.getName().indexOf(".") > 0 && supportedExtensions
								.contains(file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase())))
						.collect(Collectors.toList()));
				continue;
			}
			if (track.getName().toLowerCase().endsWith(".cue")) {
				// TODO: parse CUE
				continue;
			}

			PlaybackQueueEntry queueEntry = PlaybackQueueEntry.builder()
					.targetFileFullPath(track.getAbsolutePath())
					.targetFileName(track.getName())
					.build();
			tagReadingTaskExecutor.submit(() -> {
				try {
					TrackMetadata meta = tagRetrievalService.getAudioFileMetadata(track);
					queueEntry.setTrackMetadata(meta);
					playbackQueueTableModel.signalUpdateInTrackInfo(queueEntry);
				} catch (Throwable t) {
					LOGGER.log(Level.WARNING, "Failed to read tags for file " + track.getAbsolutePath(), t);
				}
			});

			List<PlaybackQueueEntry> newEntries = Arrays.asList(queueEntry);
			if (queuePosition >= 0) {
				playbackQueueTableModel.addRows(queuePosition, newEntries);
			} else {
				playbackQueueTableModel.addRows(newEntries);
			}
		}
	}

	@Override
	public boolean onDropQueueRowsInsideQueue(int insertPosition, int firstRow, int lastRow) {
		int rowCount = lastRow - firstRow + 1;
		if (insertPosition > lastRow || insertPosition < firstRow) {
			playbackQueueTableModel.moveRows(insertPosition, firstRow, lastRow);

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
		audioService.stop();
		audioService.shutdown();
		tagReadingTaskExecutor.shutdownNow();
		lastFmScrobbleTaskExecutor.shutdown();

		savePlayQueueColumnWidths();
		savePlayQueueContents();
	}

	private void savePlayQueueContents() {
		try {
			LOGGER.info("Storing play queue.");
			new ObjectMapper().writeValue(getPlayQueueStorageFile(), this.playbackQueueTableModel.getCopyOfQueue());
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
				this.playbackQueueTableModel.clearQueue();
				this.playbackQueueTableModel.addRows(queueEntries);
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
					Long playbackPositionMillis = event.getPlaybackPositionMilliseconds();
					int seekSliderNewPosition = (int) (playbackPositionMillis / 100);
					long totalDurationSeconds = currentAudioFileInfo.getDurationSeconds();
					SwingUtil.runOnEDT(() -> {
						mainWindow.updateSeekSliderPosition(seekSliderNewPosition);
						mainWindow.setCurrentPlayTimeDisplay(playbackPositionMillis / 1000, totalDurationSeconds);
					}, false);
					long totalListenTimeSec = this.currentTrackTotalListeningTimeSeconds
							.addAndGet(event.getPlaybackDeltaMilliseconds() / 1000);
					if (scrobblingEnabled && !currentTrackScrobbled
							&& (double) totalListenTimeSec / (double) totalDurationSeconds > scrobbleThreshold) {
						System.out.println(String.format("Listen time %s, duration %s, percent %s, threshold %s", totalListenTimeSec,
								totalDurationSeconds, (double) totalListenTimeSec / (double) totalDurationSeconds, scrobbleThreshold));
						lastFmScrobble(this.currentTrackInfo);
						currentTrackScrobbled = true;
					}
				}
			break;
			case START:
				this.currentTrackTotalListeningTimeSeconds.set(0L);
				this.currentTrackScrobbled = false;
				AudioFileInfo audioInfo = event.getAudioMetadata();
				this.currentAudioFileInfo = audioInfo;
				PlaybackQueueEntry currentEntry = playbackQueueTableModel.getCurrentEntry();
				if (currentEntry != null) {
					currentEntry.setDuration(audioInfo.getDurationSeconds());
				}
				int queuePos = playbackQueueTableModel.getCurrentQueuePosition();
				PlaybackQueueEntry trackInfo = playbackQueueTableModel.getRowValue(queuePos);
				this.currentTrackInfo = trackInfo;
				playbackQueueTableModel.fireTableRowsUpdated(queuePos, queuePos);
				SwingUtil.runOnEDT(() -> {
					if (audioInfo.isSeekable()) {
						mainWindow.allowSeek(audioInfo.getDurationSeconds().intValue() * 10);
					} else {
						mainWindow.disallowSeek();
					}
					mainWindow.updateNowPlaying(trackInfo);
				}, false);
				if (scrobblingEnabled) {
					lastFmSetNowPlaying(this.currentTrackInfo);
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

	private void lastFmSetNowPlaying(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			try {
				Session session = getLastFMSession();
				if (session != null) {
					ScrobbleData scrobbleData = toScrobbleData(trackInfo);
					LOGGER.info("Setting LastFM now playing state to " + scrobbleData);
					Track.updateNowPlaying(scrobbleData, session);
				} else {
					LOGGER.info("Skipping update now playing in LastFM - no session.");
					// TODO: enqueue for retry
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to update now playing in LastFM", e);
				// TODO: enqueue for retry
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
				} else {
					LOGGER.info("Skipping scrobbling track in LastFM - no session.");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to scrobble track in LastFM", e);
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
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to establish LastFM session", e);
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
		playbackQueueTableModel.deleteRows(firstRow, lastRow + 1);
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
				this.scrobbleThreshold = (double) scrobblePercent / 100.0d;
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
		this.scrobbleThreshold = (double) scrobblePercentageOption / 100.0d;
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
