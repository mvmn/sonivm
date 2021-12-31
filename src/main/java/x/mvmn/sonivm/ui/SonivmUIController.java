package x.mvmn.sonivm.ui;

import java.io.File;
import java.util.List;

import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;

public interface SonivmUIController {

	void onQuit();

	void onShowMainWindow();

	void onShowEQWindow();

	void onShowRetroUIMainWindow();

	void onShowRetroUIEQWinwod();

	void onShowRetroUIPlaylistWindow();

	void onPreviousTrack();

	void onPlayPause();

	void onStop();

	void onNextTrack();

	boolean onDropQueueRowsInsideQueue(int rowCount, int from, int to);

	void onDropFilesToQueue(int row, List<File> fileList);

	void onAutoStopChange(boolean selected);

	void onShuffleModeSwitch(ShuffleMode selectedItem);

	void onRepeatModeSwitch(RepeatMode selectedItem);

	void onVolumeChange(int value);

	void onSeek(int val);

	void toggleShowEqualizer();

	void onTrackSelect(int row);

	void onDeleteRowsFromQueue(int start, int end);

	void scrollToTrack(int trackQueuePosition);
}
