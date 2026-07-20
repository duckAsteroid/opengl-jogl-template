package com.asteroid.duck.opengl.util.audio.simulated;

import com.asteroid.duck.opengl.util.timer.Clock;
import org.junit.jupiter.api.Test;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulatedAudioLineTest {

	private double elapsedTime = 0;

	private final Clock simulatedTimer = new Clock() {
		@Override
		public double elapsed() {
			return elapsedTime;
		}
	};

	/**
	 * Regression test for a unit-mismatch bug: {@code buffer.remaining()} is measured in shorts
	 * while {@code samples} is measured in stereo frames. When the number of theoretically
	 * available frames exceeded what the read buffer could hold, but was still less than the
	 * buffer's raw short-capacity, the old code shrunk the buffer's limit to
	 * {@code samples * floatsPerSample()} -- a value that could exceed the buffer's actual
	 * capacity, throwing IllegalArgumentException from ShortBuffer.limit().
	 */
	@Test
	public void readDoesNotOverflowBufferWhenFewerFramesFitThanAreTheoreticallyAvailable() {
		// a large line buffer so the theoretical sample count is driven by elapsed time,
		// not capped by the line's own capacity
		SimulatedAudioLine line = new SimulatedAudioLine(simulatedTimer, time -> new double[]{0, 0}, IDEAL, 4096);
		line.start();

		// ~23 stereo frames are "available" per elapsed time, but the read buffer below can
		// only hold 16 frames (64 bytes / 4 bytes-per-frame) -- more frames than the read
		// buffer holds, but fewer than its raw short-capacity (32).
		double sampleRate = IDEAL.getSampleRate();
		elapsedTime = 23.5 / sampleRate;
		assertEquals(23, line.samples(elapsedTime));

		byte[] readBuffer = new byte[64];
		int bytesRead = assertDoesNotThrow(() -> line.read(readBuffer, 0, readBuffer.length));

		assertEquals(16 * line.bytesPerSample(), bytesRead);
	}
}
