package x.mvmn.sonivm.impl;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.table.TableColumnModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.audio.AudioFileInfo;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.audio.PlaybackEvent;
import x.mvmn.sonivm.prefs.AppPreferencesService;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.SonivmMainWindow;
import x.mvmn.sonivm.ui.model.PlaybackQueueEntry;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;
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

	private volatile AudioFileInfo currentAudioFileInfo;

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

	@Override
	public void onNextTrack() {
		int trackCount = playbackQueueTableModel.getRowCount();
		int currentTrack = playbackQueueTableModel.getCurrentQueuePosition();
		if (currentTrack >= trackCount - 1) {
			doStop();
		} else if (currentTrack < 0) {
			tryPlayFromStartOfQueue();
		} else {
			switchToTrack(++currentTrack);
		}
	}

	@Override
	public void onPreviousTrack() {
		int currentTrack = playbackQueueTableModel.getCurrentQueuePosition();
		if (currentTrack > 0) {
			switchToTrack(--currentTrack);
		}
	}

	private void tryPlayFromStartOfQueue() {
		if (playbackQueueTableModel.getRowCount() > 0) {
			switchToTrack(0);
		} else {
			doStop();
		}
	}

	private void switchToTrack(int trackQueuePosition) {
		playbackQueueTableModel.setCurrentQueuePosition(trackQueuePosition);
		File track = playbackQueueTableModel.getRowValue(trackQueuePosition).getTargetFile();
		audioService.stop();
		audioService.play(track);
		updateStaus("Playing");
		updatePlayingState(true);
	}

	private void doStop() {
		audioService.stop();
		this.currentAudioFileInfo = null;
		playbackQueueTableModel.setCurrentQueuePosition(-1);
		updateStaus("Stopped");
		updatePlayingState(false);
		SwingUtil.runOnEDT(() -> {
			mainWindow.disallowSeek();
			mainWindow.setCurrentPlayTimeDisplay(0, 0);
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

			List<PlaybackQueueEntry> newEntries = Arrays
					.asList(PlaybackQueueEntry.builder().targetFile(track).title(track.getName()).build());
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
				this.currentAudioFileInfo = null;
				onNextTrack();
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
				playbackQueueTableModel.getCurrentEntry().setDuration(audioInfo.getDurationSeconds());
				int queuePos = playbackQueueTableModel.getCurrentQueuePosition();
				playbackQueueTableModel.fireTableRowsUpdated(queuePos, queuePos);
				SwingUtil.runOnEDT(() -> {
					if (audioInfo.isSeekable()) {
						mainWindow.allowSeek(audioInfo.getDurationSeconds().intValue() * 10);
					} else {
						mainWindow.disallowSeek();
					}
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
}
