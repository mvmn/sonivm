package x.mvmn.sonivm.impl;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.ui.SonivmController;
import x.mvmn.sonivm.ui.model.PlaybackQueueTableModel;

@Component
public class SonivumControllerImpl implements SonivmController {

	private static final Set<String> supportedExtensions = Stream.of("flac", "ogg", "mp3", "m4a", "cue").collect(Collectors.toSet());

	@Autowired
	private AudioService audioService;

	@Autowired
	private PlaybackQueueTableModel playbackQueueTableModel;

	@Override
	public void onVolumeChange(int value) {
		audioService.setVolumePercentage(value);
	}

	@Override
	public void onSeek(int value) {
		audioService.seek(value);
	}

	@Override
	public void onPlay(File trackFile) {
		audioService.play(trackFile);
	}

	@Override
	public void onPause() {
		audioService.pause();
	}

	@Override
	public void onStop() {
		audioService.stop();
	}

	@Override
	public void onNextTrack(File trackFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPreviousTrack(File trackFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPlayTrack(File trackFile) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDropFilesToQueue(int queuePosition, List<File> files) {
		for (File track : files) {
			if (!track.exists()) {
				return;
			}
			if (track.isDirectory()) {
				onDropFilesToQueue(queuePosition,
						Stream.of(track.listFiles())
								.sorted(Comparator.comparing(File::getName))
								.filter(file -> file.isDirectory() || (file.getName().indexOf(".") > 0 && supportedExtensions
										.contains(file.getName().substring(file.getName().lastIndexOf(".") + 1).toLowerCase())))
								.collect(Collectors.toList()));
				return;
			}
			if (track.getName().toLowerCase().endsWith(".cue")) {
				// parse CUE
				return;
			}

			Object[] rowData = new Object[] { null, "", track.getName(), "", "", "", "", "" };
			if (queuePosition >= 0) {
				playbackQueueTableModel.insertRow(queuePosition, rowData);
			} else {
				playbackQueueTableModel.addRow(rowData);
			}
		}
	}

	@Override
	public void onWindowClose() {
		audioService.stop();
		audioService.shutdown();
	}
}
