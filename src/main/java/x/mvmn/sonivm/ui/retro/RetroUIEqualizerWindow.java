package x.mvmn.sonivm.ui.retro;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.ui.SonivmUIController;
import x.mvmn.sonivm.ui.retro.rasterui.RasterGraphicsWindow;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIBooleanIndicator;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIButton;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUISlider;
import x.mvmn.sonivm.ui.retro.rasterui.RasterUIToggleButton;

@RequiredArgsConstructor
@Builder
public class RetroUIEqualizerWindow {
	@Getter
	protected final RasterGraphicsWindow window;
	protected final RasterUIBooleanIndicator titleBar;

	protected final RasterUISlider gainSlider;
	protected final RasterUISlider[] eqSliders;
	protected final RasterUIButton btnPresets;

	protected final RasterUIToggleButton btnEqOnOff;
	protected final RasterUIToggleButton btnEqAuto;

	public void addListener(SonivmUIController sonivmUI) {
		btnPresets.addListener(() -> sonivmUI.showPresetsMenu(window,
				window.newScaleCoord(window.getScaleFactor(), btnPresets.getX() + btnPresets.getWidth()),
				window.newScaleCoord(window.getScaleFactor(), btnPresets.getY())));

		btnEqOnOff.addListener(() -> sonivmUI.setEQEnabled(btnEqOnOff.isButtonOn()));
		gainSlider.addListener(() -> sonivmUI.onEQGainChange(1.0d - gainSlider.getSliderPositionRatio()));
		for (int i = 0; i < eqSliders.length; i++) {
			final int bandNum = i;
			eqSliders[i].addListener(() -> sonivmUI.onEQBandChange(bandNum, 1.0d - eqSliders[bandNum].getSliderPositionRatio()));
		}
		// Not supported for now
		btnEqAuto.addListener(() -> btnEqAuto.setButtonOn(false));
	}

	public void setGain(double gainRatio) {
		gainSlider.setSliderPositionRatio(1.0d - gainRatio, false);
	}

	public void setBand(int bandNum, double positionRatio) {
		eqSliders[bandNum].setSliderPositionRatio(1.0d - positionRatio, false);
	}

	public void setPreset(EqualizerPreset preset) {
		setGain(preset.getGain() / 1000.d);
		for (int i = 0; i < eqSliders.length; i++) {
			setBand(i, preset.getBands()[i] / 1000.d);
		}
	}

	public void setEQEnabled(boolean enabled) {
		btnEqOnOff.setButtonOn(enabled);
	}
}
