package x.mvmn.sonivm.prefs;

import java.awt.Dimension;
import java.awt.Point;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Set;

import x.mvmn.sonivm.ui.model.RepeatMode;
import x.mvmn.sonivm.ui.model.ShuffleMode;
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
}