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
}
