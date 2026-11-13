package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.IntPredicate;

/**
 * Generated dance-style tracks with exact annotations, covering the cases that defeat simple
 * detectors on purpose: a kick under a sidechained sub-bass, a kick buried by the bass, a bass
 * line with its own onsets in the kick's band, kicks off the beat, a kick-less breakdown, and a
 * tempo change. Rendering is deterministic (fixed seeds), so results are repeatable.
 */
public final class SyntheticTracks {

    /** Group name every synthetic track reports under. */
    public static final String GROUP = "synthetic";
    /** Sample rate of the generated audio — the live capture rate. */
    public static final float SAMPLE_RATE = 48_000f;

    private static final int BEATS_PER_BAR = 4;
    /** Seconds of silence before the first beat, so it isn't at sample zero. */
    private static final double LEAD_IN = 0.5;
    /** Sub-bass root per bar, cycling: A1, A1, G1, E1. */
    private static final double[] BASS_ROOTS = {55.0, 55.0, 49.0, 41.2};

    private SyntheticTracks() {}

    /** @return every synthetic track */
    public static List<Track> all() {
        return List.of(
                track("clicks-120", SyntheticTracks::clicks120),
                track("four-on-floor-128", () -> fourOnFloor(128, 16, 0.8f, 0.4f)),
                track("buried-kick-140", () -> fourOnFloor(140, 18, 0.35f, 0.6f)),
                track("rolling-bass-124", SyntheticTracks::rollingBass124),
                track("breakbeat-130", SyntheticTracks::breakbeat130),
                track("breakdown-126", SyntheticTracks::breakdown126),
                track("tempo-change-120-128", SyntheticTracks::tempoChange120To128));
    }

    private static Track track(String name, java.util.function.Supplier<Track.Loaded> render) {
        return new Track(GROUP, name, render::get);
    }

    /** Sanity check: a 1 kHz click on every beat, nothing else. No kick annotations. */
    private static Track.Loaded clicks120() {
        double[] beats = grid(LEAD_IN, 120, 64);
        Mix mix = new Mix(beats[beats.length - 1] + 1, 1);
        for (double b : beats) mix.click(b, 0.8f);
        return mix.finish(beats, new double[0], 120);
    }

    /** Kick every beat, off-beat hats, claps on 2 and 4, sidechained sub-bass. */
    private static Track.Loaded fourOnFloor(double bpm, int bars, float kickGain, float subGain) {
        double[] beats = grid(LEAD_IN, bpm, bars * BEATS_PER_BAR);
        Mix mix = new Mix(beats[beats.length - 1] + 1, 2);
        drums(mix, beats, i -> true, kickGain);
        mix.sidechainedSub(beats, beats, 0, beats.length, subGain);
        return mix.finish(beats, beats, bpm);
    }

    /** Kick every beat with a "rolling" bass: plucks on the three 16ths after each kick. */
    private static Track.Loaded rollingBass124() {
        double bpm = 124;
        double[] beats = grid(LEAD_IN, bpm, 16 * BEATS_PER_BAR);
        Mix mix = new Mix(beats[beats.length - 1] + 1, 3);
        drums(mix, beats, i -> true, 0.7f);
        double sixteenth = 60 / bpm / 4;
        for (int i = 0; i < beats.length; i++) {
            double root = BASS_ROOTS[(i / BEATS_PER_BAR) % BASS_ROOTS.length];
            for (int s = 1; s < 4; s++) mix.pluck(beats[i] + s * sixteenth, root, 0.55f);
        }
        return mix.finish(beats, beats, bpm);
    }

    /** Syncopated kicks (beat 1, the "and" of 2, the "and" of 3), so kicks ≠ beats. */
    private static Track.Loaded breakbeat130() {
        double bpm = 130;
        double[] beats = grid(LEAD_IN, bpm, 16 * BEATS_PER_BAR);
        Mix mix = new Mix(beats[beats.length - 1] + 1, 4);
        double sixteenth = 60 / bpm / 4;
        List<Double> kicks = new ArrayList<>();
        for (int bar = 0; bar < beats.length / BEATS_PER_BAR; bar++) {
            double start = beats[bar * BEATS_PER_BAR];
            for (int s : new int[] {0, 6, 10}) {
                double t = start + s * sixteenth;
                mix.kick(t, 0.8f);
                kicks.add(t);
            }
            mix.clap(start + 4 * sixteenth, 0.35f);
            mix.clap(start + 12 * sixteenth, 0.35f);
            for (int s = 0; s < 16; s += 2) mix.hat(start + s * sixteenth, 0.12f);
        }
        double[] kickTimes = kicks.stream().mapToDouble(Double::doubleValue).toArray();
        mix.sidechainedSub(beats, kickTimes, 0, beats.length, 0.35f);
        return mix.finish(beats, kickTimes, bpm);
    }

    /** 8 bars full, 8 bars breakdown (pad and hats, no kick or bass), 8 bars full. */
    private static Track.Loaded breakdown126() {
        double bpm = 126;
        double[] beats = grid(LEAD_IN, bpm, 24 * BEATS_PER_BAR);
        Mix mix = new Mix(beats[beats.length - 1] + 1, 5);
        int breakStart = 8 * BEATS_PER_BAR;
        int breakEnd = 16 * BEATS_PER_BAR;
        IntPredicate kickOn = i -> i < breakStart || i >= breakEnd;
        drums(mix, beats, kickOn, 0.8f);
        mix.sidechainedSub(beats, beats, 0, breakStart, 0.4f);
        mix.sidechainedSub(beats, beats, breakEnd, beats.length, 0.4f);
        mix.pad(beats[breakStart], beats[breakEnd], 0.25f);
        double[] kicks = java.util.stream.IntStream.range(0, beats.length).filter(kickOn).mapToDouble(i -> beats[i]).toArray();
        return mix.finish(beats, kicks, 126);
    }

    /** 12 bars at 120 BPM, then an instant switch to 128 BPM for 12 bars. No global tempo. */
    private static Track.Loaded tempoChange120To128() {
        double[] first = grid(LEAD_IN, 120, 12 * BEATS_PER_BAR);
        double[] second = grid(first[first.length - 1] + 60.0 / 120, 128, 12 * BEATS_PER_BAR);
        double[] beats = new double[first.length + second.length];
        System.arraycopy(first, 0, beats, 0, first.length);
        System.arraycopy(second, 0, beats, first.length, second.length);
        Mix mix = new Mix(beats[beats.length - 1] + 1, 6);
        drums(mix, beats, i -> true, 0.8f);
        mix.sidechainedSub(beats, beats, 0, beats.length, 0.4f);
        return mix.finish(beats, beats, Double.NaN);
    }

    /** Kicks on the beats {@code kickOn} accepts, off-beat hats, claps on 2 and 4. */
    private static void drums(Mix mix, double[] beats, IntPredicate kickOn, float kickGain) {
        for (int i = 0; i < beats.length; i++) {
            if (kickOn.test(i)) mix.kick(beats[i], kickGain);
            double next = i + 1 < beats.length ? beats[i + 1] : beats[i] + (beats[i] - beats[i - 1]);
            mix.hat((beats[i] + next) / 2, 0.12f);
            if (i % 2 == 1) mix.clap(beats[i], 0.3f);
        }
    }

    /** {@code count} beat times at {@code bpm} starting at {@code start} seconds. */
    static double[] grid(double start, double bpm, int count) {
        double[] beats = new double[count];
        for (int i = 0; i < count; i++) beats[i] = start + i * 60.0 / bpm;
        return beats;
    }

    /** A mono mix buffer with simple drum and bass voices. */
    private static final class Mix {
        private final float[] out;
        private final SplittableRandom random;

        Mix(double seconds, long seed) {
            this.out = new float[(int) Math.ceil(seconds * SAMPLE_RATE)];
            this.random = new SplittableRandom(seed);
        }

        private int index(double t) {
            return (int) Math.round(t * SAMPLE_RATE);
        }

        private double noise() {
            return random.nextDouble() * 2 - 1;
        }

        /** 10 ms 1 kHz tone burst. */
        void click(double t0, float gain) {
            int start = index(t0);
            int length = index(0.01);
            for (int i = 0; i < length && start + i < out.length; i++) {
                double t = i / SAMPLE_RATE;
                out[start + i] += (float) (gain * Math.sin(2 * Math.PI * 1000 * t) * (1 - t / 0.01));
            }
        }

        /** Sine kick: pitch sweeps ~155 → 45 Hz, 150 ms decay, 3 ms noise click on the attack. */
        void kick(double t0, float gain) {
            int start = index(t0);
            int length = index(0.35);
            double phase = 0;
            for (int i = 0; i < length && start + i < out.length; i++) {
                double t = i / SAMPLE_RATE;
                double frequency = 45 + 110 * Math.exp(-t / 0.025);
                phase += 2 * Math.PI * frequency / SAMPLE_RATE;
                double body = Math.sin(phase) * Math.exp(-t / 0.15) * Math.min(1, t / 0.001);
                double click = t < 0.003 ? noise() * 0.4 * (1 - t / 0.003) : 0;
                out[start + i] += (float) (gain * (body + click));
            }
        }

        /** High-passed noise, 20 ms decay. */
        void hat(double t0, float gain) {
            int start = index(t0);
            int length = index(0.08);
            double previous = 0;
            for (int i = 0; i < length && start + i < out.length; i++) {
                double n = noise();
                out[start + i] += (float) (gain * (n - previous) * Math.exp(-i / SAMPLE_RATE / 0.02));
                previous = n;
            }
        }

        /** Roughly band-passed noise, 60 ms decay. */
        void clap(double t0, float gain) {
            int start = index(t0);
            int length = index(0.25);
            double previous = 0;
            double low = 0;
            for (int i = 0; i < length && start + i < out.length; i++) {
                double n = noise();
                low += 0.3 * ((n - previous) - low);
                previous = n;
                out[start + i] += (float) (gain * 2 * low * Math.exp(-i / SAMPLE_RATE / 0.06));
            }
        }

        /** Bass pluck (fundamental + 2nd harmonic), 80 ms decay. */
        void pluck(double t0, double frequency, float gain) {
            int start = index(t0);
            int length = index(0.2);
            for (int i = 0; i < length && start + i < out.length; i++) {
                double t = i / SAMPLE_RATE;
                double tone = Math.sin(2 * Math.PI * frequency * t) + 0.5 * Math.sin(4 * Math.PI * frequency * t);
                out[start + i] += (float) (gain * tone * Math.exp(-t / 0.08) * Math.min(1, t / 0.002));
            }
        }

        /**
         * Sustained sine sub-bass from beat {@code from} to beat {@code to}, root changing each
         * bar, ducked by each kick and recovering over ~100 ms (sidechain compression).
         */
        void sidechainedSub(double[] beats, double[] kicks, int from, int to, float gain) {
            double end = to < beats.length ? beats[to] : out.length / SAMPLE_RATE;
            int k = 0;
            for (int i = index(beats[from]); i < index(end) && i < out.length; i++) {
                double t = i / SAMPLE_RATE;
                int found = Arrays.binarySearch(beats, t);
                int beat = Math.max(0, found >= 0 ? found : -found - 2);
                double root = BASS_ROOTS[(beat / BEATS_PER_BAR) % BASS_ROOTS.length];
                while (k + 1 < kicks.length && kicks[k + 1] <= t) k++;
                double sinceKick = kicks.length > 0 && kicks[k] <= t ? t - kicks[k] : Double.POSITIVE_INFINITY;
                double duck = 1 - 0.85 * Math.exp(-sinceKick / 0.1);
                out[i] += (float) (gain * duck * Math.sin(2 * Math.PI * root * t));
            }
        }

        /** Slowly swelling A-minor pad (A3, C4, E4). */
        void pad(double from, double to, float gain) {
            for (int i = index(from); i < index(to) && i < out.length; i++) {
                double t = i / SAMPLE_RATE;
                double swell = 0.6 + 0.4 * Math.sin(2 * Math.PI * 0.25 * t);
                double chord = Math.sin(2 * Math.PI * 220 * t) + Math.sin(2 * Math.PI * 261.6 * t)
                        + Math.sin(2 * Math.PI * 329.6 * t);
                out[i] += (float) (gain * swell * chord / 3);
            }
        }

        /** Normalise to a 0.9 peak and attach the annotations. */
        Track.Loaded finish(double[] beats, double[] kicks, double bpm) {
            float peak = 0;
            for (float v : out) peak = Math.max(peak, Math.abs(v));
            if (peak > 0) {
                float scale = 0.9f / peak;
                for (int i = 0; i < out.length; i++) out[i] *= scale;
            }
            return new Track.Loaded(new AudioClip(out, SAMPLE_RATE), new Annotations(beats, kicks, bpm));
        }
    }
}
