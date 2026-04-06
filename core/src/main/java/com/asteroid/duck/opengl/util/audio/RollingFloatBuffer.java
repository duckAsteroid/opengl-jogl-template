package com.asteroid.duck.opengl.util.audio;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * A rolling or ring buffer can be continuously written to and stores the last N samples.
 * Samples are float data points.
 * This class is thread-safe for concurrent write and read operations.
 */
public class RollingFloatBuffer {
	/** Normalised (relative to {@link #max}, mono audio data */
	private final float[] buffer;
	// the current location at which write will occur
	private volatile int writePos;
	/** The maximum value. Values will be normalised with respect to this when read */
	private volatile short max = Short.MAX_VALUE;

	public RollingFloatBuffer(int size) {
		this.buffer = new float[size];
	}

	public void setMax(int max) {
		this.max = (short) max;
	}

	/**
	 * Write to this buffer by reading from the audio buffer.
	 * Each pair of short values is L/R channel from one sample
	 * The audio values are averaged into a single value for storage.
	 * They are also normalised based on an arbitrary MAX.
	 * @param audio the audio buffer containing L/R channel pairs
	 */
	public void write(ShortBuffer audio) {
		// take 2 samples at a time from the buffer (L + R)
		while(audio.remaining() >= 2) {
			// each get is actually a short, cast to float
			// get L and R channels
			float l = audio.get();
			float r = audio.get();
			// and average them
			// while normalising according to current MAX
			float sample = ( l + r) / (2.0f * max);
			if (writePos >= buffer.length) {
				// loop around end of buffer if required
				writePos = 0;
			}
			buffer[writePos++] = sample;
		}
	}

	/**
	 * Read from this buffer into the float (display) buffer.
	 * The values are read out in pairs:
	 * an X value - normalised 0-1, based on how far through the read
	 * a Y value - normalised audio amplitude
	 * @param floatBuffer the target buffer to fill with X,Y coordinate pairs
	 */
	public void read(FloatBuffer floatBuffer) {
		// Capture writePos atomically to ensure consistent read
		final int currentWritePos = writePos;

		// what is the most we can read?
		int readExtent = Math.min(floatBuffer.limit() / 2, buffer.length);
		// start position (can be negative)
		int startAt = currentWritePos - readExtent;
		// if the start is negative, wrap around
		if(startAt < 0) {
			startAt = buffer.length + startAt;
		}

		// transfer data into target buffer
		for (int x = 0; x < readExtent && floatBuffer.remaining() >= 2; x++) {
			// X - normalised to the extent -1 to +1
			floatBuffer.put(normalise(x, readExtent));
			// Y - straight from the rolling buffer data
			int index = (startAt + x) % buffer.length;
			floatBuffer.put(buffer[index]);
		}
	}

	/**
	 * Turns a value in the range 0 to size into a value in the range
	 * -1 to +1.
	 * @param x the value to normalise
	 * @param size the scale against which to normalise
	 * @return a normalised value corresponding to x
	 */
	static float normalise(float x, float size) {
		return (x / (size / 2.0f)) - 1;
	}

	/**
	 * Increase the maximum audio value used for normalization.
	 * @param i the amount to increase by
	 */
	public void incMax(int i) {
		this.max = (short) Math.min(max + i, Short.MAX_VALUE);
	}

	/**
	 * Decrease the maximum audio value used for normalization.
	 * @param i the amount to decrease by
	 */
	public void decMax(int i) {
		this.max = (short) Math.max(max - i, 0);
	}
}
