package com.asteroid.duck.opengl.util.audio.music;

/**
 * Key from chroma: the Krumhansl–Kessler method. Chroma frames are averaged with a
 * {@value #AVERAGE_SECONDS} s exponential average and correlated against the 24 rotated
 * major/minor key profiles; the best fit is the key. A different key only replaces the current
 * one after leading it by {@value #SWITCH_MARGIN} for {@value #SWITCH_SECONDS} s, so it doesn't
 * flicker between close keys.
 */
final class KeyEstimator {

    static final double AVERAGE_SECONDS = 15;
    static final double SWITCH_SECONDS = 4;
    static final double SWITCH_MARGIN = 0.03;
    /** Seconds of sound needed before the first estimate. */
    static final double MIN_SECONDS = 3;

    /** Krumhansl–Kessler probe-tone profiles, tonic first. */
    private static final double[] MAJOR = {6.35, 2.23, 3.48, 2.33, 4.38, 4.09, 2.52, 5.19, 2.39, 3.66, 2.29, 2.88};
    private static final double[] MINOR = {6.33, 2.68, 3.52, 5.38, 2.60, 3.53, 2.54, 4.75, 3.98, 2.69, 3.34, 3.17};

    private final double alpha;
    private final double frameSeconds;
    private final double[] average = new double[12];
    private final double[] scores = new double[24];
    private double heard;
    private int current = -1;
    private int candidate = -1;
    private double candidateSeconds;
    private float confidence;

    KeyEstimator(double frameSeconds) {
        this.frameSeconds = frameSeconds;
        this.alpha = 1 - Math.exp(-frameSeconds / AVERAGE_SECONDS);
    }

    /** Add one chroma frame (12 values, C first). */
    void add(double[] chroma) {
        heard += frameSeconds;
        // Average from the first frame on, rather than from zero, so early estimates aren't biased
        double a = Math.max(alpha, frameSeconds / heard);
        for (int i = 0; i < 12; i++) average[i] += a * (chroma[i] - average[i]);
        if (heard < MIN_SECONDS) return;

        score(average, scores);
        int best = 0;
        int second = -1;
        for (int k = 1; k < 24; k++) {
            if (scores[k] > scores[best]) {
                second = best;
                best = k;
            } else if (second < 0 || scores[k] > scores[second]) {
                second = k;
            }
        }
        if (current < 0) {
            current = best;
        } else if (best != current && scores[best] - scores[current] > SWITCH_MARGIN) {
            if (best != candidate) {
                candidate = best;
                candidateSeconds = 0;
            }
            candidateSeconds += frameSeconds;
            if (candidateSeconds >= SWITCH_SECONDS) current = best;
        } else {
            candidate = -1;
        }
        double runnerUp = current == best ? scores[second] : scores[best];
        double lead = scores[current] - runnerUp;
        confidence = (float) (Math.clamp(scores[current], 0, 1) * (0.5 + 0.5 * Math.clamp(lead / 0.1, 0, 1)));
    }

    /** @return the current key, or {@code null} before the first estimate */
    Key key() {
        return current < 0 ? null : new Key(current % 12, current < 12);
    }

    float confidence() {
        return current < 0 ? 0 : confidence;
    }

    void reset() {
        java.util.Arrays.fill(average, 0);
        heard = 0;
        current = -1;
        candidate = -1;
        confidence = 0;
    }

    /**
     * Correlate a chroma vector with every key profile.
     * @param chroma 12 values, C first
     * @param out    24 Pearson correlations: 0–11 major keys C…B, 12–23 minor keys C…B
     */
    static void score(double[] chroma, double[] out) {
        for (int tonic = 0; tonic < 12; tonic++) {
            out[tonic] = correlate(chroma, MAJOR, tonic);
            out[12 + tonic] = correlate(chroma, MINOR, tonic);
        }
    }

    /** @return the key index (as {@link #score}) that best fits {@code chroma} */
    static int best(double[] chroma) {
        double[] out = new double[24];
        score(chroma, out);
        int best = 0;
        for (int k = 1; k < 24; k++) if (out[k] > out[best]) best = k;
        return best;
    }

    private static double correlate(double[] chroma, double[] profile, int tonic) {
        double meanC = 0;
        double meanP = 0;
        for (int i = 0; i < 12; i++) {
            meanC += chroma[i];
            meanP += profile[i];
        }
        meanC /= 12;
        meanP /= 12;
        double cov = 0;
        double varC = 0;
        double varP = 0;
        for (int i = 0; i < 12; i++) {
            double c = chroma[(i + tonic) % 12] - meanC;
            double p = profile[i] - meanP;
            cov += c * p;
            varC += c * c;
            varP += p * p;
        }
        return varC == 0 ? 0 : cov / Math.sqrt(varC * varP);
    }
}
