package x.mvmn.sonivm.lastfm;

import java.util.function.Consumer;

import x.mvmn.sonivm.playqueue.PlaybackQueueEntry;
import x.mvmn.sonivm.util.Tuple2;

public interface LastFMScrobblingService {

	void setNowPlayingTrack(PlaybackQueueEntry trackInfo);

	void scrobbleTrack(PlaybackQueueEntry trackInfo);

	void onLastFMPreferencesChange();

	void addStatusListener(Consumer<Tuple2<Boolean, String>> statusListener);

	void shutdown();
}