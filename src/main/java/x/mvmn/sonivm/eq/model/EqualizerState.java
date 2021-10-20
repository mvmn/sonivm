package x.mvmn.sonivm.eq.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
public class EqualizerState extends EqualizerPreset {

	private final boolean enabled;

	public EqualizerState(boolean enabled, int gain, int[] bands) {
		super(gain, bands);
		this.enabled = enabled;
	}
}
