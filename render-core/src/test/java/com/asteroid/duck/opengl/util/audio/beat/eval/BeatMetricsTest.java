package com.asteroid.duck.opengl.util.audio.beat.eval;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BeatMetricsTest {

    /** 120 BPM from 0.5 s to ~30 s. */
    private static final double[] REF = SyntheticTracks.grid(0.5, 120, 60);

    private static double[] shift(double[] times, double by) {
        return Arrays.stream(times).map(t -> t + by).toArray();
    }

    @Test
    void perfectEstimateScoresOne() {
        assertEquals(1.0, BeatMetrics.fMeasure(REF, REF, BeatMetrics.BEAT_WINDOW), 1e-9);
        BeatMetrics.Continuity c = BeatMetrics.continuity(REF, REF);
        assertEquals(1.0, c.cmlc(), 1e-9);
        assertEquals(1.0, c.cmlt(), 1e-9);
        assertEquals(1.0, c.amlt(), 1e-9);
    }

    @Test
    void smallOffsetInsideWindowStillMatches() {
        assertEquals(1.0, BeatMetrics.fMeasure(REF, shift(REF, 0.05), BeatMetrics.BEAT_WINDOW), 1e-9);
        assertEquals(0.0, BeatMetrics.fMeasure(REF, shift(REF, 0.1), BeatMetrics.BEAT_WINDOW), 1e-9);
    }

    @Test
    void emptyEstimateScoresZeroAndNoReferenceIsNaN() {
        assertEquals(0.0, BeatMetrics.fMeasure(REF, new double[0], BeatMetrics.BEAT_WINDOW));
        assertTrue(Double.isNaN(BeatMetrics.fMeasure(new double[0], REF, BeatMetrics.BEAT_WINDOW)));
    }

    @Test
    void doubleTempoIsHalfPrecisionAndOnlyAnyMetricalLevel() {
        double[] doubled = BeatMetrics.doubled(REF);
        assertEquals(2.0 / 3, BeatMetrics.fMeasure(REF, doubled, BeatMetrics.BEAT_WINDOW), 0.01);
        BeatMetrics.Continuity c = BeatMetrics.continuity(REF, doubled);
        assertEquals(0.0, c.cmlt(), 1e-9);
        assertEquals(1.0, c.amlt(), 1e-9);
    }

    @Test
    void halfTempoAndOffBeatOnlyCountAtAnyMetricalLevel() {
        BeatMetrics.Continuity half = BeatMetrics.continuity(REF, BeatMetrics.everyOther(REF, 1));
        assertEquals(0.0, half.cmlt(), 1e-9);
        assertEquals(1.0, half.amlt(), 1e-9);

        BeatMetrics.Continuity offBeat = BeatMetrics.continuity(REF, BeatMetrics.offBeat(REF));
        assertEquals(0.0, offBeat.cmlt(), 1e-9);
        assertEquals(1.0, offBeat.amlt(), 1e-9);
    }

    @Test
    void continuityRunBreaksAtAGap() {
        // Drop the estimated beats for the middle third: longest run ≈ a third, total ≈ two thirds
        double[] gappy = Arrays.stream(REF).filter(t -> t < 10.5 || t >= 20.5).toArray();
        BeatMetrics.Continuity c = BeatMetrics.continuity(REF, gappy);
        assertEquals(1.0 / 3, c.cmlc(), 0.05);
        assertEquals(2.0 / 3, c.cmlt(), 0.05);
    }

    @Test
    void lockTimeIsTheStartOfTheFirstCorrectRun() {
        double[] wrongThenRight = Arrays.stream(REF).map(t -> t < 5 ? t + 0.25 : t).toArray();
        assertEquals(5.0, BeatMetrics.lockTime(REF, wrongThenRight), 1e-9);
        assertTrue(Double.isNaN(BeatMetrics.lockTime(REF, shift(REF, 0.25))));
    }

    @Test
    void trimDropsTheWarmUp() {
        assertTrue(Arrays.stream(BeatMetrics.trim(REF, 5)).allMatch(t -> t >= 5));
        assertEquals(REF.length - 9, BeatMetrics.trim(REF, 5).length);
    }

    @Test
    void tempoAccuracies() {
        assertEquals(1, TempoMetrics.acc1(124, 120));
        assertEquals(0, TempoMetrics.acc1(126, 120));
        assertEquals(0, TempoMetrics.acc1(240, 120));
        assertEquals(1, TempoMetrics.acc2(240, 120));
        assertEquals(1, TempoMetrics.acc2(60, 120));
        assertEquals(1, TempoMetrics.acc2(40, 120));
        assertEquals(0, TempoMetrics.acc2(160, 120));
        assertTrue(Double.isNaN(TempoMetrics.acc1(Double.NaN, 120)));
    }
}
