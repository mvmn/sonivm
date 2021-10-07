package x.mvmn.sonivm.audio;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioFileInfo {

	private final String filePath;
	private final Long durationSeconds;
	private final boolean seekable;
}
