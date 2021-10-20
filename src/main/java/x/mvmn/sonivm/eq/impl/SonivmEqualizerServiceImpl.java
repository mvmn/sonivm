package x.mvmn.sonivm.eq.impl;

import java.io.File;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import davaguine.jeq.core.IIRControls;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.ui.EqualizerWindow;
import x.mvmn.sonivm.ui.util.swing.SwingUtil;
import x.mvmn.sonivm.util.Tuple2;

@Service
public class SonivmEqualizerServiceImpl implements SonivmEqualizerService {

	private static final Logger LOGGER = Logger.getLogger(SonivmEqualizerServiceImpl.class.getCanonicalName());

	private volatile Tuple2<IIRControls, Integer> eqControlsAndChannelsCount;
	private volatile int lastGainValue = 500;
	// TODO: inject band count
	private volatile int[] lastBandValues = new int[] { 500, 500, 500, 500, 500, 500, 500, 500, 500, 500 };
	private volatile boolean eqEnabled = false;

	@Autowired
	private AudioService audioService;

	@Autowired
	private EqualizerPresetService equalizerPresetService;

	@Override
	public void setEqControls(IIRControls eqControls, int channels) {
		if (eqControls != null && channels > 0) {
			this.eqControlsAndChannelsCount = Tuple2.<IIRControls, Integer> builder().a(eqControls).b(channels).build();
			applyLatestValues();
		} else {
			this.eqControlsAndChannelsCount = null;
		}
	}

	@Override
	public void onGainChange(int valuePerMille) {
		this.lastGainValue = valuePerMille;
		Tuple2<IIRControls, Integer> eqControlsAndChannelsCount = this.eqControlsAndChannelsCount;
		if (eqControlsAndChannelsCount != null) {
			float preampMax = eqControlsAndChannelsCount.getA().getMaximumPreampDbValue();
			float preampMin = eqControlsAndChannelsCount.getA().getMinimumPreampDbValue();
			float range = preampMax - preampMin;
			float newValue = preampMin + range / 1000 * valuePerMille;
			for (int channel = 0; channel < eqControlsAndChannelsCount.getB(); channel++) {
				eqControlsAndChannelsCount.getA().setPreampDbValue(channel, newValue);
			}
		}
	}

	@Override
	public void onBandChange(int bandNumber, int valuePerMille) {
		this.lastBandValues[bandNumber] = valuePerMille;
		Tuple2<IIRControls, Integer> eqControlsAndChannelsCount = this.eqControlsAndChannelsCount;
		if (eqControlsAndChannelsCount != null) {
			float max = eqControlsAndChannelsCount.getA().getMaximumBandDbValue();
			float min = eqControlsAndChannelsCount.getA().getMinimumBandDbValue();
			float range = max - min;
			float newValue = min + range / 1000 * valuePerMille;
			for (int channel = 0; channel < eqControlsAndChannelsCount.getB(); channel++) {
				eqControlsAndChannelsCount.getA()
						.setBandValue(bandNumber, channel,
								(float) (0.25220207857061455D * Math.exp(0.08017836180235399D * (double) newValue) - 0.2522020785283656D));
			}
		}
	}

	private void applyLatestValues() {
		onGainChange(lastGainValue);
		int[] lastBandValues = this.lastBandValues;
		for (int i = 0; i < lastBandValues.length; i++) {
			onBandChange(i, lastBandValues[i]);
		}
	}

	@Override
	public void onEqualizerEnableToggle(boolean equalizerEnabled) {
		eqEnabled = equalizerEnabled;
		audioService.setUseEqualizer(equalizerEnabled);
	}

	@Override
	public EqualizerState getCurrentState() {
		return EqualizerState.builder().enabled(eqEnabled).gain(lastGainValue).bands(lastBandValues).build();
	}

	@Override
	public void setState(EqualizerState eqState) {
		onEqualizerEnableToggle(eqState.isEnabled());
		onGainChange(eqState.getGain());
		int[] bandStates = eqState.getBands();
		for (int i = 0; i < bandStates.length; i++) {
			onBandChange(i, bandStates[i]);
		}
	}

	@Override
	public void onSavePreset(String name, EqualizerPreset equalizerPreset) {
		new Thread(() -> {
			try {
				equalizerPresetService.savePreset(name, equalizerPreset);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to save preset " + name, t);
			}
		}).start();
	}

	@Override
	public void onLoadPreset(String name, EqualizerWindow eqWindow) {
		new Thread(() -> {
			try {
				EqualizerPreset preset = equalizerPresetService.loadPreset(name);
				SwingUtil.runOnEDT(() -> eqWindow.setPreset(preset), false);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to load preset " + name, t);
			}
		}).start();
	}

	@Override
	public void onExportPreset(File file, String name, EqualizerPreset equalizerPreset) {
		new Thread(() -> {
			try {
				equalizerPresetService.exportWinAmpEqfPreset(name, equalizerPreset, file);
			} catch (Throwable t) {
				LOGGER.log(Level.WARNING, "Failed to export preset to file " + file.getAbsolutePath(), t);
			}
		}).start();
	}

	@Override
	public void onImportPreset(File presetFile, EqualizerWindow eqWindow) {
		equalizerPresetService.importWinAmpEqfPreset(presetFile);
	}

	@Override
	public Collection<String> listPresets() {
		return equalizerPresetService.listPresets();
	}
}
