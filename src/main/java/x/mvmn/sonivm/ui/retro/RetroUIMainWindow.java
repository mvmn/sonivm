package x.mvmn.sonivm.ui.retro;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.audio.PlaybackState;
import x.mvmn.sonivm.impl.RepeatMode;
import x.mvmn.sonivm.impl.ShuffleMode;
import x.mvmn.sonivm.ui.SonivmUIController;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIBooleanIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIMultiIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUITextComponent;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;

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

	public void addListener(SonivmUIController listener) {
		seekSlider.addListener(() -> listener.onSeek(seekSlider.getSliderPositionRatio()));
		btnStop.addListener(listener::onStop);
		btnPlay.addListener(listener::onPlay);
		btnPause.addListener(listener::onPause);
		btnNext.addListener(listener::onNextTrack);
		btnPrevious.addListener(listener::onPreviousTrack);
		volumelider.addListener(() -> listener.onVolumeChange((int) Math.round(100 * volumelider.getSliderPositionRatio())));
		btnShuffleToggle
				.addListener(() -> listener.onShuffleModeSwitch(btnShuffleToggle.isButtonOn() ? ShuffleMode.PLAYLIST : ShuffleMode.OFF));
		btnRepeatToggle.addListener(() -> listener.onRepeatModeSwitch(btnRepeatToggle.isButtonOn() ? RepeatMode.PLAYLIST : RepeatMode.OFF));
	}

	public void setEQToggleState(boolean on) {
		this.btnEqToggle.setButtonOn(on);
	}

	public void setPlaylistToggleState(boolean on) {
		this.btnPlaylistToggle.setButtonOn(on);
	}

	public void setShuffleToggleState(boolean on) {
		this.btnShuffleToggle.setButtonOn(on);
	}

	public void setRepeatToggleState(boolean on) {
		this.btnRepeatToggle.setButtonOn(on);
	}

	public void setPlayTime(int seconds, boolean remaining) {
		setPlaybackNumbers(seconds / 60, seconds % 60, remaining);
	}

	public void setPlaybackNumbers(int min, int sec, boolean negative) {
		int minH = (min / 10) % 10;
		int minL = min % 10;
		int secH = (sec / 10) % 10;
		int secL = sec % 10;
		playTimeNumbers[0].setState(negative ? 11 : 10);
		playTimeNumbers[1].setState(minH);
		playTimeNumbers[2].setState(minL);
		playTimeNumbers[3].setState(secH);
		playTimeNumbers[4].setState(secL);
	}

	public void setPlybackIndicatorState(PlaybackState playbackState) {
		switch (playbackState) {
			case PLAYING:
				playStateIndicator.setState(0);
			break;
			case PAUSED:
				playStateIndicator.setState(1);
			break;
			default:
			case STOPPED:
				playStateIndicator.setState(2);
			break;
		}
	}

	public void setSeekSliderEnabled(boolean enabled) {
		SwingUtil.runOnEDT(() -> {
			this.seekSlider.setIndefinite(!enabled);
			this.seekSlider.repaint();
		}, false);
	}

	public void setSeekSliderPosition(double ratio) {
		SwingUtil.runOnEDT(() -> {
			this.seekSlider.setSliderPositionRatio(ratio, false);
			this.seekSlider.repaint();
		}, false);
	}

	public void setNowPlayingText(String nowPlaying, boolean repeat) {
		SwingUtil.runOnEDT(() -> {
			nowPlayingText.setRollTextOffset(repeat ? 16 : -1);
			nowPlayingText.setText(nowPlaying != null ? nowPlaying : "");
			nowPlayingText.setOffset(0);
		}, false);
	}

	public void advanceNowPlayingText(int delta) {
		int fullWidth = nowPlayingText.getTextFullWidth();
		if (fullWidth > 0) {
			int offset = nowPlayingText.getOffset() + delta;
			if (offset >= fullWidth) {
				offset = 0;
			}
			final int finalOffset = offset;
			SwingUtil.runOnEDT(() -> nowPlayingText.setOffset(finalOffset), false);
		}
	}

	public void setVolumeSliderPos(int value) {
		this.volumelider.setSliderPosition((int) Math.round(value / 100.0d * this.volumelider.getRange()), false);
	}
}
