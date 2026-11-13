package com.asteroid.duck.opengl.util.audio.beat;

import com.asteroid.duck.opengl.util.audio.beat.eval.BeatMetrics;
import com.asteroid.duck.opengl.util.audio.beat.eval.SyntheticTracks;
import com.asteroid.duck.opengl.util.audio.beat.eval.Track;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BeatTrackerTest {

    /** Everything a tracker reported. */
    private static final class Recorded implements BeatListener {
        final List<Double> beats = new ArrayList<>();
        final List<Double> tempos = new ArrayList<>();

        @Override
        public void onBeat(double time) {
            beats.add(time);
        }

        @Override
        public void onTempo(double time, double bpm, float confidence) {
            assertTrue(confidence >= 0 && confidence <= 1);
            tempos.add(bpm);
        }

        double[] beatTimes() {
            return beats.stream().mapToDouble(Double::doubleValue).toArray();
        }

        double lastTempo() {
            return tempos.getLast();
        }
    }

    private static Recorded track(float[] samples, float sampleRate) {
        Recorded recorded = new Recorded();
        BeatTracker tracker = new BeatTracker(sampleRate, recorded);
        for (int offset = 0; offset < samples.length; offset += 512) {
            tracker.process(samples, offset, Math.min(512, samples.length - offset));
        }
        return recorded;
    }

    /** Short decaying 1 kHz clicks at {@code bpm}, starting at 0.5 s. */
    private static float[] clicks(float sampleRate, double bpm, double seconds) {
        float[] samples = new float[(int) (seconds * sampleRate)];
        for (double t0 = 0.5; t0 < seconds - 0.1; t0 += 60 / bpm) {
            int start = (int) Math.round(t0 * sampleRate);
            for (int i = 0; i < 0.02 * sampleRate; i++) {
                double t = i / sampleRate;
                samples[start + i] += (float) (0.8 * Math.sin(2 * Math.PI * 1000 * t) * Math.exp(-t / 0.005));
            }
        }
        return samples;
    }

    private static double[] grid(double bpm, double seconds) {
        List<Double> beats = new ArrayList<>();
        for (double t = 0.5; t < seconds - 0.1; t += 60 / bpm) beats.add(t);
        return beats.stream().mapToDouble(Double::doubleValue).toArray();
    }

    @Test
    void tracksADanceTrack() throws IOException {
        Track four = SyntheticTracks.all().stream().filter(t -> t.name().equals("four-on-floor-128")).findFirst().orElseThrow();
        Track.Loaded loaded = four.loader().load();
        Recorded recorded = track(loaded.audio().samples(), loaded.audio().sampleRate());

        assertEquals(128, recorded.lastTempo(), 128 * 0.02);
        double f = BeatMetrics.fMeasure(BeatMetrics.trim(loaded.annotations().beats(), BeatMetrics.WARM_UP),
                BeatMetrics.trim(recorded.beatTimes(), BeatMetrics.WARM_UP), BeatMetrics.BEAT_WINDOW);
        assertTrue(f > 0.95, "beat F " + f);
    }

    @Test
    void staysOnTheBeatAtSampleRatesWithFractionalHops() {
        // At 22.05 kHz the 10 ms hop is 220.5 samples, rounded to 221: treating frames as exactly
        // 10 ms apart made beats drift ~2 ms per second early. They must stay centred.
        float sampleRate = 22_050f;
        double seconds = 40;
        Recorded recorded = track(clicks(sampleRate, 120, seconds), sampleRate);
        double[] reference = grid(120, seconds);

        double[] late = BeatMetrics.trim(recorded.beatTimes(), 20);
        assertTrue(late.length > 30);
        double[] errors = Arrays.stream(late).map(b -> {
            double nearest = Arrays.stream(reference).reduce((x, y) -> Math.abs(x - b) < Math.abs(y - b) ? x : y).orElseThrow();
            return b - nearest;
        }).sorted().toArray();
        double median = errors[errors.length / 2];
        assertEquals(0, median, 0.015, "median beat error " + median);
        assertEquals(120, recorded.lastTempo(), 120 * 0.01);
    }

    @Test
    void reportsNothingFromSilence() {
        Recorded recorded = track(new float[48_000 * 10], 48_000f);
        assertTrue(recorded.beats.isEmpty());
    }

    @Test
    void spectralFluxReportsItsExactFrameRate() {
        SpectralFlux flux = new SpectralFlux(22_050f, (t, v) -> { });
        assertEquals(22_050.0 / 221, flux.frameRate(), 1e-9);
        assertEquals(100.0, new SpectralFlux(48_000f, (t, v) -> { }).frameRate(), 1e-9);
    }

    @Test
    void tempoEstimatorFindsAnImpulseTrain() {
        double frameRate = 100;
        TempoEstimator estimator = new TempoEstimator(frameRate, 120, 1.0);
        double bpm = 125;
        double period = 60 * frameRate / bpm;
        for (int i = 0; i < 10 * frameRate; i++) {
            double phase = (i % period);
            estimator.add(phase < 1 ? 1f : 0f);
        }
        assertTrue(estimator.estimate());
        assertEquals(bpm, estimator.bpm(), bpm * 0.02);
    }
}
