package com.asteroid.duck.opengl.util.wave;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeatDetectorTest {

    // FFT geometry matching SpectrumAnalyser defaults
    private static final int   NUM_BINS  = 128;
    private static final float FFT_F_MIN = 20f;
    private static final float FFT_F_MAX = 20_000f;

    // Small history so tests warm up quickly
    private static final int   HISTORY   = 5;
    private static final float THRESHOLD  = 1.3f;
    private static final float SENSITIVITY = 2.0f;
    private static final float DECAY      = 0.1f;

    private BeatDetector detector(List<FrequencyBand> bands) {
        return new BeatDetector(bands, NUM_BINS, FFT_F_MIN, FFT_F_MAX,
                HISTORY, THRESHOLD, SENSITIVITY, DECAY);
    }

    private static float[] uniformMagnitudes(float value) {
        float[] m = new float[NUM_BINS];
        java.util.Arrays.fill(m, value);
        return m;
    }

    private static float[] spikeMagnitudes(float base, float spike, int binLow, int binHigh) {
        float[] m = uniformMagnitudes(base);
        for (int i = binLow; i < binHigh; i++) m[i] = spike;
        return m;
    }

    // ── Bin-range mapping ────────────────────────────────────────────────────────

    @Test
    void bassCoversLowBins() {
        int[] range = BeatDetector.computeBinRange(FrequencyBand.BASS, NUM_BINS, FFT_F_MIN, FFT_F_MAX);
        assertEquals(0, range[0], "bass should start at bin 0");
        assertTrue(range[1] > range[0], "bass should cover at least one bin");
        assertTrue(range[1] < NUM_BINS, "bass should not reach the top bin");
    }

    @Test
    void hiHatCoversHighBins() {
        int[] range = BeatDetector.computeBinRange(FrequencyBand.HI_HAT, NUM_BINS, FFT_F_MIN, FFT_F_MAX);
        assertTrue(range[0] > NUM_BINS / 2, "hi-hat should start in the upper half");
        assertEquals(NUM_BINS, range[1], "hi-hat should reach the last bin");
    }

    @Test
    void bandOutsideFFTRangeIsEmpty() {
        FrequencyBand outOfRange = new FrequencyBand("infrasound", 1f, 15f);
        int[] range = BeatDetector.computeBinRange(outOfRange, NUM_BINS, FFT_F_MIN, FFT_F_MAX);
        assertEquals(range[0], range[1], "out-of-range band should have no bins");
    }

    @Test
    void bandsAreContiguous() {
        // Bass, snare, hi-hat should partition the bin space with no gaps
        int[] bass  = BeatDetector.computeBinRange(FrequencyBand.BASS,   NUM_BINS, FFT_F_MIN, FFT_F_MAX);
        int[] snare = BeatDetector.computeBinRange(FrequencyBand.SNARE,  NUM_BINS, FFT_F_MIN, FFT_F_MAX);
        int[] hihat = BeatDetector.computeBinRange(FrequencyBand.HI_HAT, NUM_BINS, FFT_F_MIN, FFT_F_MAX);
        assertEquals(bass[1],  snare[0], "bass/snare boundary should be contiguous");
        assertEquals(snare[1], hihat[0], "snare/hi-hat boundary should be contiguous");
    }

    // ── Beat detection ───────────────────────────────────────────────────────────

    @Test
    void silenceProducesNoBeat() {
        BeatDetector d = detector(FrequencyBand.defaults());
        float[] silence = uniformMagnitudes(0f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(silence);
        for (int b = 0; b < d.getBandCount(); b++) {
            assertEquals(0f, d.getBeatStrength(b), 1e-6f, "silence should produce no beat");
        }
    }

    @Test
    void bassSpikeTriggersBass() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, NUM_BINS, FFT_F_MIN, FFT_F_MAX);

        // Warm up with baseline — extra frames let the startup transient decay to zero
        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(baseline);

        // Spike bass by 3×
        d.update(spikeMagnitudes(0.2f, 0.6f, bassRange[0], bassRange[1]));

        assertTrue(d.getBeatStrength("bass") > 0f, "bass spike should trigger bass beat");
    }

    @Test
    void bassSpikeLeavesSnareUnaffected() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, NUM_BINS, FFT_F_MIN, FFT_F_MAX);

        // Extra frames let the startup transient decay to zero before the spike
        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(baseline);

        // Spike only bass bins, keep snare at baseline
        d.update(spikeMagnitudes(0.2f, 0.8f, bassRange[0], bassRange[1]));

        assertEquals(0f, d.getBeatStrength("snare"), 1e-6f,
                "bass-only spike should not trigger snare");
    }

    @Test
    void beatStrengthDecays() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, NUM_BINS, FFT_F_MIN, FFT_F_MAX);

        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 2; i++) d.update(baseline);
        d.update(spikeMagnitudes(0.2f, 0.8f, bassRange[0], bassRange[1]));

        float afterSpike = d.getBeatStrength("bass");
        assertTrue(afterSpike > 0f, "beat strength should be non-zero after spike");

        // Return to baseline; strength should decay each frame
        for (int i = 0; i < 3; i++) d.update(baseline);
        assertTrue(d.getBeatStrength("bass") < afterSpike,
                "beat strength should decay when energy returns to baseline");
    }

    @Test
    void getBeatStrengthByIndex() {
        BeatDetector d = detector(FrequencyBand.defaults());
        d.update(uniformMagnitudes(0f));
        assertEquals(d.getBeatStrength(0), d.getBeatStrength("bass"));
        assertEquals(d.getBeatStrength(1), d.getBeatStrength("snare"));
        assertEquals(d.getBeatStrength(2), d.getBeatStrength("hihat"));
    }

    @Test
    void getBeatStrengthsLength() {
        BeatDetector d = detector(FrequencyBand.defaults());
        d.update(uniformMagnitudes(0f));
        assertEquals(3, d.getBeatStrengths().length);
    }

    @Test
    void unknownBandNameThrows() {
        BeatDetector d = detector(FrequencyBand.defaults());
        assertThrows(IllegalArgumentException.class, () -> d.getBeatStrength("theremin"));
    }

    @Test
    void customBandsWork() {
        List<FrequencyBand> custom = List.of(
                new FrequencyBand("sub",  20f,  80f),
                new FrequencyBand("kick", 80f, 200f)
        );
        BeatDetector d = detector(custom);
        assertEquals(2, d.getBandCount());
        d.update(uniformMagnitudes(0f));
        assertEquals(0f, d.getBeatStrength("sub"),  1e-6f);
        assertEquals(0f, d.getBeatStrength("kick"), 1e-6f);
    }
}
