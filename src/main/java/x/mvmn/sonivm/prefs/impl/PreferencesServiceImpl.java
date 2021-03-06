package x.mvmn.sonivm.prefs.impl;

import java.awt.Dimension;
import java.awt.Point;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import x.mvmn.sonivm.Sonivm;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.util.EncryptionUtil;
import x.mvmn.sonivm.util.EncryptionUtil.KeyAndNonce;
import x.mvmn.sonivm.util.Tuple4;

@Service
public class PreferencesServiceImpl implements PreferencesService {
	private static final Logger LOGGER = Logger.getLogger(PreferencesServiceImpl.class.getCanonicalName());

	private static final String STRING_LIST_VALUES_SEPARATOR = "\r\n%SONIVMPREFSEPARATOR%\r\n";

	private static final String DEFAULT_ENCRYPTION_KEY = "9e89b44de1ff37c5246ad0af18406454";
	private static final String DEFAULT_ENCRYPTION_SECRET = "147320ea9b8930fe196a4231da50ada4";

	private static final String KEY_ENCPWD = "scrobblerencpwd";

	private static final String KEY_LASTFM_USER = "lastfmusername";
	private static final String KEY_LASTFM_PASS = "lastfmpassword";
	private static final String KEY_LASTFMAPIKEY = "lastfmapikey";
	private static final String KEY_LASTFMAPISECRET = "lastfmapisecret";
	private static final String KEY_PERCENTAGE_TO_SCROBBLE_AT = "scrobbleatpercent";

	private static final String KEY_LOOK_AND_FEEL = "lookandfeel";

	private static final String KEY_PLAYQUEUE_COLUMN_WIDTHS = "playqueuecolumnwidths";
	private static final String KEY_PLAYQUEUE_COLUMN_POSITIONS = "playqueuecolumnpositions";

	private static final String KEY_RETROUI_PLAYQUEUE_COLUMN_WIDTHS = "retrouiplayqueuecolumnwidths";
	private static final String KEY_RETROUI_PLAYQUEUE_COLUMN_POSITIONS = "retrouiplayqueuecolumnpositions";

	private static final String KEY_SHUFFLE_MODE = "shufflemode";
	private static final String KEY_REPEAT_MODE = "repeatmode";
	private static final String KEY_AUTOSTOP = "autostop";

	private static final String KEY_EQ_ENABLED = "eqenabled";
	private static final String KEY_EQ_GAIN = "eqgain";
	private static final String KEY_EQ_BANDS_VALUES = "eqbandsvalues";

	private static final String KEY_SUPPORTED_FILE_EXTENSIONS = "supportedfileextensions";
	private static final String DEFAULT_SUPPORTED_FILE_EXTENSIONS = Stream.of("cue", "flac", "ogg", "mp3", "m4a", "wav")
			.collect(Collectors.joining(STRING_LIST_VALUES_SEPARATOR));

	private static final String KEY_MAIN_WINDOW_STATE = "mainwindowstate";
	private static final String KEY_EQ_WINDOW_STATE = "eqwindowstate";

	private static final String KEY_RETROUI_MAIN_WINDOW_STATE = "retrouimainwindowstate";
	private static final String KEY_RETROUI_EQ_WINDOW_STATE = "retrouieqwindowstate";
	private static final String KEY_RETROUI_PLAYLIST_WINDOW_STATE = "retrouiplwindowstate";
	private static final String KEY_RETROUI_PLAYLIST_WINDOW_SIZE_EXT_X = "retrouiplextx";
	private static final String KEY_RETROUI_PLAYLIST_WINDOW_SIZE_EXT_Y = "retrouiplexty";
	private static final String KEY_RETROUI_SKIN = "retrouiskin";
	private static final String KEY_VOLUME = "volume";
	private static final String KEY_BALANCE = "balance";

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
		return Preferences.userNodeForPackage(Sonivm.class);
	}

	@Override
	public String getUsername() {
		return prefs.get(KEY_LASTFM_USER, null);
	}

	@Override
	public void setUsername(String value) {
		prefs.put(KEY_LASTFM_USER, value);
	}

	@Override
	public String getPassword() throws GeneralSecurityException {
		String password = prefs.get(KEY_LASTFM_PASS, null);

		if (password != null) {
			password = EncryptionUtil.decrypt(password, keyAndNonce);
		}
		return password;
	}

	@Override
	public void setPassword(String value) throws GeneralSecurityException {
		String password = EncryptionUtil.encrypt(value, keyAndNonce);
		prefs.put(KEY_LASTFM_PASS, password);
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

	@Override
	public int[] getRetroUIPlayQueueColumnWidths() {
		return getIntArrayProperty(KEY_RETROUI_PLAYQUEUE_COLUMN_WIDTHS, null);
	}

	@Override
	public void setRetroUIPlayQueueColumnWidths(int[] widths) {
		setIntArrayProperty(KEY_RETROUI_PLAYQUEUE_COLUMN_WIDTHS, widths);
	}

	@Override
	public int[] getRetroUIPlayQueueColumnPositions() {
		return getIntArrayProperty(KEY_RETROUI_PLAYQUEUE_COLUMN_POSITIONS, null);
	}

	@Override
	public void setRetroUIPlayQueueColumnPositions(int[] colPos) {
		setIntArrayProperty(KEY_RETROUI_PLAYQUEUE_COLUMN_POSITIONS, colPos);
	}

	@Override
	public int[] getEqBands() {
		int[] result = getIntArrayProperty(KEY_EQ_BANDS_VALUES, null);
		// TODO: band count
		return result != null ? result : new int[] { 500, 500, 500, 500, 500, 500, 500, 500, 500, 500 };
	}

	@Override
	public void setEqBands(int[] eqBands) {
		setIntArrayProperty(KEY_EQ_BANDS_VALUES, eqBands);
	}

	@Override
	public boolean isEqEnabled() {
		return Boolean.valueOf(prefs.get(KEY_EQ_ENABLED, "false"));
	}

	@Override
	public void setEqEnabled(boolean eqEnabled) {
		prefs.put(KEY_EQ_ENABLED, Boolean.toString(eqEnabled));
	}

	@Override
	public int getEqGain() {
		String eqGainStr = prefs.get(KEY_EQ_GAIN, "500");
		try {
			return Integer.parseInt(eqGainStr);
		} catch (NumberFormatException nfe) {
			LOGGER.warning("Number format exception for preference " + KEY_EQ_GAIN + " value " + eqGainStr);
			return 500;
		}
	}

	@Override
	public void setEqGain(int eqGain) {
		prefs.put(KEY_EQ_GAIN, Integer.toString(eqGain));
	}

	@Override
	public ShuffleMode getShuffleMode() {
		return ShuffleMode.valueOf(prefs.get(KEY_SHUFFLE_MODE, ShuffleMode.OFF.name()));
	}

	@Override
	public void setShuffleMode(ShuffleMode value) {
		prefs.put(KEY_SHUFFLE_MODE, value.name());
	}

	@Override
	public RepeatMode getRepeatMode() {
		return RepeatMode.valueOf(prefs.get(KEY_REPEAT_MODE, RepeatMode.OFF.name()));
	}

	@Override
	public void setRepeatMode(RepeatMode value) {
		prefs.put(KEY_REPEAT_MODE, value.name());
	}

	@Override
	public Set<String> getSupportedFileExtensions() {
		return getStringListProperty(KEY_SUPPORTED_FILE_EXTENSIONS, DEFAULT_SUPPORTED_FILE_EXTENSIONS).stream()
				.collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public void setSupportedFileExtensions(Collection<String> extensions) {
		extensions = extensions.stream().map(String::toLowerCase).collect(Collectors.toCollection(TreeSet::new));
		setStringListProperty(KEY_SUPPORTED_FILE_EXTENSIONS, extensions);
	}

	@Override
	public void saveMainWindowState(Tuple4<Boolean, String, Point, Dimension> windowState) {
		this.saveWindowState(KEY_MAIN_WINDOW_STATE, windowState);
	}

	@Override
	public Tuple4<Boolean, String, Point, Dimension> getMainWindowState() {
		return this.restoreWindowState(KEY_MAIN_WINDOW_STATE);
	}

	@Override
	public void saveEQWindowState(Tuple4<Boolean, String, Point, Dimension> windowState) {
		this.saveWindowState(KEY_EQ_WINDOW_STATE, windowState);
	}

	@Override
	public Tuple4<Boolean, String, Point, Dimension> getEQWindowState() {
		return this.restoreWindowState(KEY_EQ_WINDOW_STATE);
	}

	@Override
	public void saveRetroUIMainWindowState(Tuple4<Boolean, String, Point, Dimension> windowState) {
		this.saveWindowState(KEY_RETROUI_MAIN_WINDOW_STATE, windowState);
	}

	@Override
	public Tuple4<Boolean, String, Point, Dimension> getRetroUIMainWindowState() {
		return this.restoreWindowState(KEY_RETROUI_MAIN_WINDOW_STATE);
	}

	@Override
	public void saveRetroUIEqWindowState(Tuple4<Boolean, String, Point, Dimension> windowState) {
		this.saveWindowState(KEY_RETROUI_EQ_WINDOW_STATE, windowState);
	}

	@Override
	public Tuple4<Boolean, String, Point, Dimension> getRetroUIEQWindowState() {
		return this.restoreWindowState(KEY_RETROUI_EQ_WINDOW_STATE);
	}

	@Override
	public void saveRetroUIPlaylistWindowState(Tuple4<Boolean, String, Point, Dimension> windowState) {
		this.saveWindowState(KEY_RETROUI_PLAYLIST_WINDOW_STATE, windowState);
	}

	@Override
	public Tuple4<Boolean, String, Point, Dimension> getRetroUIPlaylistWindowState() {
		return this.restoreWindowState(KEY_RETROUI_PLAYLIST_WINDOW_STATE);
	}

	@Override
	public boolean isAutoStop() {
		return Boolean.valueOf(prefs.get(KEY_AUTOSTOP, "false"));
	}

	@Override
	public void setAutoStop(boolean autoStop) {
		prefs.put(KEY_AUTOSTOP, Boolean.toString(autoStop));
	}

	@Override
	public String getRetroUISkin() {
		return prefs.get(KEY_RETROUI_SKIN, null);
	}

	@Override
	public void setRetroUISkin(String value) {
		prefs.put(KEY_RETROUI_SKIN, value);
	}

	@Override
	public int getRetroUIPlaylistSizeExtX() {
		return Integer.parseInt(prefs.get(KEY_RETROUI_PLAYLIST_WINDOW_SIZE_EXT_X, String.valueOf(0)));
	}

	@Override
	public void setRetroUIPlaylistSizeExtX(int value) {
		prefs.put(KEY_RETROUI_PLAYLIST_WINDOW_SIZE_EXT_X, String.valueOf(value));
	}

	@Override
	public int getRetroUIPlaylistSizeExtY() {
		return Integer.parseInt(prefs.get(KEY_RETROUI_PLAYLIST_WINDOW_SIZE_EXT_Y, String.valueOf(0)));
	}

	@Override
	public void setRetroUIPlaylistSizeExtY(int value) {
		prefs.put(KEY_RETROUI_PLAYLIST_WINDOW_SIZE_EXT_Y, String.valueOf(value));
	}

	@Override
	public int getVolume() {
		return Integer.parseInt(prefs.get(KEY_VOLUME, "100"));
	}

	@Override
	public void setVolume(int volume) {
		prefs.put(KEY_VOLUME, String.valueOf(volume));
	}

	@Override
	public void setBalance(int balanceLR) {
		prefs.put(KEY_BALANCE, String.valueOf(balanceLR));

	}

	@Override
	public int getBalance() {
		return Integer.parseInt(prefs.get(KEY_BALANCE, "50"));
	}

	protected Tuple4<Boolean, String, Point, Dimension> restoreWindowState(String key) {
		String windowStateSerialized = prefs.get(key, null);
		if (windowStateSerialized == null) {
			return null;
		}
		try {
			String[] parts = windowStateSerialized.split(";");
			boolean visible = Boolean.valueOf(parts[0]);
			String[] xAndY = parts[1].split(",");
			int x = Integer.parseInt(xAndY[0]);
			int y = Integer.parseInt(xAndY[1]);
			Point location = new Point(x, y);
			String[] widthAndHeight = parts[2].split(",");
			int width = Integer.parseInt(widthAndHeight[0]);
			int height = Integer.parseInt(widthAndHeight[1]);
			Dimension size = new Dimension(width, height);

			return Tuple4.<Boolean, String, Point, Dimension> builder()
					.a(visible)
					.b(parts.length > 3 ? parts[3] : "")
					.c(location)
					.d(size)
					.build();
		} catch (NumberFormatException nfe) {
			LOGGER.log(Level.WARNING, "Failed to restore window state for key " + key + " - number format issue", nfe);
			return null;
		}
	}

	protected void saveWindowState(String key, Tuple4<Boolean, String, Point, Dimension> windowState) {
		String windowStateSerialized = windowState.getA().toString() + ";" + windowState.getC().x + "," + windowState.getC().y + ";"
				+ windowState.getD().width + "," + windowState.getD().height + ";" + windowState.getB();
		prefs.put(key, windowStateSerialized);
	}

	protected List<String> getStringListProperty(String prefKey, String defaultVal) {
		String value = prefs.get(prefKey, defaultVal);
		if (value == null) {
			return Collections.emptyList();
		} else {
			return Stream.of(value.split(STRING_LIST_VALUES_SEPARATOR)).collect(Collectors.toList());
		}
	}

	protected void setStringListProperty(String prefKey, Collection<String> values) {
		if (values != null && !values.isEmpty()) {
			prefs.put(prefKey, values.stream().collect(Collectors.joining(STRING_LIST_VALUES_SEPARATOR)));
		} else {
			prefs.remove(prefKey);
		}
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
