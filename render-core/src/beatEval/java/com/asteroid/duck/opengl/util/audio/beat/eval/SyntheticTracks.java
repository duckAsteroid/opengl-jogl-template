package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.IntPredicate;
import java.util.function.Supplier;

/**
 * Generated dance-style tracks with exact annotations, covering the cases that defeat simple
 * detectors on purpose: a kick under bass that isn't ducked for it, a bass line with its own
 * onsets in the kick's band (rolling and off-beat), mid-range onsets on every 8th, kicks off
 * the beat, swing and loose timing, a kick-less breakdown, and a tempo change.
 *
 * <p>Except for the click track, each mix goes through a master bus — reverb, background noise,
 * a fast bus compressor and a soft clipper — so kick transients are smeared and squashed the way
 * a mastered record's are, not sitting cleanly on silence.</p>
 *
 * <p>Beat annotations are the metrical grid. Kick annotations are the times the kicks were
 * actually played, so a humanised kick may sit a few milliseconds off its beat. Rendering is
 * deterministic (fixed seeds), so results are repeatable.</p>
 */
public final class SyntheticTracks {

    /** Group name every synthetic track reports under. */
    public static final String GROUP = "synthetic";
    /** Sample rate of the generated audio — the live capture rate. */
    public static final float SAMPLE_RATE = 48_000f;

    private static final int BEATS_PER_BAR = 4;
    /** Seconds of silence before the first beat, so it isn't at sample zero. */
    private static final double LEAD_IN = 0.5;
    /** Bass root per bar, cycling: A1, A1, G1, E1. */
    private static final double[] BASS_ROOTS = {55.0, 55.0, 49.0, 41.2};
    /** Lead arpeggio: A3, C4, E4, A4. */
    private static final double[] LEAD_NOTES = {220.0, 261.6, 329.6, 440.0};

    /** Master bus settings for a typical mastered dance track. */
    private static final Master MASTERED = new Master(0.18f, 0.01f, 0.25f, 4f);
    /** Heavier compression and noise: the kick has to fight the rest of the mix. */
    private static final Master SQUASHED = new Master(0.25f, 0.02f, 0.15f, 8f);

    private SyntheticTracks() {}

    /** @return every synthetic track */
    public static List<Track> all() {
        return List.of(
                track("clicks-120", SyntheticTracks::clicks120),
                track("four-on-floor-128", SyntheticTracks::fourOnFloor128),
                track("buried-kick-140", SyntheticTracks::buriedKick140),
                track("rolling-bass-124", SyntheticTracks::rollingBass124),
                track("offbeat-bass-122", SyntheticTracks::offBeatBass122),
                track("breakbeat-130", SyntheticTracks::breakbeat130),
                track("lofi-hiphop-90", SyntheticTracks::lofiHipHop90),
                track("breakdown-126", SyntheticTracks::breakdown126),
                track("tempo-change-120-128", SyntheticTracks::tempoChange120To128));
    }

    private static Track track(String name, Supplier<Track.Loaded> render) {
        return new Track(GROUP, name, render::get);
    }

    /** Sanity check: a dry 1 kHz click on every beat, nothing else. No kick annotations. */
    private static Track.Loaded clicks120() {
        double[] beats = grid(LEAD_IN, 120, 64);
        Mix mix = new Mix(beats, 1, 0);
        for (double b : beats) mix.click(b, 0.8f);
        return mix.finish(new double[0], 120, null);
    }

    /** Full arrangement: kick every beat, hats, claps, sidechained sub, lead 8ths, pad. */
    private static Track.Loaded fourOnFloor128() {
        double[] beats = grid(LEAD_IN, 128, 16 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 2, 0.002);
        double[] kicks = drums(mix, beats, i -> true, 0.8f);
        mix.sub(kicks, 0, beats.length, 0.4f, true);
        mix.lead(0, beats.length, 0.12f);
        mix.pad(beats[0], mix.end(), 0.1f);
        return mix.finish(kicks, 128, MASTERED);
    }

    /** Quiet kick under a loud sub that is not ducked for it, plus mid-bass stabs, heavy compression. */
    private static Track.Loaded buriedKick140() {
        double[] beats = grid(LEAD_IN, 140, 18 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 3, 0.002);
        double[] kicks = drums(mix, beats, i -> true, 0.35f);
        mix.sub(kicks, 0, beats.length, 0.6f, false);
        double sixteenth = mix.sixteenth(0);
        for (int i = 0; i < beats.length; i++) {
            if (i % 2 == 1) mix.pluck(beats[i] + 3 * sixteenth, 2 * root(i), 0.3f);
        }
        mix.lead(0, beats.length, 0.15f);
        return mix.finish(kicks, 140, SQUASHED);
    }

    /** Kick every beat with a "rolling" bass: plucks on the three 16ths after each kick. */
    private static Track.Loaded rollingBass124() {
        double[] beats = grid(LEAD_IN, 124, 16 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 4, 0.002);
        double[] kicks = drums(mix, beats, i -> true, 0.7f);
        double sixteenth = mix.sixteenth(0);
        for (int i = 0; i < beats.length; i++) {
            for (int s = 1; s < 4; s++) mix.pluck(beats[i] + s * sixteenth, root(i), 0.55f);
        }
        mix.pad(beats[0], mix.end(), 0.08f);
        return mix.finish(kicks, 124, MASTERED);
    }

    /** Classic house off-beat bassline: a loud bass note on every "and", open hats with it. */
    private static Track.Loaded offBeatBass122() {
        double[] beats = grid(LEAD_IN, 122, 16 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 5, 0.002);
        double[] kicks = drums(mix, beats, i -> true, 0.7f);
        double eighth = 2 * mix.sixteenth(0);
        for (int i = 0; i < beats.length; i++) {
            mix.pluck(beats[i] + eighth, root(i), 0.7f);
            mix.openHat(beats[i] + eighth, 0.15f);
        }
        mix.lead(0, beats.length, 0.1f);
        return mix.finish(kicks, 122, MASTERED);
    }

    /** Syncopated kicks (beat 1, the "and" of 2, the "and" of 3), so kicks ≠ beats; loose timing. */
    private static Track.Loaded breakbeat130() {
        double[] beats = grid(LEAD_IN, 130, 16 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 6, 0.005);
        double sixteenth = mix.sixteenth(0);
        List<Double> kicks = new ArrayList<>();
        for (int bar = 0; bar < beats.length / BEATS_PER_BAR; bar++) {
            double start = beats[bar * BEATS_PER_BAR];
            for (int s : new int[] {0, 6, 10}) kicks.add(mix.kick(start + s * sixteenth, 0.8f));
            mix.clap(start + 4 * sixteenth, 0.35f);
            mix.clap(start + 12 * sixteenth, 0.35f);
            for (int s = 0; s < 16; s += 2) mix.hat(start + s * sixteenth, 0.12f);
        }
        double[] kickTimes = toArray(kicks);
        mix.sub(kickTimes, 0, beats.length, 0.35f, true);
        mix.lead(0, beats.length, 0.1f);
        return mix.finish(kickTimes, 130, MASTERED);
    }

    /** 90 BPM, swung 16ths, loose kicks on 1, the swung "a" of 2 and 3, snare on 2 and 4, vinyl noise. */
    private static Track.Loaded lofiHipHop90() {
        double[] beats = grid(LEAD_IN, 90, 12 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 7, 0.012);
        double sixteenth = mix.sixteenth(0);
        double swing = 0.33 * sixteenth;
        List<Double> kicks = new ArrayList<>();
        for (int bar = 0; bar < beats.length / BEATS_PER_BAR; bar++) {
            double start = beats[bar * BEATS_PER_BAR];
            for (int s : new int[] {0, 7, 8}) {
                double t = start + s * sixteenth + (s % 2 == 1 ? swing : 0);
                kicks.add(mix.kick(t, 0.75f));
            }
            mix.clap(start + 4 * sixteenth, 0.45f);
            mix.clap(start + 12 * sixteenth, 0.45f);
            for (int s = 0; s < 16; s++) {
                mix.hat(start + s * sixteenth + (s % 2 == 1 ? swing : 0), s % 2 == 0 ? 0.1f : 0.05f);
            }
            for (int b = 0; b < BEATS_PER_BAR; b++) mix.pluck(beats[bar * BEATS_PER_BAR + b], root(bar * BEATS_PER_BAR), 0.4f);
        }
        mix.pad(beats[0], mix.end(), 0.15f);
        mix.crackle(0.02f);
        double[] kickTimes = toArray(kicks);
        return mix.finish(kickTimes, 90, SQUASHED);
    }

    /** 8 bars full, 8 bars breakdown (pad, hats and a riser, no kick or bass), 8 bars full. */
    private static Track.Loaded breakdown126() {
        double[] beats = grid(LEAD_IN, 126, 24 * BEATS_PER_BAR);
        Mix mix = new Mix(beats, 8, 0.002);
        int breakStart = 8 * BEATS_PER_BAR;
        int breakEnd = 16 * BEATS_PER_BAR;
        IntPredicate kickOn = i -> i < breakStart || i >= breakEnd;
        double[] kicks = drums(mix, beats, kickOn, 0.8f);
        mix.sub(kicks, 0, breakStart, 0.4f, true);
        mix.sub(kicks, breakEnd, beats.length, 0.4f, true);
        mix.lead(0, beats.length, 0.1f);
        mix.pad(beats[breakStart], beats[breakEnd], 0.25f);
        mix.riser(beats[breakEnd - 4 * BEATS_PER_BAR], beats[breakEnd], 0.3f);
        return mix.finish(kicks, 126, MASTERED);
    }

    /** 12 bars at 120 BPM, then an instant switch to 128 BPM for 12 bars. No global tempo. */
    private static Track.Loaded tempoChange120To128() {
        double[] first = grid(LEAD_IN, 120, 12 * BEATS_PER_BAR);
        double[] second = grid(first[first.length - 1] + 60.0 / 120, 128, 12 * BEATS_PER_BAR);
        double[] beats = new double[first.length + second.length];
        System.arraycopy(first, 0, beats, 0, first.length);
        System.arraycopy(second, 0, beats, first.length, second.length);
        Mix mix = new Mix(beats, 9, 0.002);
        double[] kicks = drums(mix, beats, i -> true, 0.8f);
        mix.sub(kicks, 0, beats.length, 0.4f, true);
        mix.lead(0, beats.length, 0.1f);
        return mix.finish(kicks, Double.NaN, MASTERED);
    }

    /**
     * Kicks on the beats {@code kickOn} accepts, off-beat hats, claps on 2 and 4.
     * @return the times the kicks were played
     */
    private static double[] drums(Mix mix, double[] beats, IntPredicate kickOn, float kickGain) {
        List<Double> kicks = new ArrayList<>();
        for (int i = 0; i < beats.length; i++) {
            if (kickOn.test(i)) kicks.add(mix.kick(beats[i], kickGain));
            mix.hat(beats[i] + 2 * mix.sixteenth(i), 0.12f);
            if (i % 2 == 1) mix.clap(beats[i], 0.3f);
        }
        return toArray(kicks);
    }

    private static double root(int beat) {
        return BASS_ROOTS[(beat / BEATS_PER_BAR) % BASS_ROOTS.length];
    }

    private static double[] toArray(List<Double> times) {
        return times.stream().mapToDouble(Double::doubleValue).sorted().toArray();
    }

    /** {@code count} beat times at {@code bpm} starting at {@code start} seconds. */
    static double[] grid(double start, double bpm, int count) {
        double[] beats = new double[count];
        for (int i = 0; i < count; i++) beats[i] = start + i * 60.0 / bpm;
        return beats;
    }

    /**
     * Master bus settings.
     * @param reverb      reverb wet level
     * @param noise       background (pink-ish) noise level
     * @param threshold   compressor threshold (linear)
     * @param ratio       compressor ratio
     */
    private record Master(float reverb, float noise, float threshold, float ratio) {}

    /** A mono mix buffer over a beat grid, with simple drum, bass and synth voices. */
    private static final class Mix {
        private final double[] beats;
        private final float[] out;
        private final SplittableRandom random;
        private final double jitter;

        /**
         * @param beats  the beat grid; the mix runs one second past the last beat
         * @param seed   random seed
         * @param jitter standard deviation, in seconds, of the timing error added to each drum hit
         */
        Mix(double[] beats, long seed, double jitter) {
            this.beats = beats;
            this.out = new float[(int) Math.ceil((beats[beats.length - 1] + 1) * SAMPLE_RATE)];
            this.random = new SplittableRandom(seed);
            this.jitter = jitter;
        }

        double end() {
            return out.length / SAMPLE_RATE;
        }

        /** Length of a 16th note at beat {@code i}. */
        double sixteenth(int i) {
            int j = Math.min(i, beats.length - 2);
            return (beats[j + 1] - beats[j]) / 4;
        }

        private int index(double t) {
            return (int) Math.round(t * SAMPLE_RATE);
        }

        private double noise() {
            return random.nextDouble() * 2 - 1;
        }

        private double humanise(double t) {
            if (jitter == 0) return t;
            // Sum of uniforms ≈ Gaussian, clamped to ±2.5σ
            double g = (random.nextDouble() + random.nextDouble() + random.nextDouble() - 1.5) * 2;
            return Math.max(0, t + Math.clamp(g, -2.5, 2.5) * jitter);
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

        /**
         * Sine kick: pitch sweeps ~155 → 45 Hz, 150 ms decay, 3 ms noise click on the attack.
         * @return the time it was actually played (after humanising)
         */
        double kick(double when, float gain) {
            double t0 = humanise(when);
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
            return start / (double) SAMPLE_RATE;
        }

        /** High-passed noise with the given decay. */
        private void hiss(double when, float gain, double decay) {
            int start = index(humanise(when));
            int length = index(4 * decay);
            double previous = 0;
            for (int i = 0; i < length && start + i < out.length; i++) {
                double n = noise();
                out[start + i] += (float) (gain * (n - previous) * Math.exp(-i / SAMPLE_RATE / decay));
                previous = n;
            }
        }

        /** Closed hat: 20 ms decay. */
        void hat(double when, float gain) {
            hiss(when, gain, 0.02);
        }

        /** Open hat: 120 ms decay. */
        void openHat(double when, float gain) {
            hiss(when, gain, 0.12);
        }

        /** Roughly band-passed noise, 60 ms decay. */
        void clap(double when, float gain) {
            int start = index(humanise(when));
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
         * bar. If {@code ducked}, each kick ducks it, recovering over ~100 ms (sidechain
         * compression); otherwise it sits under the kick at full level.
         */
        void sub(double[] kicks, int from, int to, float gain, boolean ducked) {
            double endTime = to < beats.length ? beats[to] : end();
            int k = 0;
            for (int i = index(beats[from]); i < index(endTime) && i < out.length; i++) {
                double t = i / SAMPLE_RATE;
                int found = Arrays.binarySearch(beats, t);
                int beat = Math.max(0, found >= 0 ? found : -found - 2);
                double duck = 1;
                if (ducked) {
                    while (k + 1 < kicks.length && kicks[k + 1] <= t) k++;
                    double sinceKick = kicks.length > 0 && kicks[k] <= t ? t - kicks[k] : Double.POSITIVE_INFINITY;
                    duck = 1 - 0.85 * Math.exp(-sinceKick / 0.1);
                }
                out[i] += (float) (gain * duck * Math.sin(2 * Math.PI * root(beat) * t));
            }
        }

        /** Bright saw-ish arpeggio on every 8th note from beat {@code from} to beat {@code to}. */
        void lead(int from, int to, float gain) {
            int note = 0;
            for (int i = from; i < to; i++) {
                for (int half = 0; half < 2; half++) {
                    double t0 = beats[i] + half * 2 * sixteenth(i);
                    double frequency = LEAD_NOTES[note++ % LEAD_NOTES.length];
                    int start = index(t0);
                    int length = index(0.25);
                    for (int s = 0; s < length && start + s < out.length; s++) {
                        double t = s / SAMPLE_RATE;
                        double saw = 0;
                        for (int h = 1; h <= 6; h++) saw += Math.sin(2 * Math.PI * h * frequency * t) / h;
                        out[start + s] += (float) (gain * saw * Math.exp(-t / 0.12) * Math.min(1, t / 0.003));
                    }
                }
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

        /** Noise swelling in level and brightness from {@code from} to {@code to}. */
        void riser(double from, double to, float gain) {
            int start = index(from);
            int length = index(to) - start;
            double low = 0;
            for (int i = 0; i < length && start + i < out.length; i++) {
                double progress = i / (double) length;
                low += (0.02 + 0.5 * progress) * (noise() - low);
                out[start + i] += (float) (gain * progress * progress * low);
            }
        }

        /** Sparse vinyl crackle over the whole track. */
        void crackle(float gain) {
            for (int i = 0; i < out.length; i++) {
                if (random.nextDouble() < 0.0004) out[i] += (float) (gain * noise() * 8);
            }
        }

        /**
         * Apply the master bus (unless {@code master} is null), normalise to a 0.9 peak and attach
         * the annotations.
         */
        Track.Loaded finish(double[] kicks, double bpm, Master master) {
            if (master != null) {
                reverb(master.reverb());
                addNoise(master.noise());
                compress(master.threshold(), master.ratio());
            }
            float peak = 0;
            for (float v : out) peak = Math.max(peak, Math.abs(v));
            if (peak > 0) {
                float scale = 0.9f / peak;
                for (int i = 0; i < out.length; i++) out[i] *= scale;
            }
            return new Track.Loaded(new AudioClip(out, SAMPLE_RATE), new Annotations(beats, kicks, bpm));
        }

        /** Schroeder reverb: four parallel feedback combs into two series all-passes, mixed in. */
        private void reverb(float wet) {
            int[] combs = {index(0.0297), index(0.0371), index(0.0411), index(0.0437)};
            float[] tail = new float[out.length];
            for (int delay : combs) {
                float[] line = new float[out.length];
                for (int i = 0; i < out.length; i++) {
                    line[i] = out[i] + (i >= delay ? 0.78f * line[i - delay] : 0);
                    tail[i] += line[i] / combs.length;
                }
            }
            for (int delay : new int[] {index(0.005), index(0.0017)}) {
                float[] in = tail.clone();
                for (int i = 0; i < out.length; i++) {
                    float delayedIn = i >= delay ? in[i - delay] : 0;
                    float delayedOut = i >= delay ? tail[i - delay] : 0;
                    tail[i] = -0.7f * in[i] + delayedIn + 0.7f * delayedOut;
                }
            }
            for (int i = 0; i < out.length; i++) out[i] += wet * tail[i];
        }

        /** Pink-ish background noise (one-pole low-passed white). */
        private void addNoise(float level) {
            double low = 0;
            for (int i = 0; i < out.length; i++) {
                low += 0.05 * (noise() - low);
                out[i] += (float) (level * 4 * low);
            }
        }

        /**
         * Feed-forward peak compressor (5 ms attack, 100 ms release) with make-up gain, then a
         * tanh soft clipper. The fast attack is what flattens kick transients on a master bus.
         */
        private void compress(float threshold, float ratio) {
            float peak = 0;
            for (float v : out) peak = Math.max(peak, Math.abs(v));
            if (peak == 0) return;
            for (int i = 0; i < out.length; i++) out[i] /= peak;
            double attack = Math.exp(-1 / (0.005 * SAMPLE_RATE));
            double release = Math.exp(-1 / (0.1 * SAMPLE_RATE));
            double envelope = 0;
            double makeUp = 1 / (threshold + (1 - threshold) / ratio);
            for (int i = 0; i < out.length; i++) {
                double level = Math.abs(out[i]);
                envelope = level > envelope ? attack * envelope + (1 - attack) * level
                        : release * envelope + (1 - release) * level;
                double gain = envelope > threshold ? (threshold + (envelope - threshold) / ratio) / envelope : 1;
                out[i] = (float) Math.tanh(1.5 * out[i] * gain * makeUp);
            }
        }
    }
}
