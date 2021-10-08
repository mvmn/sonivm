package x.mvmn.sonivm.ui.model;

import java.util.function.Consumer;

import javax.sound.sampled.Mixer;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioDeviceOption {
	final Mixer.Info audioDeviceInfo;
	final Consumer<AudioDeviceOption> onSelect;

	@Override
	public String toString() {
		return audioDeviceInfo != null ? audioDeviceInfo.getName() : "< Default >";
	}

	public void selected() {
		onSelect.accept(this);
	}
}
