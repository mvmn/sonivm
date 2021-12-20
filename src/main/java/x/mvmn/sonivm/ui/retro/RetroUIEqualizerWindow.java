package x.mvmn.sonivm.ui.retro;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;

@RequiredArgsConstructor
@Builder
public class RetroUIEqualizerWindow {
	@Getter
	protected final RasterGraphicsWindow window;

	protected final RasterUISlider gainSlider;

}
