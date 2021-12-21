package x.mvmn.sonivm.ui.retro;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;

@RequiredArgsConstructor
@Builder
public class RetroUIEqualizerWindow {
	@Getter
	protected final RasterGraphicsWindow window;
	protected final RasterUIIndicator titleBar;

	protected final RasterUISlider gainSlider;
	protected final RasterUISlider[] eqSliders;
	protected final RasterUIButton btnPresets;

	protected final RasterUIToggleButton btnEqOnOff;
	protected final RasterUIToggleButton btnEqAuto;
}
