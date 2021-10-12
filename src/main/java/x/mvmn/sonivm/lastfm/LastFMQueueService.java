package x.mvmn.sonivm.lastfm;

import java.util.List;
import java.util.function.Consumer;

import de.umass.lastfm.scrobble.ScrobbleData;

public interface LastFMQueueService {

	void queueTrack(ScrobbleData track);

	void processQueuedTracks(Consumer<List<ScrobbleData>> processor, int max);

}