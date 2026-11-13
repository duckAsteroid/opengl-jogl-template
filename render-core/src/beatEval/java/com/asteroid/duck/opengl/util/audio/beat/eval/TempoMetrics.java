package com.asteroid.duck.opengl.util.audio.beat.eval;

/**
 * The standard tempo accuracy measures (Gouyon et al. 2006). Results are {@code 1} (correct),
 * {@code 0} (wrong) or {@code NaN} (either tempo unknown), so they average into a percentage.
 */
public final class TempoMetrics {

    /** Relative tolerance for a correct tempo. */
    public static final double TOLERANCE = 0.04;

    /** Metrical-level factors Acc2 accepts as well as the tempo itself. */
    private static final double[] ACC2_FACTORS = {1, 2, 0.5, 3, 1.0 / 3};

    private TempoMetrics() {}

    /**
     * Accuracy 1: estimate within {@value #TOLERANCE} of the reference.
     * @param estimated estimated tempo in BPM
     * @param reference reference tempo in BPM
     * @return 1, 0 or NaN
     */
    public static double acc1(double estimated, double reference) {
        if (Double.isNaN(estimated) || Double.isNaN(reference)) return Double.NaN;
        return Math.abs(estimated - reference) <= TOLERANCE * reference ? 1 : 0;
    }

    /**
     * Accuracy 2: as Acc1, but also accepting 2×, ½×, 3× and ⅓× the reference.
     * @param estimated estimated tempo in BPM
     * @param reference reference tempo in BPM
     * @return 1, 0 or NaN
     */
    public static double acc2(double estimated, double reference) {
        if (Double.isNaN(estimated) || Double.isNaN(reference)) return Double.NaN;
        for (double factor : ACC2_FACTORS) {
            if (acc1(estimated, reference * factor) == 1) return 1;
        }
        return 0;
    }
}
