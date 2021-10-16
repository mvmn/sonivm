package x.mvmn.sonivm.eq;

import davaguine.jeq.core.IIRControls;
import x.mvmn.sonivm.eq.model.EqualizerState;

public interface SonivmEqualizerService {
	void onGainChange(int valuePerMille);

	void onBandChange(int bandNumber, int valuePerMille);

	void setEqControls(IIRControls eqControls, int channels);

	void onEqualizerEnableToggle(boolean equalizerEnabled);

	EqualizerState getCurrentState();

	void setState(EqualizerState eqState);
}
