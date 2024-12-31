package com.asteroid.duck.opengl.util.audio.simulated;

import java.util.*;

/**
 *
 * @param source the source of the sound
 * @param barPattern the on/off pattern in the bar
 * @param barLength the length (seconds) of a bar
 */
public record Note(SampledWaveformData source, List<Boolean> barPattern, double barLength) implements SampledWaveformData {
	public static final double[] NO_SOUND = new double[] {0.0, 0.0};

	private static final List<Boolean> ALWAYS_ON = List.of(true);

	public Note(SampledWaveformData source, double barLength, String bitPattern) {
		this(source, from(bitPattern), barLength);
	}

	public static List<Boolean> from(String bitPattern) {
		Objects.requireNonNull(bitPattern, "BitPattern must not be null");
		if (bitPattern.isEmpty()) {
			return ALWAYS_ON;
		}
		Boolean[] barPattern = new Boolean[bitPattern.length()];
		for(int i = 0; i < bitPattern.length(); i++) {
			char c = bitPattern.charAt(i);
			if (c == '1') {
				barPattern[i] = true;
			}
			else {
				barPattern[i] = false;
			}
		}
		return Arrays.asList(barPattern);
	}

	@Override
	public double[] sample(double time) {
		double timeInBar = time % barLength;
		int index = (int)(timeInBar / barLength * barPattern.size());
		if (barPattern.get(index)) {
			return source.sample(timeInBar);
		}
		else {
			return NO_SOUND;
		}
	}
}
