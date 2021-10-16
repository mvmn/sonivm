package x.mvmn.sonivm.audio;

import javax.sound.sampled.AudioFileFormat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioFileInfo {

	private final String filePath;
	private final Integer durationSeconds;
	private final boolean seekable;
	private final AudioFileFormat audioFileFormat;
}
