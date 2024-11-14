package com.asteroid.duck.opengl.util.audio;

import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class RollingFloatBufferTest {
	RollingFloatBuffer subject = new RollingFloatBuffer(5);

	public static ShortBuffer generateAudio(int samples, Function<Integer, Short> generator) {
		short[] data = new short[samples * 2];
		for (int i = 0; i < data.length / 2; i++) {
			int index = i * 2;
			short sample = generator.apply(index);
			data[index] = sample;
			data[index + 1] = sample;
		}
		return ShortBuffer.wrap(data);
	}


	@Test
	public void test() {
		subject.setMax(5);
		ShortBuffer audio = generateAudio(10, (i)-> (short) (i % 5));
		for (int i = 0; i < audio.limit() / 2; i++) {
			int index = i * 2;
			System.out.println(audio.get(index)+","+ audio.get(index + 1));
		}
		System.out.println();
		subject.write(audio);
		FloatBuffer pixels = FloatBuffer.allocate(10);
		subject.read(pixels);
		for (int i = 0; i < pixels.limit() / 2; i++) {
			int index = i * 2;
			System.out.println(pixels.get(index) +"="+pixels.get(index+1));
		}
	}

	@Test
	public void testNormalise() {
		float x = RollingFloatBuffer.normalise(10,10.0f);
		assertEquals(1.0f, x);
		x = RollingFloatBuffer.normalise(5,10.0f);
		assertEquals(0.0f, x);
		x = RollingFloatBuffer.normalise(0,10.0f);
		assertEquals(-1.0f, x);
	}

}
