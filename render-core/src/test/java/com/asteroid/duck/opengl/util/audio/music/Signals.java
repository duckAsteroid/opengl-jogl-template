package com.asteroid.duck.opengl.util.audio.music;

import com.asteroid.duck.opengl.util.audio.beat.eval.SyntheticTracks;
import com.asteroid.duck.opengl.util.audio.beat.eval.Track;

import java.io.IOException;
import java.util.SplittableRandom;

/** Test signals at {@link #RATE}. */
final class Signals {

    static final float RATE = SyntheticTracks.SAMPLE_RATE;

    private Signals() {}

    static float[] tone(double hz, double seconds, double gain) {
        float[] out = new float[(int) (seconds * RATE)];
        for (int i = 0; i < out.length; i++) out[i] = (float) (gain * Math.sin(2 * Math.PI * hz * i / RATE));
        return out;
    }

    static float[] noise(double seconds, double gain) {
        SplittableRandom random = new SplittableRandom(1);
        float[] out = new float[(int) (seconds * RATE)];
        for (int i = 0; i < out.length; i++) out[i] = (float) (gain * (random.nextDouble() * 2 - 1));
        return out;
    }

    static float[] silence(double seconds) {
        return new float[(int) (seconds * RATE)];
    }

    static float[] concat(float[]... parts) {
        int length = 0;
        for (float[] p : parts) length += p.length;
        float[] out = new float[length];
        int at = 0;
        for (float[] p : parts) {
            System.arraycopy(p, 0, out, at, p.length);
            at += p.length;
        }
        return out;
    }

    /**
     * Sustained chords, one per bar of 2 s, looped: each note a tone with a few harmonics, plus
     * the chord root an octave down.
     * @param chords  MIDI note numbers per chord
     * @param seconds total length
     */
    static float[] chords(int[][] chords, double seconds) {
        float[] out = new float[(int) (seconds * RATE)];
        int bar = (int) (2 * RATE);
        for (int i = 0; i < out.length; i++) {
            int[] chord = chords[(i / bar) % chords.length];
            double t = i / (double) RATE;
            double envelope = Math.min(1, (i % bar) / (0.02 * RATE));
            double sum = 0;
            for (int n = 0; n <= chord.length; n++) {
                double hz = n < chord.length ? midi(chord[n]) : midi(chord[0] - 12);
                for (int h = 1; h <= 3; h++) sum += Math.sin(2 * Math.PI * hz * h * t) / (h * h);
            }
            out[i] = (float) (0.15 * envelope * sum / (chord.length + 1));
        }
        return out;
    }

    static double midi(int note) {
        return 440 * Math.pow(2, (note - 69) / 12.0);
    }

    static int[][] transpose(int[][] chords, int semitones) {
        int[][] out = new int[chords.length][];
        for (int c = 0; c < chords.length; c++) {
            out[c] = chords[c].clone();
            for (int n = 0; n < out[c].length; n++) out[c][n] += semitones;
        }
        return out;
    }

    /** I–IV–V–I in C major. */
    static final int[][] C_MAJOR = {{60, 64, 67}, {53, 57, 60}, {55, 59, 62}, {60, 64, 67}};
    /** i–iv–V–i in A minor (E major chord, with the G#). */
    static final int[][] A_MINOR = {{57, 60, 64}, {50, 53, 57}, {52, 56, 59}, {57, 60, 64}};

    static Track.Loaded synthetic(String name) throws IOException {
        return SyntheticTracks.all().stream().filter(t -> t.name().equals(name)).findFirst().orElseThrow()
                .loader().load();
    }

    static MusicRun run(float[] samples) {
        return MusicRun.of(samples, RATE);
    }

    static MusicRun run(Track.Loaded track) {
        return MusicRun.of(track.audio().samples(), track.audio().sampleRate());
    }
}
