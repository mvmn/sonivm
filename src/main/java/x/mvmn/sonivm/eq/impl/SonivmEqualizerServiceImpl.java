package x.mvmn.sonivm.eq.impl;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import davaguine.jeq.core.IIRControls;
import x.mvmn.sonivm.audio.AudioService;
import x.mvmn.sonivm.eq.SonivmEqualizerService;
import x.mvmn.sonivm.eq.model.EqualizerPreset;
import x.mvmn.sonivm.eq.model.EqualizerState;
import x.mvmn.sonivm.util.Tuple2;

@Service
public class SonivmEqualizerServiceImpl implements SonivmEqualizerService {

	private volatile Tuple2<IIRControls, Integer> eqControlsAndChannelsCount;
	private volatile int lastGainValue = 500;
	private final CopyOnWriteArrayList<Integer> lastBandValues = new CopyOnWriteArrayList<Integer>(
			IntStream.generate(() -> 500).limit(10).mapToObj(Integer::valueOf).collect(Collectors.toList()));
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
	public void setGain(int valuePerMille) {
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
		this.lastBandValues.set(bandNumber, valuePerMille);
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
		setGain(lastGainValue);
		int[] lastBandValues = this.lastBandValues.stream().mapToInt(Integer::intValue).toArray();
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
		return EqualizerState.builder()
				.enabled(eqEnabled)
				.gain(lastGainValue)
				.bands(lastBandValues.stream().mapToInt(Integer::intValue).toArray())
				.build();
	}

	@Override
	public void setState(EqualizerState eqState) {
		onEqualizerEnableToggle(eqState.isEnabled());
		setPreset(eqState);
	}

	@Override
	public void setPreset(EqualizerPreset eqPreset) {
		setGain(eqPreset.getGain());
		int[] bandStates = eqPreset.getBands();
		for (int i = 0; i < bandStates.length; i++) {
			onBandChange(i, bandStates[i]);
		}
	}
}
