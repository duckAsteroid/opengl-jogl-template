package com.asteroid.duck.opengl.util.audio.beat;

/**
 * Estimates the tempo from a fixed-rate onset-strength signal (e.g. {@link SpectralFlux}).
 *
 * <p>On each {@link #estimate()}: autocorrelate the last {@value #WINDOW_SECONDS} s of onset
 * strength (mean removed); score each candidate beat period {@code L} as
 * {@code acf(L) + ½·acf(2L) + ¼·acf(4L)}, so a period whose two-beat and bar multiples also
 * repeat beats one that only repeats itself; weight by a log-normal tempo prior; then fold into
 * a running average of scores, which gives hysteresis — one odd window doesn't flip the tempo.
 * The winning period is refined by parabolic interpolation.</p>
 */
public final class TempoEstimator {

    static final double WINDOW_SECONDS = 8;
    static final double MIN_SECONDS = 3;
    static final double MIN_BPM = 60;
    static final double MAX_BPM = 200;
    /** Exponential smoothing weight given to each new score vector. */
    static final double SMOOTHING = 0.3;

    private final double frameRate;
    private final double priorBpm;
    private final double priorOctaves;
    private final float[] ring;
    private final double[] frame;
    private final double[] acf;
    private final double[] smoothed;
    private final int minLag;
    private final int maxLag;

    private long count;
    private double bpm = Double.NaN;
    private float confidence;

    /**
     * @param frameRate    onset frames per second
     * @param priorBpm     centre of the tempo prior
     * @param priorOctaves width (standard deviation, in octaves) of the tempo prior
     */
    public TempoEstimator(double frameRate, double priorBpm, double priorOctaves) {
        this.frameRate = frameRate;
        this.priorBpm = priorBpm;
        this.priorOctaves = priorOctaves;
        int window = (int) Math.round(WINDOW_SECONDS * frameRate);
        this.ring = new float[window];
        this.frame = new double[window];
        this.minLag = (int) Math.floor(60 * frameRate / MAX_BPM);
        this.maxLag = (int) Math.ceil(60 * frameRate / MIN_BPM);
        this.acf = new double[Math.min(window, 4 * maxLag + 1)];
        this.smoothed = new double[maxLag + 2];
    }

    /**
     * @param value the next onset-strength frame
     */
    public void add(float value) {
        ring[(int) (count++ % ring.length)] = value;
    }

    /**
     * Re-estimate from the frames added so far. Does nothing until {@value #MIN_SECONDS} s of
     * frames have arrived.
     * @return {@code true} if an estimate is available
     */
    public boolean estimate() {
        int n = (int) Math.min(count, ring.length);
        if (n < MIN_SECONDS * frameRate) return false;
        int start = (int) (count - n);
        double mean = 0;
        for (int i = 0; i < n; i++) mean += frame[i] = ring[(int) ((start + i) % ring.length)];
        mean /= n;
        for (int i = 0; i < n; i++) frame[i] -= mean;
        int lags = Math.min(acf.length, n);
        for (int lag = 0; lag < lags; lag++) {
            double sum = 0;
            for (int i = 0; i + lag < n; i++) sum += frame[i] * frame[i + lag];
            acf[lag] = sum / (n - lag);
        }
        if (acf[0] <= 0) return !Double.isNaN(bpm);

        for (int lag = minLag; lag <= maxLag; lag++) {
            double score = acf[lag] + 0.5 * at(2 * lag, lags) + 0.25 * at(4 * lag, lags);
            double octaves = Math.log(60 * frameRate / lag / priorBpm) / Math.log(2) / priorOctaves;
            score = Math.max(0, score / acf[0]) * Math.exp(-0.5 * octaves * octaves);
            smoothed[lag] = (1 - SMOOTHING) * smoothed[lag] + SMOOTHING * score;
        }

        int best = minLag;
        double total = 0;
        for (int lag = minLag; lag <= maxLag; lag++) {
            total += smoothed[lag];
            if (smoothed[lag] > smoothed[best]) best = lag;
        }
        if (smoothed[best] <= 0) return !Double.isNaN(bpm);
        double offset = 0;
        if (best > minLag && best < maxLag) {
            double a = smoothed[best - 1];
            double b = smoothed[best];
            double c = smoothed[best + 1];
            double denominator = a - 2 * b + c;
            if (denominator < 0) offset = Math.clamp(0.5 * (a - c) / denominator, -0.5, 0.5);
        }
        bpm = 60 * frameRate / (best + offset);
        double average = total / (maxLag - minLag + 1);
        confidence = (float) Math.clamp((smoothed[best] - average) / smoothed[best], 0, 1);
        return true;
    }

    private double at(int lag, int lags) {
        return lag < lags ? acf[lag] : 0;
    }

    /** @return the current tempo estimate in BPM, or {@code NaN} before the first */
    public double bpm() {
        return bpm;
    }

    /** @return how clearly the winning tempo stands out, in {@code [0, 1]} */
    public float confidence() {
        return confidence;
    }
}
