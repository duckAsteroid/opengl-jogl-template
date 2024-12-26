package com.asteroid.duck.opengl.util.audio;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * A rolling or ring buffer can be continuously written to and stores the last N samples.
 * Samples are float data points.
 */
public class RollingFloatBuffer {
	/** Normalised (relative to {@link #max}, mono audio data */
	private final float[] buffer;
	// the current location at which write will occur
	private int writePos;
	// the current location at which read will occur
	private int readPos;
	/** The maximum value. Values will be normalised with respect to this when read */
	private short max = Short.MAX_VALUE;

	public RollingFloatBuffer(int size) {
		this.buffer = new float[size];
	}

	public void setMax(int max) {
		this.max = (short) max;
	}

	public void write(ShortBuffer audio) {
		// take 2 samples at a time from the buffer (L + R)
		while(audio.remaining() > 2) {
			float l = audio.get();
			float r = audio.get();
			// get L and R channels and average them
			// normalise according to current MAX
			float sample = ( l + r) / (2.0f * max);
			if (writePos >= buffer.length) {
				// loop around end of buffer if required
				writePos = 0;
			}
			buffer[writePos++] = sample;
		}
	}

	public void read(FloatBuffer floatBuffer) {
		// what is the most we can read?
		int readExtent = Math.min(floatBuffer.limit(), buffer.length);
		// start position (can be negative)
		int startAt = writePos - readExtent;
		// if the start is negative
		while(startAt < 0) {
			// remove the length of the buffer
			startAt = buffer.length - startAt;
		}
		// transfer data into target buffer
		int x = 0;
		while (floatBuffer.remaining() >= 2) {
			// X - normalised to the extent 0 - 1
			floatBuffer.put(normalise(x, readExtent));
			x++;
			// this will also roll around - so we can put N copies of the data into the buffer
			int index = (startAt + x) % buffer.length;
			// Y - straight from the rolling buffer data
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

	private int decAudioIndex(int audioIndex) {
		audioIndex = audioIndex - 1;
		if (audioIndex < 0) {
			audioIndex = buffer.length - 1;
		}
		return audioIndex;
	}

	public void incMax(int i) {
		this.max = (short) Math.min(max + i, Short.MAX_VALUE);
		System.out.println(max);
	}

	public void decMax(int i) {
		this.max = (short) Math.max(max - i, 0);
		System.out.println(max);
	}
}
