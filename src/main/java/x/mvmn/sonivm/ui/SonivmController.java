package x.mvmn.sonivm.ui;

import java.io.File;
import java.util.List;

import x.mvmn.sonivm.audio.PlaybackEventListener;

public interface SonivmController extends PlaybackEventListener {

	void onVolumeChange(int value);

	void onSeek(int value);

	void onPlayPause();

	void onStop();

	void onNextTrack();

	void onPreviousTrack();

	void onDropFilesToQueue(int queuePosition, List<File> files);

	boolean onDropQueueRowsInsideQueue(int queuePosition, int rowStart, int rowEnd);

	void onWindowClose();

	void onTrackSelect(int queuePosition);
}
