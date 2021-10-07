package x.mvmn.sonivm.audio;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaybackEvent {
	public static enum Type {
		START, FINISH, ERROR, PROGRESS;
	}

	public static enum ErrorType {
		FILE_NOT_FOUND, FILE_FORMAT_ERROR, PLAYBACK_ERROR;
	}

	private final Type type;
	private final AudioFileInfo audioMetadata;
	private final Long playbackTimeDelta;
	private final String error;
	private final ErrorType errorType;
}
