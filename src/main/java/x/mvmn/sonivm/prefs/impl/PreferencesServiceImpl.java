package x.mvmn.sonivm.prefs.impl;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import x.mvmn.sonivm.SonivmLauncher;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.util.EncryptionUtil;
import x.mvmn.sonivm.util.EncryptionUtil.KeyAndNonce;

@Service
public class PreferencesServiceImpl implements PreferencesService {
	private static final Logger LOGGER = Logger.getLogger(PreferencesServiceImpl.class.getCanonicalName());

	private static final String DEFAULT_ENCRYPTION_KEY = "9e89b44de1ff37c5246ad0af18406454";
	private static final String DEFAULT_ENCRYPTION_SECRET = "147320ea9b8930fe196a4231da50ada4";

	private static final String KEY_ENCPWD = "scrobblerencpwd";
	private static final String KEY_USER = "lastfmusername";
	private static final String KEY_PASS = "lastfmpassword";
	private static final String KEY_LASTFMAPIKEY = "lastfmapikey";
	private static final String KEY_LASTFMAPISECRET = "lastfmapisecret";
	private static final String KEY_PERCENTAGE_TO_SCROBBLE_AT = "scrobbleatpercent";
	private static final String KEY_LOOK_AND_FEEL = "lookandfeel";
	private static final String KEY_PLAYQUEUE_COLUMN_WIDTHS = "playqueuecolumnwidths";
	private static final String KEY_PLAYQUEUE_COLUMN_POSITIONS = "playqueuecolumnpositions";

	private final Preferences prefs;
	private final KeyAndNonce keyAndNonce;

	public PreferencesServiceImpl() throws NoSuchAlgorithmException {
		this.prefs = getPreferences();
		String encpwd = prefs.get(KEY_ENCPWD, null);
		if (encpwd == null) {
			encpwd = EncryptionUtil.generateKeyAndNonce().serialize();
			prefs.put(KEY_ENCPWD, encpwd);
		}
		this.keyAndNonce = KeyAndNonce.deserialize(encpwd);
	}

	protected Preferences getPreferences() {
		return Preferences.userNodeForPackage(SonivmLauncher.class);
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
		return prefs.get(KEY_LASTFMAPIKEY, DEFAULT_ENCRYPTION_KEY);
	}

	@Override
	public void setApiKey(String value) {
		prefs.put(KEY_LASTFMAPIKEY, value);
	}

	@Override
	public String getApiSecret() {
		return prefs.get(KEY_LASTFMAPISECRET, DEFAULT_ENCRYPTION_SECRET);
	}

	@Override
	public void setApiSecret(String value) {
		prefs.put(KEY_LASTFMAPISECRET, value);
	}

	@Override
	public String getLookAndFeel() {
		return prefs.get(KEY_LOOK_AND_FEEL, null);
	}

	@Override
	public void setLookAndFeel(String value) {
		prefs.put(KEY_LOOK_AND_FEEL, value);
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

	@Override
	public int[] getPlayQueueColumnWidths() {
		return getIntArrayProperty(KEY_PLAYQUEUE_COLUMN_WIDTHS, null);
	}

	@Override
	public void setPlayQueueColumnWidths(int[] widths) {
		setIntArrayProperty(KEY_PLAYQUEUE_COLUMN_WIDTHS, widths);
	}

	@Override
	public int[] getPlayQueueColumnPositions() {
		return getIntArrayProperty(KEY_PLAYQUEUE_COLUMN_POSITIONS, null);
	}

	@Override
	public void setPlayQueueColumnPositions(int[] colPos) {
		setIntArrayProperty(KEY_PLAYQUEUE_COLUMN_POSITIONS, colPos);
	}

	protected int[] getIntArrayProperty(String prefKey, String defaultVal) {
		String value = prefs.get(prefKey, defaultVal);
		if (value == null) {
			return null;
		} else {
			return Stream.of(value.split(",")).mapToInt(Integer::parseInt).toArray();
		}
	}

	protected void setIntArrayProperty(String prefKey, int[] values) {
		prefs.put(prefKey, IntStream.of(values).mapToObj(String::valueOf).collect(Collectors.joining(",")));
	}
}
