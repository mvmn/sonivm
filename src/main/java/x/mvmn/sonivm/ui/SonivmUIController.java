package x.mvmn.sonivm.ui;

import java.awt.Component;
import java.io.File;
import java.util.List;

import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;

public interface SonivmUIController {

	void onQuit();

	void onShowMainWindow();

	void onShowEQWindow();

	void onShowRetroUIMainWindow();

	void onShowRetroUIEQWindow();

	void onShowRetroUIPlaylistWindow();

	void onPreviousTrack();

	void onPlayPause();

	void onPlay();

	void onPause();

	void onStop();

	void onNextTrack();

	boolean onDropQueueRowsInsideQueue(int rowCount, int from, int to);

	void onDropFilesToQueue(int row, List<File> fileList);

	void onAutoStopChange(boolean selected);

	void onShuffleModeSwitch(ShuffleMode selectedItem);

	void onRepeatModeSwitch(RepeatMode selectedItem);

	void onVolumeChange(int value);

	void toggleShowEqualizer();

	void onTrackSelect(int row);

	void onDeleteRowsFromQueue(int start, int end);

	void scrollToTrack(int trackQueuePosition);

	void scrollToNowPlaying();

	void onRetroUiSkinChange(String skinFileName);

	void onRefreshSkinsList();

	void onSeek(int tenthOfSecond);

	void onSeek(double ratio);

	void onImportSkins();

	void setEQEnabled(boolean enabled);

	void showPresetsMenu(Component parentComponent, int x, int y);

	void onEQGainChange(double value);

	void onEQBandChange(int bandNumber, double value);

	void onEQSavePreset(String presetName, EqualizerPreset preset);

	void onEQImportPreset(File selectedFile);

	void onEQExportPreset(File selectedFile, String presetName, EqualizerPreset preset);

	void onEQReset();

	void onBalanceChange(double sliderPositionRatio);

	void retroUISwitchTimeDisplay();

	void onRetroUISearchTextChange();

	void onSearchNextMatch();

	void onSearchPreviousMatch();

	void showSkinBrowser();
}
