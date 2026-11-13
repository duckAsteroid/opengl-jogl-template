package com.asteroid.duck.opengl.util.audio.beat.eval;

import java.util.Arrays;

/** Small NaN-aware statistics helpers. */
final class Stats {

    private Stats() {}

    /**
     * @param values values; {@code NaN}s are ignored
     * @return the median, or {@code NaN} if there are no non-NaN values
     */
    static double median(double[] values) {
        double[] sorted = Arrays.stream(values).filter(v -> !Double.isNaN(v)).sorted().toArray();
        if (sorted.length == 0) return Double.NaN;
        int mid = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[mid] : (sorted[mid - 1] + sorted[mid]) / 2;
    }

    /**
     * @param values values; {@code NaN}s are ignored
     * @return the mean, or {@code NaN} if there are no non-NaN values
     */
    static double mean(double[] values) {
        return Arrays.stream(values).filter(v -> !Double.isNaN(v)).average().orElse(Double.NaN);
    }

    /**
     * @param values values; {@code NaN}s are ignored
     * @return how many are not {@code NaN}
     */
    static long count(double[] values) {
        return Arrays.stream(values).filter(v -> !Double.isNaN(v)).count();
    }
}
