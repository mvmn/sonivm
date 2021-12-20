package x.mvmn.sonivm.ui.retro;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;

@RequiredArgsConstructor
@Builder
public class RetroUIMainWindow {
	@Getter
	protected final RasterGraphicsWindow window;

	protected final RasterUIButton btnStop;
	protected final RasterUIButton btnPlay;
	protected final RasterUIButton btnNext;
	protected final RasterUIButton btnPrevious;
	protected final RasterUIButton btnPause;

	protected final RasterUIButton btnEject;

	protected final RasterUISlider seekSlider;

	protected final RasterUISlider volumelider;
	protected final RasterUISlider balanceSlider;
}
