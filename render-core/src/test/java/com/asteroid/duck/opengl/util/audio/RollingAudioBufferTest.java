package com.asteroid.duck.opengl.util.audio;

import org.junit.jupiter.api.Test;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * Stress test for issue #27: a writer lapping the buffer flat-out while the reader reads the
     * whole capacity. The writer emits a triangle wave whose adjacent samples always differ by
     * exactly 1, so any larger step in what the reader sees is a torn or overwritten read.
     */
    @Test
    void concurrentReadNeverSeesTornOrOverwrittenSamples() throws InterruptedException {
        int capacityFrames = 256;
        RollingAudioBuffer buffer = new RollingAudioBuffer(capacityFrames);
        int chunkFrames = 16; // AudioReader.CHUNK_SIZE
        AtomicBoolean stop = new AtomicBoolean();
        Thread writer = Thread.ofPlatform().daemon().start(() -> {
            byte[] chunk = new byte[chunkFrames * 4];
            int frame = 0;
            while (!stop.get()) {
                for (int i = 0; i < chunkFrames; i++, frame++) {
                    short s = triangle(frame);
                    chunk[i * 4]     = (byte) (s & 0xFF);
                    chunk[i * 4 + 1] = (byte) ((s >> 8) & 0xFF);
                    chunk[i * 4 + 2] = chunk[i * 4];
                    chunk[i * 4 + 3] = chunk[i * 4 + 1];
                }
                buffer.write(chunk, 0, chunk.length);
            }
        });

        // Let the writer lap the buffer at least once before reading.
        while (!writer.isAlive()) Thread.onSpinWait();
        Thread.sleep(10);

        float[] samples = new float[capacityFrames];
        int violations = 0;
        String firstViolation = null;
        long deadline = System.nanoTime() + 500_000_000L;
        while (System.nanoTime() < deadline) {
            buffer.readSamples(samples, capacityFrames, ChannelMode.LEFT);
            for (int i = 1; i < capacityFrames; i++) {
                int prev = Math.round(samples[i - 1] * Short.MAX_VALUE);
                int curr = Math.round(samples[i] * Short.MAX_VALUE);
                if (Math.abs(curr - prev) > 1) {
                    if (firstViolation == null) {
                        firstViolation = "sample " + i + ": " + prev + " -> " + curr;
                    }
                    violations++;
                }
            }
        }
        stop.set(true);
        writer.join();

        assertEquals(0, violations, "discontinuities in read samples, first at " + firstViolation);
    }

    /** Triangle wave in [-1000, 1000] with a step of exactly 1 between adjacent frames. */
    private static short triangle(int frame) {
        int phase = Math.floorMod(frame, 4000);
        return (short) (phase < 2000 ? phase - 1000 : 3000 - phase);
    }

    @Test
    void normalise() {
        assertEquals( 1.0f, RollingAudioBuffer.normalise(10, 10.0f), 0.0001f);
        assertEquals( 0.0f, RollingAudioBuffer.normalise(5,  10.0f), 0.0001f);
        assertEquals(-1.0f, RollingAudioBuffer.normalise(0,  10.0f), 0.0001f);
    }
}
