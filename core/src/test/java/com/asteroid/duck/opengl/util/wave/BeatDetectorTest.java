package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.audio.analysis.BeatDetector;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyBand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeatDetectorTest {

    // FFT geometry — matches FrequencyProcessorTest defaults
    private static final int   FFT_SIZE    = 1024;
    private static final float SAMPLE_RATE = 48_000f;
    private static final int   RAW_BINS    = FFT_SIZE / 2 + 1;

    // Small history so tests warm up quickly
    private static final int   HISTORY   = 5;
    private static final float THRESHOLD  = 1.3f;
    private static final float SENSITIVITY = 2.0f;
    private static final float DECAY      = 0.1f;

    private BeatDetector detector(List<FrequencyBand> bands) {
        return new BeatDetector(bands, FFT_SIZE, SAMPLE_RATE,
                HISTORY, THRESHOLD, SENSITIVITY, DECAY);
    }

    private static float[] uniformMagnitudes(float value) {
        float[] m = new float[RAW_BINS];
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
        int[] range = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);
        assertTrue(range[0] >= 1, "bass should start at or after bin 1 (bin 0 is DC)");
        assertTrue(range[1] > range[0], "bass should cover at least one bin");
        assertTrue(range[1] < RAW_BINS / 4, "bass should stay in the low end of the spectrum");
    }

    @Test
    void hiHatSpansHundredsOfRawBins() {
        // Unlike a log-binned display grid, raw linear FFT bins mean a wide band like
        // hi-hat (2kHz-20kHz) legitimately covers hundreds of distinct bins — this
        // resolution is exactly what a small numBins display grid used to throw away.
        int[] range = BeatDetector.computeBinRange(FrequencyBand.HI_HAT, FFT_SIZE, SAMPLE_RATE);
        assertTrue(range[1] - range[0] > 300, "hi-hat should span hundreds of raw bins");
        assertTrue(range[1] <= FFT_SIZE / 2, "hi-hat should not exceed the Nyquist bin");
    }

    @Test
    void bandOutsideFFTRangeIsEmpty() {
        FrequencyBand outOfRange = new FrequencyBand("ultrasound", 30_000f, 40_000f);
        int[] range = BeatDetector.computeBinRange(outOfRange, FFT_SIZE, SAMPLE_RATE);
        assertEquals(range[0], range[1], "out-of-range band should have no bins");
    }

    @Test
    void bandsAreContiguous() {
        // Bass, snare, hi-hat should partition the bin space with no gaps
        int[] bass  = BeatDetector.computeBinRange(FrequencyBand.BASS,   FFT_SIZE, SAMPLE_RATE);
        int[] snare = BeatDetector.computeBinRange(FrequencyBand.SNARE,  FFT_SIZE, SAMPLE_RATE);
        int[] hihat = BeatDetector.computeBinRange(FrequencyBand.HI_HAT, FFT_SIZE, SAMPLE_RATE);
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
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

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
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

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
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

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
