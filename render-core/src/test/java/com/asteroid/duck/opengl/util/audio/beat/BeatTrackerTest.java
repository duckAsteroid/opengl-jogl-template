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
        /** {time, bpm, confidence} for every tempo report. */
        final List<double[]> tempoReports = new ArrayList<>();

        @Override
        public void onBeat(double time) {
            beats.add(time);
        }

        @Override
        public void onTempo(double time, double bpm, float confidence) {
            assertTrue(confidence >= 0 && confidence <= 1);
            tempos.add(bpm);
            tempoReports.add(new double[] {time, bpm, confidence});
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

    private static Track.Loaded synthetic(String name) throws IOException {
        return SyntheticTracks.all().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow().loader().load();
    }

    private static double meanConfidence(Recorded recorded, double from, double to) {
        return recorded.tempoReports.stream().filter(r -> r[0] >= from && r[0] < to)
                .mapToDouble(r -> r[2]).average().orElseThrow();
    }

    @Test
    void stopsAndForgetsTheTempoWhenTheMusicStops() throws IOException {
        Track.Loaded music = synthetic("four-on-floor-128");
        float sampleRate = music.audio().sampleRate();
        float[] samples = Arrays.copyOf(music.audio().samples(), music.audio().samples().length + (int) (10 * sampleRate));
        double end = music.audio().duration();

        Recorded recorded = track(samples, sampleRate);

        assertTrue(Double.isNaN(recorded.lastTempo()), "tempo after silence " + recorded.lastTempo());
        assertEquals(0, recorded.tempoReports.getLast()[2]);
        // The reverb tail and the 1.5 s silence window allow a few more beats, no more
        long late = recorded.beats.stream().filter(b -> b > end + 2).count();
        assertEquals(0, late, "beats more than 2 s after the music stopped");
    }

    @Test
    void confidenceFallsWhenTheBeatStopsAndRecoversWhenItReturns() throws IOException {
        // 20 s of four-on-the-floor, 12 s of a sustained pad (sound, but no onsets), the music again.
        // (The synthetic "breakdown" track keeps its claps and hats, so its beat never stops.)
        Track.Loaded music = synthetic("four-on-floor-128");
        float sampleRate = music.audio().sampleRate();
        int part = (int) (20 * sampleRate);
        int pad = (int) (12 * sampleRate);
        float[] samples = new float[2 * part + pad];
        System.arraycopy(music.audio().samples(), 0, samples, 0, part);
        for (int i = 0; i < pad; i++) {
            samples[part + i] = (float) (0.2 * Math.sin(2 * Math.PI * 220 * i / sampleRate));
        }
        System.arraycopy(music.audio().samples(), 0, samples, part + pad, part);

        Recorded recorded = track(samples, sampleRate);

        double before = meanConfidence(recorded, 10, 20);
        double latePad = meanConfidence(recorded, 28, 32);
        double after = meanConfidence(recorded, 44, 52);
        assertTrue(latePad < 0.5 * before, "pad " + latePad + " vs before " + before);
        assertTrue(after > 0.9 * before, "after " + after + " vs before " + before);
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
