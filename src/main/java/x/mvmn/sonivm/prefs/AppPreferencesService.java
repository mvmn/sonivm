package x.mvmn.sonivm.prefs;

import java.security.GeneralSecurityException;

public interface AppPreferencesService {

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
}