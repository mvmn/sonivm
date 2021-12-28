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

	public void setPlaybackNumbers(int min, int sec, boolean negative) {
		int minH = (min / 10) % 10;
		int minL = min % 10;
		int secH = (sec / 10) % 10;
		int secL = sec % 10;
		playTimeNumbers[0].setState(negative ? 12 : 11);
		playTimeNumbers[1].setState(minH);
		playTimeNumbers[2].setState(minL);
		playTimeNumbers[3].setState(secH);
		playTimeNumbers[4].setState(secL);
	}

	public void setPlybackIndicatorState(boolean playing, boolean paused) {
		playStateIndicator.setState(playing ? paused ? 1 : 0 : 2);
	}
}
