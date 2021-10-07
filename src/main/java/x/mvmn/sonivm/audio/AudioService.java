package x.mvmn.sonivm.audio;

import java.io.File;
import java.util.Set;

public interface AudioService {

	Set<String> listAudioDevices();

	void setAudioDevice(String soundDeviceName);

	void play(File file);

	void pause();

	void resume();

	void stop();

	void seek(int milliseconds);

	void shutdown();

	void setVolumePercentage(int volumePercent);
}
