package x.mvmn.sonivm.eq;

import davaguine.jeq.core.IIRControls;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.eq.model.EqualizerState;

public interface SonivmEqualizerService {
	void setGain(int valuePerMille);

	void onBandChange(int bandNumber, int valuePerMille);

	void setEqControls(IIRControls eqControls, int channels);

	void setEQEnabled(boolean equalizerEnabled);

	EqualizerState getCurrentState();

	void setPreset(EqualizerPreset eqPreset);

	void setState(EqualizerState eqState);
}
