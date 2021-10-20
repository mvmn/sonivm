package x.mvmn.sonivm.eq;

import java.io.File;
import java.util.Collection;

import davaguine.jeq.core.IIRControls;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.ui.EqualizerWindow;

public interface SonivmEqualizerService {
	void onGainChange(int valuePerMille);

	void onBandChange(int bandNumber, int valuePerMille);

	void setEqControls(IIRControls eqControls, int channels);

	void onEqualizerEnableToggle(boolean equalizerEnabled);

	EqualizerState getCurrentState();

	void setState(EqualizerState eqState);

	void onSavePreset(String name, EqualizerPreset equalizerPreset);

	void onLoadPreset(String name, EqualizerWindow eqWindow);

	void onExportPreset(File presetFile, String name, EqualizerPreset equalizerPreset);

	void onImportPreset(File presetFile, EqualizerWindow eqWindow);

	Collection<String> listPresets();
}
