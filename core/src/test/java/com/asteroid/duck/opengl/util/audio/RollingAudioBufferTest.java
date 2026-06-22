package com.asteroid.duck.opengl.util.audio;

import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class RollingAudioBufferTest {

    /** 5 stereo frames capacity. */
    RollingAudioBuffer subject = new RollingAudioBuffer(5);

    /** Build raw little-endian 16-bit stereo PCM bytes. L and R are set to the same value. */
    static byte[] generateAudio(int stereoFrames, IntUnaryOperator sampleValue) {
        byte[] data = new byte[stereoFrames * 4];
        for (int i = 0; i < stereoFrames; i++) {
            short s = (short) sampleValue.applyAsInt(i);
            // L
            data[i * 4]     = (byte) (s & 0xFF);
            data[i * 4 + 1] = (byte) ((s >> 8) & 0xFF);
            // R (same as L)
            data[i * 4 + 2] = (byte) (s & 0xFF);
            data[i * 4 + 3] = (byte) ((s >> 8) & 0xFF);
        }
        return data;
    }

    @Test
    void readSamplesLeft() {
        short value = 1000;
        byte[] audio = generateAudio(3, i -> value);
        subject.write(audio, 0, audio.length);

        float[] samples = new float[3];
        subject.readSamples(samples, 3, ChannelMode.LEFT);

        float expected = value / (float) Short.MAX_VALUE;
        for (float s : samples) {
            assertEquals(expected, s, 0.0001f);
        }
    }

    @Test
    void readSamplesMonoBlendMatchesLeftWhenSymmetric() {
        byte[] audio = generateAudio(3, i -> (i + 1) * 1000);
        subject.write(audio, 0, audio.length);

        float[] left  = new float[3];
        float[] blend = new float[3];
        subject.readSamples(left,  3, ChannelMode.LEFT);
        subject.readSamples(blend, 3, ChannelMode.MONO_BLEND);

        // When L == R, blend should equal left exactly.
        assertArrayEquals(left, blend, 0.0001f);
    }

    @Test
    void readWrapsAroundRingBuffer() {
        // Write 7 frames into a 5-frame buffer — the first 2 get overwritten.
        byte[] audio = generateAudio(7, i -> (short) (i * 1000));
        subject.write(audio, 0, audio.length);

        // We can only read the most recent 5 frames (buffer capacity).
        float[] samples = new float[5];
        subject.readSamples(samples, 5, ChannelMode.LEFT);

        // Expected: frames 2..6 (values 2000..6000), oldest first.
        for (int i = 0; i < 5; i++) {
            float expected = ((i + 2) * 1000) / (float) Short.MAX_VALUE;
            assertEquals(expected, samples[i], 0.0001f);
        }
    }

    @Test
    void readXYPairs() {
        byte[] audio = generateAudio(4, i -> (short) (i * 8192));
        subject.write(audio, 0, audio.length);

        FloatBuffer dest = FloatBuffer.allocate(8); // 4 X,Y pairs
        subject.read(dest, ChannelMode.LEFT);
        dest.flip();

        assertEquals(8, dest.limit());
        // X values: normalise(i, 4) = (i/2.0) - 1
        assertEquals(-1.0f, dest.get(0), 0.0001f); // X of pair 0: normalise(0,4) = -1.0
        assertEquals( 0.5f, dest.get(6), 0.0001f); // X of pair 3: normalise(3,4) = 0.5
    }

    @Test
    void normalise() {
        assertEquals( 1.0f, RollingAudioBuffer.normalise(10, 10.0f), 0.0001f);
        assertEquals( 0.0f, RollingAudioBuffer.normalise(5,  10.0f), 0.0001f);
        assertEquals(-1.0f, RollingAudioBuffer.normalise(0,  10.0f), 0.0001f);
    }
}
