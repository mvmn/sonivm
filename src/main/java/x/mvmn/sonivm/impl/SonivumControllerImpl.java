package x.mvmn.sonivm.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.swing.table.TableColumnModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.model.IntRange;
import x.mvmn.sonivm.prefs.AppPreferencesService;
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
	private AppPreferencesService appPreferencesService;

	@Autowired
	private TagRetrievalService tagRetrievalService;

	private volatile AudioFileInfo currentAudioFileInfo;
	// private volatile PlaybackQueueEntry currentTrackInfo;
	private volatile ShuffleMode shuffleState = ShuffleMode.OFF;
	private volatile RepeatMode repeatState = RepeatMode.OFF;

	private final ExecutorService tagReadingTaskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

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
					switchToTrack(++currentTrackQueuePos);
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
		int queueSize = playbackQueueTableModel.getRowCount();
		if (trackQueuePosition >= queueSize) {
			doStop();
		} else {
			playbackQueueTableModel.setCurrentQueuePosition(trackQueuePosition);
			File track = playbackQueueTableModel.getRowValue(trackQueuePosition).getTargetFile();
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
		// this.currentTrackInfo = null;
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

			PlaybackQueueEntry queueEntry = PlaybackQueueEntry.builder().targetFile(track).build();
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
		savePlayQueueColumnWidths();

		audioService.stop();
		audioService.shutdown();
		tagReadingTaskExecutor.shutdownNow();
	}

	private void savePlayQueueColumnWidths() {
		try {
			TableColumnModel columnModel = mainWindow.getPlayQueueTable().getColumnModel();
			int[] columnWidths = new int[columnModel.getColumnCount()];
			for (int i = 0; i < columnModel.getColumnCount(); i++) {
				columnWidths[i] = columnModel.getColumn(i).getWidth();
			}
			appPreferencesService.setPlayQueueColumnWidths(columnWidths);
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
				}
			break;
			case START:
				AudioFileInfo audioInfo = event.getAudioMetadata();
				this.currentAudioFileInfo = audioInfo;
				PlaybackQueueEntry currentEntry = playbackQueueTableModel.getCurrentEntry();
				if (currentEntry != null) {
					currentEntry.setDuration(audioInfo.getDurationSeconds());
				}
				int queuePos = playbackQueueTableModel.getCurrentQueuePosition();
				PlaybackQueueEntry trackInfo = playbackQueueTableModel.getRowValue(queuePos);
				// this.currentTrackInfo = trackInfo;
				playbackQueueTableModel.fireTableRowsUpdated(queuePos, queuePos);
				SwingUtil.runOnEDT(() -> {
					if (audioInfo.isSeekable()) {
						mainWindow.allowSeek(audioInfo.getDurationSeconds().intValue() * 10);
					} else {
						mainWindow.disallowSeek();
					}
					mainWindow.updateNowPlaying(trackInfo);
				}, false);
			break;
			case DATALINE_CHANGE:
			// Control[] controls = event.getDataLineControls();
			// for (Control dataLineControl : controls) {
			// System.out.println(dataLineControl.getType() + ": " + dataLineControl);
			// }
			break;
		}
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
	public void onBeforeUiPack() {}

	@Override
	public void onBeforeUiSetVisible() {
		restorePlayQueueColumnWidths();
	}

	private void restorePlayQueueColumnWidths() {
		try {
			int[] playQueueColumnWidths = appPreferencesService.getPlayQueueColumnWidths();
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
				appPreferencesService.setLookAndFeel(lookAndFeelId);
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
}
