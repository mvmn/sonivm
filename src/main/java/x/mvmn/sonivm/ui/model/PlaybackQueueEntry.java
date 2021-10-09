package x.mvmn.sonivm.ui.model;

import java.io.File;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaybackQueueEntry {
	private File targetFile;

	private volatile TrackMetadata trackMetadata;
	private volatile Long duration;

	@Data
	@Builder
	public static class TrackMetadata {
		private String trackNumber;
		private String artist;
		private String album;
		private String title;
		private String date;
		private String genre;
		private Long duration;
	}

	public String getTrackNumber() {
		return trackMetadata != null ? trackMetadata.getTrackNumber() : null;
	}

	public String getArtist() {
		return trackMetadata != null ? trackMetadata.getArtist() : null;
	}

	public String getAlbum() {
		return trackMetadata != null ? trackMetadata.getAlbum() : null;
	}

	public String getTitle() {
		return trackMetadata != null ? trackMetadata.getTitle() : targetFile.getName();
	}

	public String getDate() {
		return trackMetadata != null ? trackMetadata.getDate() : null;
	}

	public String getGenre() {
		return trackMetadata != null ? trackMetadata.getGenre() : null;
	}

	public Long getDuration() {
		return duration != null ? duration : (trackMetadata != null ? trackMetadata.getDuration() : null);
	}

	public String toDisplayStr() {
		if (trackMetadata == null) {
			return targetFile.getName();
		} else {
			return String.format("%1$s \"%2$s\" (%4$s) - %3$s", trackMetadata.getArtist(), trackMetadata.getAlbum(),
					trackMetadata.getTitle(), trackMetadata.getDate());
		}
	}
}
