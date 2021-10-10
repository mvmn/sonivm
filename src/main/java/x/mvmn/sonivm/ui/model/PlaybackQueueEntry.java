package x.mvmn.sonivm.ui.model;

import java.beans.Transient;

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

	private volatile TrackMetadata trackMetadata;
	private volatile Long duration;

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
		private Long duration;
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
		return trackMetadata != null ? trackMetadata.getTitle() : targetFileName;
	}

	@Transient
	public String getDate() {
		return trackMetadata != null ? trackMetadata.getDate() : null;
	}

	@Transient
	public String getGenre() {
		return trackMetadata != null ? trackMetadata.getGenre() : null;
	}

	public Long getDuration() {
		return duration != null ? duration : (trackMetadata != null ? trackMetadata.getDuration() : null);
	}

	@Transient
	public String toDisplayStr() {
		if (trackMetadata == null) {
			return targetFileName;
		} else {
			return String.format("%1$s \"%2$s\" (%4$s) - %3$s", trackMetadata.getArtist(), trackMetadata.getAlbum(),
					trackMetadata.getTitle(), trackMetadata.getDate());
		}
	}
}
