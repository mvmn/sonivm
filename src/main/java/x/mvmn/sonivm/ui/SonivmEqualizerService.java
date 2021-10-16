package x.mvmn.sonivm.ui;

import davaguine.jeq.core.IIRControls;

public interface SonivmEqualizerService {
	void onGainChange(int valuePerMille);

	void onBandChange(int bandNumber, int valuePerMille);

	void setEqControls(IIRControls eqControls, int channels);

	void onEqualizerEnableToggle(boolean equalizerEnabled);
}
