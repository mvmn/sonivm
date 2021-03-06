package x.mvmn.sonivm.prefs;

import java.awt.Dimension;
import java.awt.Point;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Set;

import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;
import x.mvmn.sonivm.util.Tuple4;

public interface PreferencesService {

	String getUsername();

	void setUsername(String value);

	String getPassword() throws GeneralSecurityException;

	void setPassword(String value) throws GeneralSecurityException;

	String getApiKey();

	void setApiKey(String value);

	String getApiSecret();

	void setApiSecret(String value);

	int getPercentageToScrobbleAt(int defaultVal);

	void setPercentageToScrobbleAt(int value);

	String getLookAndFeel();

	void setLookAndFeel(String value);

	int[] getPlayQueueColumnWidths();

	void setPlayQueueColumnWidths(int[] widths);

	int[] getPlayQueueColumnPositions();

	void setPlayQueueColumnPositions(int[] colPos);

	int[] getEqBands();

	void setEqBands(int[] eqBands);

	boolean isEqEnabled();

	void setEqEnabled(boolean eqEnabled);

	int getEqGain();

	void setEqGain(int eqGain);

	ShuffleMode getShuffleMode();

	void setShuffleMode(ShuffleMode value);

	RepeatMode getRepeatMode();

	void setRepeatMode(RepeatMode value);

	Set<String> getSupportedFileExtensions();

	void setSupportedFileExtensions(Collection<String> extensions);

	void saveMainWindowState(Tuple4<Boolean, String, Point, Dimension> windowState);

	Tuple4<Boolean, String, Point, Dimension> getMainWindowState();

	void saveEQWindowState(Tuple4<Boolean, String, Point, Dimension> windowState);

	Tuple4<Boolean, String, Point, Dimension> getEQWindowState();

	boolean isAutoStop();

	void setAutoStop(boolean autoStop);

	void saveRetroUIMainWindowState(Tuple4<Boolean, String, Point, Dimension> windowState);

	Tuple4<Boolean, String, Point, Dimension> getRetroUIMainWindowState();

	void saveRetroUIEqWindowState(Tuple4<Boolean, String, Point, Dimension> windowState);

	Tuple4<Boolean, String, Point, Dimension> getRetroUIEQWindowState();

	void saveRetroUIPlaylistWindowState(Tuple4<Boolean, String, Point, Dimension> windowState);

	Tuple4<Boolean, String, Point, Dimension> getRetroUIPlaylistWindowState();

	String getRetroUISkin();

	void setRetroUISkin(String value);

	int getRetroUIPlaylistSizeExtX();

	void setRetroUIPlaylistSizeExtX(int value);

	int getRetroUIPlaylistSizeExtY();

	void setRetroUIPlaylistSizeExtY(int value);

	int[] getRetroUIPlayQueueColumnWidths();

	void setRetroUIPlayQueueColumnWidths(int[] widths);

	int[] getRetroUIPlayQueueColumnPositions();

	void setRetroUIPlayQueueColumnPositions(int[] colPos);

	int getVolume();

	void setVolume(int volume);

	void setBalance(int balanceLR);

	int getBalance();
}