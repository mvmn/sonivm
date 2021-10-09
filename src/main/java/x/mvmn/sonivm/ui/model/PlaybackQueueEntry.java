package x.mvmn.sonivm.ui.model;

import java.io.File;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaybackQueueEntry {
	private File targetFile;
	private Long trackNumber;
	private String artist;
	private String album;
	private String title;
	private Long duration;
	private String date;
	private String genre;

	private boolean tagDataPopulated;

	public String toDisplayStr() {
		if (!tagDataPopulated) {
			return targetFile.getName();
		} else {
			return String.format("%0$s \"%1$s\" (%3$s) - %2$s", artist, album, title, date);
		}
	}
}
