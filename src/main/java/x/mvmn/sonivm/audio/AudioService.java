package x.mvmn.sonivm.audio;

import java.io.File;

public interface AudioService {

	void setAudioDevice(String soundDeviceName);

	void play(File file);

	void play(File file, int startFromMilliseconds);

	void pause();

	void resume();

	void stop();

	void seek(int milliseconds);

	void shutdown();

	void setVolumePercentage(int volumePercent);

	void addPlaybackEventListener(PlaybackEventListener playbackEventListener);

	void removePlaybackEventListener(PlaybackEventListener playbackEventListener);

	void removeAllPlaybackEventListeners();

	boolean isPaused();

	boolean isPlaying();

	boolean isStopped();

	boolean isUseEqualizer();

	void setUseEqualizer(boolean useEq);
}
