package x.mvmn.sonivm.eq.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class EqualizerPreset {
	private final int gain;
	private final int[] bands;

	@JsonCreator
	public EqualizerPreset(@JsonProperty("gain") int gain, @JsonProperty("bands") int[] bands) {
		this.gain = gain;
		this.bands = bands;
	}

	public static EqualizerPreset of(int gain, int bands[]) {
		return EqualizerPreset.builder().gain(gain).bands(bands).build();
	}
}
