package x.mvmn.sonivm.ui;

import java.io.File;
import java.util.List;

import x.mvmn.sonivm.audio.PlaybackEventListener;
import x.mvmn.sonivm.ui.model.AudioDeviceOption;

public interface SonivmController extends PlaybackEventListener {

	void onVolumeChange(int value);

	void onSeek(int value);

	void onPlayPause();

	void onStop();

	void onNextTrack();

	void onPreviousTrack();

	void onTrackSelect(int queuePosition);

	void onWindowClose();

	void onDropFilesToQueue(int queuePosition, List<File> files);

	boolean onDropQueueRowsInsideQueue(int queuePosition, int firstRow, int lastRow);

	void onDeleteRowsFromQueue(int firstRow, int lastRow);

	void onBeforeUiPack();

	void onBeforeUiSetVisible();

	void onSetAudioDevice(AudioDeviceOption audiodeviceoption);

	void onSetLookAndFeel(String lnf);
}
