package x.mvmn.sonivm.ui.model;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class PlaybackQueueEntryCompareBiPredicate implements BiPredicate<PlaybackQueueEntry, PlaybackQueueEntry> {
	private final List<Function<PlaybackQueueEntry, String>> propertyExtractors;

	public PlaybackQueueEntryCompareBiPredicate(List<Function<PlaybackQueueEntry, String>> propertyExtractor) {
		this.propertyExtractors = propertyExtractor;
	}

	@Override
	public boolean test(PlaybackQueueEntry a, PlaybackQueueEntry b) {
		return a.propertiesMatch(b, propertyExtractors);
	}

	public static PlaybackQueueEntryCompareBiPredicate BY_ARTIST = new PlaybackQueueEntryCompareBiPredicate(
			Arrays.asList(PlaybackQueueEntry::getArtist));

	public static PlaybackQueueEntryCompareBiPredicate BY_ARTIST_AND_ALBUM = new PlaybackQueueEntryCompareBiPredicate(
			Arrays.asList(PlaybackQueueEntry::getArtist, PlaybackQueueEntry::getAlbum));
}
