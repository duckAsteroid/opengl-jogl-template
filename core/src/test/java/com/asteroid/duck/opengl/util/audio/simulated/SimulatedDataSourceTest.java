package com.asteroid.duck.opengl.util.audio.simulated;

import com.asteroid.duck.opengl.util.timer.Timer;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.LineUnavailableException;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.IntSummaryStatistics;
import java.util.stream.IntStream;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;
import static org.junit.jupiter.api.Assertions.*;

class SimulatedDataSourceTest {
	private double elapsedTime = 0;

	private Timer simulatedTimer = new Timer() {

		@Override
		public double elapsed() {
			return elapsedTime;
		}
	};

	private SimulatedDataSource subject = new SimulatedDataSource(simulatedTimer, Waveform.MIDDLE_C.amplify(1000));

	@Test
	public void testNormalFlow() throws LineUnavailableException {
		byte[] readBuffer = new byte[1024];

		subject.open(IDEAL, 1024);
		assertEquals(4, subject.bytesPerSample()); // 2 (stereo) x 16 bits = 4 bytes
		// @start time = 0
		elapsedTime = 0;
		subject.start();

		// time for 10 samples = 10 * (1 / 44,100)
		elapsedTime = 10 * (1 / IDEAL.getSampleRate());
		// verify calculated number of samples is correct...
		assertEquals(10, subject.samples(elapsedTime));

		// now lets test reading
		int bytesRead = subject.read(readBuffer, 0, readBuffer.length);
		// we expect 10 * 16 bit stereo samples
		assertEquals(10 * 2 * 2, bytesRead);
		// convert the read region to a float buffer
		ShortBuffer shortBuffer = ByteBuffer.wrap(readBuffer, 0, bytesRead).asShortBuffer();
		assertEquals(10 * 2, shortBuffer.limit());

		elapsedTime += 256 * (1 / IDEAL.getSampleRate());
		bytesRead = subject.read(readBuffer, 0, readBuffer.length);
		assertEquals(980, bytesRead);
		final ShortBuffer secondBuffer = ByteBuffer.wrap(readBuffer, 0, bytesRead).asShortBuffer();
		assertEquals(490, secondBuffer.limit());
		IntStream intStream = IntStream.range(0, secondBuffer.capacity())
						.map(secondBuffer::get);
		IntSummaryStatistics summary = intStream.summaryStatistics();
		System.out.println(summary);
		assertTrue(summary.getMax() <= 500);
		assertTrue(summary.getMin() >= -500);

		subject.stop();

		subject.close();
	}

}
