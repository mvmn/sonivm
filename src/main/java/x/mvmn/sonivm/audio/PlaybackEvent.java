package x.mvmn.sonivm.audio;

import javax.sound.sampled.Control;

import davaguine.jeq.core.IIRControls;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlaybackEvent {
	public static enum Type {
		START, FINISH, ERROR, PROGRESS, DATALINE_CHANGE;
	}

	public static enum ErrorType {
		FILE_NOT_FOUND, FILE_FORMAT_ERROR, PLAYBACK_ERROR, AUDIODEVICE_ERROR;
	}

	private final Type type;
	private final AudioFileInfo audioMetadata;
	private final Control[] dataLineControls;
	private final IIRControls eqControls;
	private final Long playbackPositionMilliseconds;
	private final Long playbackDeltaMilliseconds;
	private final String error;
	private final ErrorType errorType;
}
