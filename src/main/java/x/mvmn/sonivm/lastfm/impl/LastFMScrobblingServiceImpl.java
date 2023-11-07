package x.mvmn.sonivm.lastfm.impl;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.scrobble.ScrobbleData;
import de.umass.lastfm.scrobble.ScrobbleResult;
import jakarta.annotation.PostConstruct;
import x.mvmn.sonivm.lastfm.LastFMQueueService;
import x.mvmn.sonivm.lastfm.LastFMScrobblingService;
import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.prefs.PreferencesService;
import x.mvmn.sonivm.util.StringUtil;
import x.mvmn.sonivm.util.Tuple2;

@Service
public class LastFMScrobblingServiceImpl implements LastFMScrobblingService {

	private static final Logger LOGGER = Logger.getLogger(LastFMScrobblingServiceImpl.class.getCanonicalName());

	@Autowired
	protected LastFMQueueService lastFMQueueService;

	@Autowired
	protected PreferencesService preferencesService;

	private final ExecutorService lastFmScrobbleTaskExecutor = Executors.newFixedThreadPool(1);
	private final AtomicReference<Session> lastFMSession = new AtomicReference<>();
	private final CopyOnWriteArrayList<Consumer<Tuple2<Boolean, String>>> statusListeners = new CopyOnWriteArrayList<>();

	@Override
	public void setNowPlayingTrack(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			ScrobbleData scrobbleData = toScrobbleData(trackInfo);
			boolean success = false;
			String status = "";
			try {
				Session session = getLastFMSession();
				if (session != null) {
					LOGGER.info("Setting LastFM now playing state to " + scrobbleData);
					ScrobbleResult scrobbleResult = Track.updateNowPlaying(scrobbleData, session);
					if (!scrobbleResult.isSuccessful()) {
						status = "LastFM update now playing failed: " + scrobbleResult.getErrorCode() + " "
								+ scrobbleResult.getErrorMessage();
						LOGGER.info(status);
					} else {
						success = true;
						status = "Set LastFM now playing to " + trackInfo.toDisplayStr();
					}
				} else {
					LOGGER.info("Skipping update now playing in LastFM - no session.");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to update now playing in LastFM", e);
				status = "Failed to update now playing in LastFM: " + e.getClass().getSimpleName() + " "
						+ StringUtil.blankForNull(e.getMessage());
			}
			updateStatus(Tuple2.<Boolean, String> builder().a(success).b(status).build());

			if (success) {
				reSubmitFailedLastFMSubmissions();
			}
		});
	}

	@Override
	public void scrobbleTrack(PlaybackQueueEntry trackInfo) {
		lastFmScrobbleTaskExecutor.execute(() -> {
			ScrobbleData scrobbleData = toScrobbleData(trackInfo);
			Tuple2<Boolean, String> result = doScrobbleTrack(scrobbleData);
			if (!result.getA()) {
				lastFMQueueService.queueTrack(scrobbleData);
			}
			updateStatus(result);
		});
	}

	@Override
	public void onLastFMPreferencesChange() {
		lastFMSession.set(null);
	}

	protected Tuple2<Boolean, String> doScrobbleTrack(ScrobbleData scrobbleData) {
		boolean success = false;
		String status = "";
		try {
			Session session = getLastFMSession();
			if (session != null) {
				LOGGER.info("Scrobbling LastFM track played " + scrobbleData);
				ScrobbleResult scrobbleResult = Track.scrobble(scrobbleData, session);
				if (!scrobbleResult.isSuccessful()) {
					status = "LastFM scrobbling failed: " + scrobbleResult.getErrorCode() + " " + scrobbleResult.getErrorMessage();
					LOGGER.info(status);
				} else {
					success = true;
					status = "Scrobbled to LastFM " + PlaybackQueueEntry.toDisplayStr(scrobbleData);
				}
			} else {
				LOGGER.info("Skipping scrobbling track in LastFM - no session.");
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to scrobble track in LastFM", e);
			status = "Failed to scrobble track in LastFM: " + e.getClass().getSimpleName() + " " + StringUtil.blankForNull(e.getMessage());
		}
		return Tuple2.<Boolean, String> builder().a(success).b(status).build();
	}

	protected Session getLastFMSession() throws Exception {
		Session result = lastFMSession.get();
		if (result == null) {
			String user = this.preferencesService.getUsername();
			String password = this.preferencesService.getPassword();
			String apiKey = this.preferencesService.getApiKey();
			String secret = this.preferencesService.getApiSecret();

			if (user != null && password != null) {
				try {
					LOGGER.info("Trying to establish LastFM session");
					result = Authenticator.getMobileSession(user, password, apiKey, secret);
					if (result == null) {
						throw new Exception("Failed to connect");
					}
					lastFMSession.set(result);
					LOGGER.info("Successfully established LastFM session");
					updateStatus(Tuple2.<Boolean, String> builder().a(true).b("Established LastFM session").build());
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to establish LastFM session", e);
					updateStatus(Tuple2.<Boolean, String> builder()
							.a(false)
							.b("Failed to establish LastFM session: " + e.getClass().getSimpleName() + " "
									+ StringUtil.blankForNull(e.getMessage()))
							.build());
				}
			}
		}
		return result;
	}

	protected ScrobbleData toScrobbleData(PlaybackQueueEntry trackInfo) {
		ScrobbleData result = new ScrobbleData(trackInfo.getArtist(), trackInfo.getTitle(), (int) (System.currentTimeMillis() / 1000));
		if (trackInfo.getDuration() != null) {
			result.setDuration(trackInfo.getDuration().intValue());
		}

		if (trackInfo.getTrackMetadata() != null) {
			result.setAlbum(trackInfo.getAlbum());
			try {
				result.setTrackNumber(Integer.parseInt(trackInfo.getTrackNumber()));
			} catch (NumberFormatException nfe) {
				LOGGER.finest("Can't parse track number as integer for LastFM: " + trackInfo.getTrackNumber());
			}
		}
		return result;
	}

	protected void reSubmitFailedLastFMSubmissions() {
		try {
			lastFMQueueService.processQueuedTracks(tracks -> {
				for (ScrobbleData track : tracks) {
					Tuple2<Boolean, String> result = this.doScrobbleTrack(track);
					if (!result.getA()) {
						throw new RuntimeException("Failed to re-scrobble track " + track + ": " + result.getB());
					}
				}
			}, 100);
		} catch (Throwable t) {
			LOGGER.log(Level.SEVERE, "Failed to re-submit LastFM tracks", t);
		}
	}

	@PostConstruct
	protected void init() {
		lastFmScrobbleTaskExecutor.execute(() -> reSubmitFailedLastFMSubmissions());
	}

	@Override
	public void shutdown() {
		LOGGER.info("Shutting down LastFM task executor.");
		lastFmScrobbleTaskExecutor.shutdown();
	}

	protected void updateStatus(Tuple2<Boolean, String> status) {
		statusListeners.forEach(listener -> listener.accept(status));
	}

	@Override
	public void addStatusListener(Consumer<Tuple2<Boolean, String>> statusListener) {
		statusListeners.add(statusListener);
	}
}
