package x.mvmn.sonivm;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import x.mvmn.sonivm.audio.AudioServiceEventListener;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.impl.AudioDeviceOption;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;

public interface PlaybackController extends AudioServiceEventListener {

	void onVolumeChange(int value);

	void onSeek(int tenthOfSeconds);

	void onPlayPause();

	void onStop();

	void onNextTrack();

	void onPreviousTrack();

	void onTrackSelect(int queuePosition);

	void onQuit();

	void onDropFilesToQueue(int queuePosition, List<File> files, Consumer<String> importProgressListener);

	boolean onDropQueueRowsInsideQueue(int queuePosition, int firstRow, int lastRow);

	void onDeleteRowsFromQueue(int firstRow, int lastRow);
	
	void onQueueAdd();
	
	void onQueueRemove();

	void restorePlaybackState();

	void onSetAudioDevice(AudioDeviceOption audiodeviceoption);

	void onSetLookAndFeel(String lnf);

	void onShuffleModeSwitch(ShuffleMode selectedItem);

	void onRepeatModeSwitch(RepeatMode repeatMode);

	void onAutoStopChange(boolean autoStop);

	void onLastFMScrobblePercentageChange(int scrobblePercentageOption);

	void onLastFMCredsOrKeysUpdate();

	ShuffleMode getShuffleMode();

	RepeatMode getRepeatMode();

	boolean isAutoStop();

	int getTrackQueuePosition();

	void addPlaybackListener(PlaybackListener listener);

	int getCurrentTrackLengthSeconds();

	void onPlay();

	void onPause();

	PlaybackState getCurrentPlaybackState();

	int getCurrentVolumePercentage();

	void setBalance(int zeroToHundred);

	int getBalance();
}
