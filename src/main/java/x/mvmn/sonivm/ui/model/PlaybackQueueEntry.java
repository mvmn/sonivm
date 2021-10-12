package x.mvmn.sonivm.ui.model;

import java.beans.Transient;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackQueueEntry {
	private String targetFileFullPath;
	private String targetFileName;
	private boolean cueSheetTrack;
	private Integer cueSheetTrackStartTimeMillis;
	private Integer cueSheetTrackFinishTimeMillis;

	private volatile TrackMetadata trackMetadata;
	private volatile Integer duration;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class TrackMetadata {
		private String trackNumber;
		private String artist;
		private String album;
		private String title;
		private String date;
		private String genre;
		private Integer duration;
	}

	@Transient
	public String getTrackNumber() {
		return trackMetadata != null ? trackMetadata.getTrackNumber() : null;
	}

	@Transient
	public String getArtist() {
		return trackMetadata != null ? trackMetadata.getArtist() : null;
	}

	@Transient
	public String getAlbum() {
		return trackMetadata != null ? trackMetadata.getAlbum() : null;
	}

	@Transient
	public String getTitle() {
		return trackMetadata != null && trackMetadata.getTitle() != null ? trackMetadata.getTitle() : targetFileName;
	}

	@Transient
	public String getDate() {
		return trackMetadata != null ? trackMetadata.getDate() : null;
	}

	@Transient
	public String getGenre() {
		return trackMetadata != null ? trackMetadata.getGenre() : null;
	}

	public Integer getDuration() {
		if (cueSheetTrack) {
			int result = (int) ((cueSheetTrackFinishTimeMillis - cueSheetTrackStartTimeMillis) / 1000);
			return Math.max(0, result);
		}
		return duration != null ? duration : (trackMetadata != null ? trackMetadata.getDuration() : null);
	}

	@Transient
	public String toDisplayStr() {
		if (trackMetadata == null) {
			return targetFileName;
		} else {
			return String.format("%1$s \"%2$s\" (%4$s) - %3$s", getArtist(), getAlbum(), getTitle(), getDate());
		}
	}

	public boolean artistMatches(PlaybackQueueEntry other) {
		return propertyEquals(this, other, PlaybackQueueEntry::getArtist);
	}

	public boolean albumMatches(PlaybackQueueEntry other) {
		return propertyEquals(this, other, PlaybackQueueEntry::getAlbum);
	}

	private static boolean propertyEquals(PlaybackQueueEntry entryA, PlaybackQueueEntry entryB,
			Function<PlaybackQueueEntry, String> propertyExtractor) {
		String valA = propertyExtractor.apply(entryA);
		String valB = propertyExtractor.apply(entryB);
		return trackPropertiesEqual(valA, valB);
	}

	private static boolean trackPropertiesEqual(String valA, String valB) {
		if (valA == null) {
			valA = "";
		}
		if (valB == null) {
			valB = "";
		}
		return valA.trim().equalsIgnoreCase(valB.trim());
	}
}
