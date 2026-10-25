package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.audio.analysis.BeatDetector;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyBand;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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
    private static final float NOISE_FLOOR = 0.02f;
    // Full scale decays to zero in 1/6 s — the old 0.1 per frame at 60 fps
    private static final Duration DECAY_TIME = Duration.ofNanos(1_000_000_000L / 6);
    private static final double   FRAME      = 1.0 / 60;

    private BeatDetector detector(List<FrequencyBand> bands) {
        return detector(bands, DECAY_TIME);
    }

    private BeatDetector detector(List<FrequencyBand> bands, Duration decayTime) {
        return new BeatDetector(bands, FFT_SIZE, SAMPLE_RATE,
                HISTORY, THRESHOLD, SENSITIVITY, NOISE_FLOOR, decayTime);
    }

    /** Warm up on a 0.2 baseline at 60 fps, then spike the bass band to full beat strength. */
    private static void warmUpAndSpikeBass(BeatDetector d) {
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);
        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(baseline, FRAME);
        d.update(spikeMagnitudes(0.2f, 0.8f, bassRange[0], bassRange[1]), FRAME);
    }

    /** Feed the 0.2 baseline at {@code hz} updates per second for {@code seconds}. */
    private static void runBaseline(BeatDetector d, int hz, double seconds) {
        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < Math.round(seconds * hz); i++) d.update(baseline, 1.0 / hz);
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
        for (int i = 0; i < HISTORY * 3; i++) d.update(silence, FRAME);
        for (int b = 0; b < d.getBandCount(); b++) {
            assertEquals(0f, d.getBeatStrength(b), 1e-6f, "silence should produce no beat");
        }
    }

    @Test
    void quietNoiseFluctuationDoesNotTriggerFalseBeat() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

        // Warm up on a near-silent baseline, well below the noise floor
        float[] quiet = uniformMagnitudes(0.001f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(quiet, FRAME);

        // 3x relative spike — comfortably above the ratio threshold — but still far below the
        // absolute noise floor. Without the noiseFloor gate this would falsely trigger.
        d.update(spikeMagnitudes(0.001f, 0.003f, bassRange[0], bassRange[1]), FRAME);

        assertEquals(0f, d.getBeatStrength("bass"), 1e-6f,
                "sub-noise-floor fluctuation should not trigger a beat even with a high ratio");
    }

    @Test
    void bassSpikeTriggersBass() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

        // Warm up with baseline — extra frames let the startup transient decay to zero
        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(baseline, FRAME);

        // Spike bass by 3×
        d.update(spikeMagnitudes(0.2f, 0.6f, bassRange[0], bassRange[1]), FRAME);

        assertTrue(d.getBeatStrength("bass") > 0f, "bass spike should trigger bass beat");
    }

    @Test
    void bassSpikeLeavesSnareUnaffected() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

        // Extra frames let the startup transient decay to zero before the spike
        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 3; i++) d.update(baseline, FRAME);

        // Spike only bass bins, keep snare at baseline
        d.update(spikeMagnitudes(0.2f, 0.8f, bassRange[0], bassRange[1]), FRAME);

        assertEquals(0f, d.getBeatStrength("snare"), 1e-6f,
                "bass-only spike should not trigger snare");
    }

    @Test
    void beatStrengthDecays() {
        BeatDetector d = detector(FrequencyBand.defaults());
        int[] bassRange = BeatDetector.computeBinRange(FrequencyBand.BASS, FFT_SIZE, SAMPLE_RATE);

        float[] baseline = uniformMagnitudes(0.2f);
        for (int i = 0; i < HISTORY * 2; i++) d.update(baseline, FRAME);
        d.update(spikeMagnitudes(0.2f, 0.8f, bassRange[0], bassRange[1]), FRAME);

        float afterSpike = d.getBeatStrength("bass");
        assertTrue(afterSpike > 0f, "beat strength should be non-zero after spike");

        // Return to baseline; strength should decay each frame
        for (int i = 0; i < 3; i++) d.update(baseline, FRAME);
        assertTrue(d.getBeatStrength("bass") < afterSpike,
                "beat strength should decay when energy returns to baseline");
    }

    @Test
    void decayRateIsIndependentOfUpdateRate() {
        BeatDetector at60  = detector(FrequencyBand.defaults());
        BeatDetector at240 = detector(FrequencyBand.defaults());
        warmUpAndSpikeBass(at60);
        warmUpAndSpikeBass(at240);
        assertEquals(1f, at60.getBeatStrength("bass"), 1e-6f, "spike should saturate");

        runBaseline(at60,   60, 0.1);
        runBaseline(at240, 240, 0.1);

        // 0.1 s of a 1/6 s full-scale decay takes 0.6 off, whatever the update rate
        assertEquals(0.4f, at60.getBeatStrength("bass"),  1e-4f);
        assertEquals(0.4f, at240.getBeatStrength("bass"), 1e-4f);
    }

    @Test
    void longGapBetweenUpdatesIsClamped() {
        BeatDetector d = detector(FrequencyBand.defaults(), Duration.ofSeconds(10));
        warmUpAndSpikeBass(d);

        d.update(uniformMagnitudes(0.2f), 10.0);

        // A 10 s stall decays only MAX_UPDATE_INTERVAL worth: 0.25 s / 10 s = 0.025
        assertEquals(1f - 0.025f, d.getBeatStrength("bass"), 1e-4f);
    }

    @Test
    @SuppressWarnings("deprecation")
    void perFrameConstructorAssumesSixtyFps() {
        BeatDetector perFrame = new BeatDetector(FrequencyBand.defaults(), FFT_SIZE, SAMPLE_RATE,
                HISTORY, THRESHOLD, SENSITIVITY, NOISE_FLOOR, 0.1f);
        warmUpAndSpikeBass(perFrame);

        runBaseline(perFrame, 60, 0.1);

        assertEquals(0.4f, perFrame.getBeatStrength("bass"), 1e-4f);
    }

    @Test
    void nonPositiveDecayTimeIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> detector(FrequencyBand.defaults(), Duration.ZERO));
    }

    @Test
    void getBeatStrengthByIndex() {
        BeatDetector d = detector(FrequencyBand.defaults());
        d.update(uniformMagnitudes(0f), FRAME);
        assertEquals(d.getBeatStrength(0), d.getBeatStrength("bass"));
        assertEquals(d.getBeatStrength(1), d.getBeatStrength("snare"));
        assertEquals(d.getBeatStrength(2), d.getBeatStrength("hihat"));
    }

    @Test
    void getBeatStrengthsLength() {
        BeatDetector d = detector(FrequencyBand.defaults());
        d.update(uniformMagnitudes(0f), FRAME);
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
        d.update(uniformMagnitudes(0f), FRAME);
        assertEquals(0f, d.getBeatStrength("sub"),  1e-6f);
        assertEquals(0f, d.getBeatStrength("kick"), 1e-6f);
    }
}
