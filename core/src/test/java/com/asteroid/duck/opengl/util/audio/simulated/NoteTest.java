package com.asteroid.duck.opengl.util.audio.simulated;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NoteTest {
	private final static double[] FIXED_SAMPLE = new double[]{ 1.0, -1.0};
	private final static SampledWaveformData FIXED = (t) -> FIXED_SAMPLE;

	@Test
	void from() {
		final List<Boolean> actual = Note.from("1010");
		final List<Boolean> expected = List.of(true, false, true, false);
		assertIterableEquals(expected, actual);
	}

	@Test
	void sample() {
		Note n = new Note(FIXED, 1, "10");
		assertArrayEquals(FIXED_SAMPLE, n.sample(millis(0)));
		assertArrayEquals(FIXED_SAMPLE, n.sample(millis(499)));
		assertArrayEquals(Note.NO_SOUND, n.sample(millis(501)));
		assertArrayEquals(Note.NO_SOUND, n.sample(millis(999)));
		assertArrayEquals(FIXED_SAMPLE, n.sample(millis(1000)));
	}

	private static double millis(long millis) {
		return millis / 1000.0;
	}
}
