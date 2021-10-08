package x.mvmn.sonivm.ui;

import java.io.File;
import java.util.List;

public interface SonivmController {

	public void onVolumeChange(int value);

	public void onSeek(int value);

	public void onPlay(File trackFile);

	public void onPause();

	public void onStop();

	public void onNextTrack(File trackFile);

	public void onPreviousTrack(File trackFile);

	public void onPlayTrack(File trackFile);

	public void onDropFilesToQueue(int queuePosition, List<File> files);

	public void onWindowClose();
}
