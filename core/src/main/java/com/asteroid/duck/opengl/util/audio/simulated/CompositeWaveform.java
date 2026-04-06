package com.asteroid.duck.opengl.util.audio.simulated;

import java.util.ArrayList;

public class CompositeWaveform extends ArrayList<StereoDataSource> implements StereoDataSource {
	public CompositeWaveform(int i) {
		super(i);
	}

	/**
	 * Add a mono source to the composite waveform, with a static central stereo position.
	 * @param source the mono source to add
	 */
	public void add(MonoDataSource source) {
		add(StaticStereoPositioner.CENTER.wrap(source));
	}

	@Override
	public double[] sample(double time) {
		double[] sample = new double[2];
		// combine all the waveforms for this sample
		for(StereoDataSource w : this) {
			double[] s = w.sample(time);
			sample[0] += s[0];
			sample[1] += s[1];
		}
		return sample;
	}
}
