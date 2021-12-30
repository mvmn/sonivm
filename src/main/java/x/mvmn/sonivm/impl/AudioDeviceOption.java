package x.mvmn.sonivm.impl;

import javax.sound.sampled.Mixer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioDeviceOption {
	final Mixer.Info audioDeviceInfo;

	@Override
	public String toString() {
		return audioDeviceInfo != null ? audioDeviceInfo.getName() : "< Default >";
	}
}
