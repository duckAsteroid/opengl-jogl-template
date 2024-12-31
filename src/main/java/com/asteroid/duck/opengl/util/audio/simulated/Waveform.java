package com.asteroid.duck.opengl.util.audio.simulated;

/**
 * Represents a static sinusoidal waveform
 *
 * @param freq           the frequency in Hz
 * @param amplitude      the amplitude of the waveform
 * @param phase          the relative phase of the waveform (in degrees)
 * @param stereoPosition the relative position of the wave in stereo. -1 left to 1 right.
 */
public record Waveform(double freq, double amplitude, double phase, double stereoPosition) implements SampledWaveformData {

	public Waveform(double freq, double amplitude, double phase, double stereoPosition) {
		this.freq = freq;
		this.amplitude = amplitude;
		this.phase = phase;
		if (stereoPosition < -1.0 || stereoPosition > 1.0) {
			throw new IllegalArgumentException("stereoPosition must be between -1.0 and +1.0.");
		}
		this.stereoPosition = stereoPosition;
	}

	public Waveform(double freq) {
		this(freq, 1.0, 0.0, 0.0);
	}

	public static Waveform MIDDLE_C = new Waveform(261.63);

	public double[] sample(double time) {
		double signalSample = amplitude * Math.sin(2 * Math.PI * freq * time + Math.toRadians(phase));
		double leftChannelAmplitude;
		double rightChannelAmplitude;

		// Calculate amplitudes for left and right channels
		leftChannelAmplitude = (1.0 - stereoPosition) / 2.0;
		rightChannelAmplitude = (1.0 + stereoPosition) / 2.0;

		// Apply amplitudes to your signal sample
		double leftSample = (signalSample / 2.0) * leftChannelAmplitude;
		double rightSample = (signalSample / 2.0) * rightChannelAmplitude;

		return new double[]{
						leftSample, rightSample
		};
	}

	/**
	 * Return this waveform transposed up or down (negative) by the given number of semitones.
	 * The new waveform will have the same phase, amplitude and stereomix.
	 */
	public Waveform transpose(int semitoneSteps) {
		double factor = Math.pow(2.0, (double) semitoneSteps / 12.0);
		return new Waveform(this.freq * factor, amplitude, phase, stereoPosition);
	}

	public Waveform atStereo(double stereoLocation) {
		return new Waveform(freq, amplitude, phase, stereoLocation);
	}

	public Waveform amplify(double amplitude) {
		return new Waveform(freq, this.amplitude * amplitude, phase, stereoPosition);
	}
}
