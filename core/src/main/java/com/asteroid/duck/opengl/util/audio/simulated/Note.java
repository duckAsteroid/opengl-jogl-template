package com.asteroid.duck.opengl.util.audio.simulated;

import java.util.*;

/**
 * A SampledWaveformData that represents a muscial note.
 * @param source the source of the sound
 * @param barPattern the on/off pattern in the bar
 * @param barLength the length (seconds) of a bar
 */
public record Note(MonoDataSource source, StereoPositioner positioner, List<Boolean> barPattern, double barLength) implements StereoDataSource {
	public static final double[] NO_SOUND = new double[] {0.0, 0.0};

	private static final List<Boolean> ALWAYS_ON = List.of(true);

	public Note(MonoDataSource source, StereoPositioner positioner, double barLength, String bitPattern) {
		this(source, positioner, from(bitPattern), barLength);
	}

	public Note(MonoDataSource source, double length, String pattern) {
		this(source, StaticStereoPositioner.CENTER, length, pattern);
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
			var channelSample = source.sample(time);
			return positioner.sample(channelSample, time);
		}
		else {
			return NO_SOUND;
		}
	}
}
