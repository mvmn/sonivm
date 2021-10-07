package x.mvmn.sonivm.audio.impl;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
public class AudioServiceTask {
	public static enum Type {
		PLAY, STOP, PAUSE, SET_AUDIODEVICE, SEEK
	}

	private final Type type;
	private final String data;
	private final Long numericData;
}
