package com.asteroid.duck.opengl.util.audio.simulated;

import java.util.ArrayList;

public class CompositeWaveform extends ArrayList<SampledWaveformData> implements SampledWaveformData {
	public CompositeWaveform(int i) {
		super(i);
	}

	@Override
	public double[] sample(double time) {
		double[] sample = new double[2];
		// combine all the waveforms for this sample
		for(SampledWaveformData w : this) {
			double[] s = w.sample(time);
			sample[0] += s[0];
			sample[1] += s[1];
		}
		return sample;
	}
}
