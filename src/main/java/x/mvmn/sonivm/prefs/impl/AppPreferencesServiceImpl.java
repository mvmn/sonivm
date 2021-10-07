package x.mvmn.sonivm.prefs.impl;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import org.springframework.stereotype.Service;

import x.mvmn.sonivm.SonivmLauncher;
import x.mvmn.sonivm.prefs.AppPreferencesService;
import x.mvmn.sonivm.util.EncryptionUtil;
import x.mvmn.sonivm.util.EncryptionUtil.KeyAndNonce;

@Service
public class AppPreferencesServiceImpl implements AppPreferencesService {
	private static final Logger LOGGER = Logger.getLogger(AppPreferencesServiceImpl.class.getCanonicalName());

	private static final String DEFAULT_KEY = "9e89b44de1ff37c5246ad0af18406454";
	private static final String DEFAULT_SECRET = "147320ea9b8930fe196a4231da50ada4";

	private static final String KEY_ENCPWD = "scrobblerencpwd";
	private static final String KEY_USER = "lastfmusername";
	private static final String KEY_PASS = "lastfmpassword";
	private static final String KEY_LASTFMAPIKEY = "lastfmapikey";
	private static final String KEY_LASTFMAPISECRET = "lastfmapisecret";
	private static final String KEY_PERCENTAGE_TO_SCROBBLE_AT = "scrobbleatpercent";

	private final Preferences prefs;
	private final KeyAndNonce keyAndNonce;

	public AppPreferencesServiceImpl() throws NoSuchAlgorithmException {
		this.prefs = getPreferences();
		String encpwd = prefs.get(KEY_ENCPWD, null);
		if (encpwd == null) {
			encpwd = EncryptionUtil.generateKeyAndNonce().serialize();
			prefs.put(KEY_ENCPWD, encpwd);
		}
		this.keyAndNonce = KeyAndNonce.deserialize(encpwd);
	}

	@Override
	public String getUsername() {
		return prefs.get(KEY_USER, null);
	}

	@Override
	public void setUsername(String value) {
		prefs.put(KEY_USER, value);
	}

	@Override
	public String getPassword() throws GeneralSecurityException {
		String password = prefs.get(KEY_PASS, null);

		if (password != null) {
			password = EncryptionUtil.decrypt(password, keyAndNonce);
		}
		return password;
	}

	@Override
	public void setPassword(String value) throws GeneralSecurityException {
		String password = EncryptionUtil.encrypt(value, keyAndNonce);
		prefs.put(KEY_PASS, password);
	}

	@Override
	public String getApiKey() {
		return prefs.get(KEY_LASTFMAPIKEY, DEFAULT_KEY);
	}

	@Override
	public void setApiKey(String value) {
		prefs.put(KEY_LASTFMAPIKEY, value);
	}

	@Override
	public String getApiSecret() {
		return prefs.get(KEY_LASTFMAPISECRET, DEFAULT_SECRET);
	}

	@Override
	public void setApiSecret(String value) {
		prefs.put(KEY_LASTFMAPISECRET, value);
	}

	protected Preferences getPreferences() {
		return Preferences.userNodeForPackage(SonivmLauncher.class);
	}

	@Override
	public void setPercentageToScrobbleAt(int value) {
		prefs.put(KEY_PERCENTAGE_TO_SCROBBLE_AT, String.valueOf(value));
	}

	@Override
	public int getPercentageToScrobbleAt(int defaultVal) {
		int result = defaultVal;
		String value = prefs.get(KEY_PERCENTAGE_TO_SCROBBLE_AT, String.valueOf(defaultVal));
		try {
			result = Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			LOGGER.warning("Number format exception for preference " + KEY_PERCENTAGE_TO_SCROBBLE_AT + " value " + value);
		}
		return result;
	}
}
