package com.asteriod.duck.opengl.util.audio;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.List;

public class RollingFloatBuffer {
	/** Normalised (relative to {@link #max}, mono audio data */
	private final float[] buffer;
	private int writePos;

	private short max = Short.MAX_VALUE;

	public RollingFloatBuffer(int size) {
		this.buffer = new float[size];
	}

	public void setMax(int max) {
		this.max = (short) max;
	}

	public void write(ShortBuffer audio) {
		while(audio.remaining() > 2) {
			float sample = (audio.get() + audio.get()) / (2.0f * max);
			if (writePos >= buffer.length) {
				writePos = 0;
			}
			buffer[writePos++] = sample;
		}
	}

	public void read(FloatBuffer floatBuffer) {
		int readExtent = Math.min(floatBuffer.limit(), buffer.length);
		int startAt = writePos - readExtent;
		while(startAt < 0) {
			startAt = buffer.length - startAt;
		}
		int x = 0;
		while (floatBuffer.remaining() >= 2) {
			floatBuffer.put(normalise(x, readExtent));
			x++;
			int index = (startAt + x) % buffer.length;
			floatBuffer.put(buffer[index]);
		}
	}

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
