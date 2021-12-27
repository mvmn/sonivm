package x.mvmn.sonivm.ui.retro;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIBooleanIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIMultiIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUITextComponent;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;

@RequiredArgsConstructor
@Builder
public class RetroUIMainWindow {
	@Getter
	protected final RasterGraphicsWindow window;
	protected final RasterUIBooleanIndicator titleBar;

	protected final RasterUIButton btnStop;
	protected final RasterUIButton btnPlay;
	protected final RasterUIButton btnNext;
	protected final RasterUIButton btnPrevious;
	protected final RasterUIButton btnPause;

	protected final RasterUIButton btnEject;

	protected final RasterUISlider seekSlider;

	protected final RasterUISlider volumelider;
	protected final RasterUISlider balanceSlider;

	protected final RasterUIBooleanIndicator monoIndicator;
	protected final RasterUIBooleanIndicator stereoIndicator;

	protected final RasterUIToggleButton btnEqToggle;
	protected final RasterUIToggleButton btnPlaylistToggle;

	protected final RasterUIToggleButton btnShuffleToggle;
	protected final RasterUIToggleButton btnRepeatToggle;

	protected final RasterUIMultiIndicator playStateIndicator;
	protected final RasterUIMultiIndicator[] playTimeNumbers;
	
	protected final RasterUITextComponent nowPlayingText;
}
