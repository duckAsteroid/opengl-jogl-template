package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.util.Arrays;
import java.util.List;

/**
 * Standard beat- and onset-tracking metrics, following the definitions used by
 * {@code mir_eval.beat} (F-measure, continuity) closely enough to compare against published
 * numbers. All event arrays are times in seconds, ascending.
 */
public final class BeatMetrics {

    /** Seconds at the start of a track that are excluded before scoring, as {@code mir_eval} does. */
    public static final double WARM_UP = 5.0;
    /** Beat F-measure tolerance window, seconds either side. */
    public static final double BEAT_WINDOW = 0.07;
    /** Onset (kick) F-measure tolerance window, seconds either side. */
    public static final double ONSET_WINDOW = 0.05;
    /** Continuity phase and period tolerance, as a fraction of the reference beat interval. */
    public static final double CONTINUITY_TOLERANCE = 0.175;
    /** Consecutive correct beats that count as "locked". */
    public static final int LOCK_RUN = 4;

    /**
     * Continuity scores, each a fraction of reference beats in {@code [0, 1]}.
     * @param cmlc longest continuous correct run, correct metrical level
     * @param cmlt total correct, correct metrical level
     * @param amlc longest continuous correct run, any allowed metrical level
     * @param amlt total correct, any allowed metrical level (double/half tempo, off-beat)
     */
    public record Continuity(double cmlc, double cmlt, double amlc, double amlt) {}

    private BeatMetrics() {}

    /**
     * @param times event times
     * @param from  cut-off in seconds
     * @return the times at or after {@code from}
     */
    public static double[] trim(double[] times, double from) {
        return Arrays.stream(times).filter(t -> t >= from).toArray();
    }

    /**
     * Greedy one-to-one matching of estimated events to reference events within a window.
     * @param reference reference times
     * @param estimated estimated times
     * @param window    tolerance in seconds either side
     * @return per estimated event, whether it matched a reference event
     */
    public static boolean[] matchedEstimates(double[] reference, double[] estimated, double window) {
        boolean[] matched = new boolean[estimated.length];
        int r = 0;
        int e = 0;
        while (r < reference.length && e < estimated.length) {
            double diff = estimated[e] - reference[r];
            if (Math.abs(diff) <= window) {
                matched[e++] = true;
                r++;
            } else if (diff < 0) {
                e++;
            } else {
                r++;
            }
        }
        return matched;
    }

    /**
     * F-measure of estimated against reference events.
     * @param reference reference times
     * @param estimated estimated times
     * @param window    tolerance in seconds either side
     * @return F in {@code [0, 1]}, or {@code NaN} if there are no reference events
     */
    public static double fMeasure(double[] reference, double[] estimated, double window) {
        if (reference.length == 0) return Double.NaN;
        if (estimated.length == 0) return 0.0;
        int matches = 0;
        for (boolean m : matchedEstimates(reference, estimated, window)) if (m) matches++;
        if (matches == 0) return 0.0;
        double precision = matches / (double) estimated.length;
        double recall = matches / (double) reference.length;
        return 2 * precision * recall / (precision + recall);
    }

    /**
     * Continuity-based scores. A reference beat is correct when its nearest estimated beat is
     * within {@value #CONTINUITY_TOLERANCE} of the reference interval in phase, and the estimated
     * interval leading to it is within the same tolerance of the reference interval. The
     * "any metrical level" scores also accept double tempo, off-beat and both half-tempo
     * variants of the reference.
     * @param reference reference beats
     * @param estimated estimated beats
     * @return the scores; all zero if either sequence has fewer than two beats
     */
    public static Continuity continuity(double[] reference, double[] estimated) {
        if (reference.length < 2 || estimated.length < 2) return new Continuity(0, 0, 0, 0);
        double[] correct = continuityOf(reference, estimated);
        double amlc = correct[0];
        double amlt = correct[1];
        for (double[] variant : List.of(doubled(reference), offBeat(reference),
                everyOther(reference, 0), everyOther(reference, 1))) {
            if (variant.length < 2) continue;
            double[] scores = continuityOf(variant, estimated);
            amlc = Math.max(amlc, scores[0]);
            amlt = Math.max(amlt, scores[1]);
        }
        return new Continuity(correct[0], correct[1], amlc, amlt);
    }

    /** {@code [longest run, total]} correct, as fractions of the reference beats. */
    private static double[] continuityOf(double[] reference, double[] estimated) {
        int run = 0;
        int longest = 0;
        int total = 0;
        for (int m = 0; m < reference.length; m++) {
            double refInterval = m == 0 ? reference[1] - reference[0] : reference[m] - reference[m - 1];
            int n = nearest(estimated, reference[m]);
            double estInterval = n == 0 ? estimated[1] - estimated[0] : estimated[n] - estimated[n - 1];
            boolean inPhase = Math.abs(estimated[n] - reference[m]) < CONTINUITY_TOLERANCE * refInterval;
            boolean inPeriod = Math.abs(1 - estInterval / refInterval) < CONTINUITY_TOLERANCE;
            if (inPhase && inPeriod) {
                total++;
                longest = Math.max(longest, ++run);
            } else {
                run = 0;
            }
        }
        return new double[] {longest / (double) reference.length, total / (double) reference.length};
    }

    private static int nearest(double[] sorted, double t) {
        int i = Arrays.binarySearch(sorted, t);
        if (i >= 0) return i;
        int after = -i - 1;
        if (after == 0) return 0;
        if (after == sorted.length) return sorted.length - 1;
        return t - sorted[after - 1] <= sorted[after] - t ? after - 1 : after;
    }

    /** Reference beats plus the midpoints between them (double tempo). */
    static double[] doubled(double[] beats) {
        double[] out = new double[beats.length * 2 - 1];
        for (int i = 0; i < beats.length; i++) {
            out[2 * i] = beats[i];
            if (i + 1 < beats.length) out[2 * i + 1] = (beats[i] + beats[i + 1]) / 2;
        }
        return out;
    }

    /** Midpoints between reference beats (off-beat). */
    static double[] offBeat(double[] beats) {
        double[] out = new double[beats.length - 1];
        for (int i = 0; i + 1 < beats.length; i++) out[i] = (beats[i] + beats[i + 1]) / 2;
        return out;
    }

    /** Every other reference beat starting at {@code start} (half tempo). */
    static double[] everyOther(double[] beats, int start) {
        double[] out = new double[(beats.length - start + 1) / 2];
        for (int i = 0; i < out.length; i++) out[i] = beats[start + 2 * i];
        return out;
    }

    /**
     * Time taken to lock on: the time of the first estimated beat that starts a run of
     * {@value #LOCK_RUN} consecutive estimated beats each matching a reference beat (within
     * {@link #BEAT_WINDOW}).
     * @param reference reference beats (untrimmed)
     * @param estimated estimated beats (untrimmed)
     * @return lock time in seconds, or {@code NaN} if it never locks
     */
    public static double lockTime(double[] reference, double[] estimated) {
        boolean[] matched = matchedEstimates(reference, estimated, BEAT_WINDOW);
        int streak = 0;
        for (int i = 0; i < matched.length; i++) {
            streak = matched[i] ? streak + 1 : 0;
            if (streak == LOCK_RUN) return estimated[i - LOCK_RUN + 1];
        }
        return Double.NaN;
    }
}
