package x.mvmn.sonivm.eq.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import davaguine.jeq.core.IIRControls;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.util.Tuple2;

@Service
public class SonivmEqualizerServiceImpl implements SonivmEqualizerService {

	private volatile Tuple2<IIRControls, Integer> eqControlsAndChannelsCount;
	private volatile int lastGainValue = 300;
	// TODO: inject band count
	private volatile int[] lastBandValues = new int[] { 500, 500, 500, 500, 500, 500, 500, 500, 500, 500 };
	private volatile boolean eqEnabled = false;

	@Autowired
	private AudioService audioService;

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
			float preampMax = eqControlsAndChannelsCount.getA().getMaximumPreampValue();
			float preampMin = eqControlsAndChannelsCount.getA().getMinimumPreampValue();
			float range = preampMax - preampMin;
			float newValue = preampMin + range / 1000 * valuePerMille;
			for (int channel = 0; channel < eqControlsAndChannelsCount.getB(); channel++) {
				eqControlsAndChannelsCount.getA().setPreampValue(channel, newValue);
			}
		}
	}

	@Override
	public void onBandChange(int bandNumber, int valuePerMille) {
		this.lastBandValues[bandNumber] = valuePerMille;
		Tuple2<IIRControls, Integer> eqControlsAndChannelsCount = this.eqControlsAndChannelsCount;
		if (eqControlsAndChannelsCount != null) {
			float max = eqControlsAndChannelsCount.getA().getMaximumBandValue();
			float min = eqControlsAndChannelsCount.getA().getMinimumBandValue();
			float range = max - min;
			float newValue = min + range / 1000 * valuePerMille;
			for (int channel = 0; channel < eqControlsAndChannelsCount.getB(); channel++) {
				eqControlsAndChannelsCount.getA().setBandValue(bandNumber, channel, newValue);
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
}
