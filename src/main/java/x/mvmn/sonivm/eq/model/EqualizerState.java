package x.mvmn.sonivm.eq.model;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@Builder
public class EqualizerState {
	private final boolean enabled;
	private final int gain;
	private final int[] bands;
}
