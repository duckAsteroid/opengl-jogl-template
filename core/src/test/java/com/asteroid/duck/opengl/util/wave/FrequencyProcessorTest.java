package com.asteroid.duck.opengl.util.wave;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrequencyProcessorTest {

    private static final int   FFT_SIZE    = 1024;
    private static final int   NUM_BINS    = 128;
    private static final float SAMPLE_RATE = 48_000f;
    private static final float F_MIN       = 20f;
    private static final float F_MAX       = 20_000f;

    private FrequencyProcessor processor() {
        return new FrequencyProcessor(FFT_SIZE, NUM_BINS, SAMPLE_RATE, F_MIN, F_MAX, -80f, 0f);
    }

    /** Captures every onSpectrum call for inspection. */
    static class CaptureSink implements FrequencySink {
        final List<float[]> received = new ArrayList<>();

        @Override
        public void onSpectrum(float[] magnitudes) {
            float[] copy = new float[magnitudes.length];
            System.arraycopy(magnitudes, 0, copy, 0, magnitudes.length);
            received.add(copy);
        }
    }

    // ── Parameter accessors ──────────────────────────────────────────────────────

    @Test
    void accessorsMatchConstruction() {
        FrequencyProcessor p = processor();
        assertEquals(NUM_BINS,    p.getNumBins());
        assertEquals(FFT_SIZE,    p.getFftSize());
        assertEquals(F_MIN,       p.getFMin());
        assertEquals(F_MAX,       p.getFMax());
    }

    // ── Fan-out ──────────────────────────────────────────────────────────────────

    @Test
    void processFansOutToAllSinks() {
        FrequencyProcessor p = processor();
        CaptureSink sinkA = new CaptureSink();
        CaptureSink sinkB = new CaptureSink();
        p.addSink(sinkA);
        p.addSink(sinkB);

        p.process();

        assertEquals(1, sinkA.received.size(), "sinkA should be called once");
        assertEquals(1, sinkB.received.size(), "sinkB should be called once");
    }

    @Test
    void processDeliversNumBinsMagnitudes() {
        FrequencyProcessor p = processor();
        CaptureSink sink = new CaptureSink();
        p.addSink(sink);

        p.process();

        assertEquals(NUM_BINS, sink.received.get(0).length);
    }

    @Test
    void removeSinkStopsDelivery() {
        FrequencyProcessor p = processor();
        CaptureSink sink = new CaptureSink();
        p.addSink(sink);
        p.removeSink(sink);

        p.process();

        assertTrue(sink.received.isEmpty(), "removed sink should not receive calls");
    }

    @Test
    void silenceProducesZeroMagnitudes() {
        FrequencyProcessor p = processor();
        CaptureSink sink = new CaptureSink();
        p.addSink(sink);

        // No audio written — buffer is all zeros → FFT of silence → magnitudes all 0
        p.process();

        float[] mags = sink.received.get(0);
        for (float m : mags) {
            assertEquals(0f, m, 1e-6f, "silence should produce zero magnitudes");
        }
    }

    @Test
    void multipleProcessCallsNotifyEachFrame() {
        FrequencyProcessor p = processor();
        CaptureSink sink = new CaptureSink();
        p.addSink(sink);

        p.process();
        p.process();
        p.process();

        assertEquals(3, sink.received.size());
    }

    // ── BeatDetector integration ─────────────────────────────────────────────────

    @Test
    void beatDetectorFromProcessorMatchesGeometry() {
        FrequencyProcessor p = processor();
        BeatDetector beats = new BeatDetector(p);
        p.addSink(beats);

        p.process();

        assertEquals(3, beats.getBandCount());
        assertEquals(3, beats.getBeatStrengths().length);
    }

    @Test
    void beatDetectorReceivesUpdatesViaProcessor() {
        FrequencyProcessor p = processor();
        BeatDetector beats = new BeatDetector(p);
        p.addSink(beats);

        // Calling process() should drive beat detection (silence → no exception)
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 10; i++) p.process();
        });
    }
}
