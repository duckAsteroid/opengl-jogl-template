package com.asteroid.duck.opengl.util.audio.beat.eval;

import com.asteroid.duck.opengl.util.audio.analysis.BeatDetector;
import com.asteroid.duck.opengl.util.audio.analysis.FFTProcessor;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyBand;
import com.asteroid.duck.opengl.util.audio.beat.BeatAnalyser;
import com.asteroid.duck.opengl.util.audio.beat.BeatListener;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The bar to beat: the existing {@link BeatDetector} as {@code SpectrumWave} tunes and drives it
 * (4096-point FFT, updated at 60 Hz, 40–200 Hz bass band), turned into events.
 * <ul>
 *   <li>Kick (and beat) = the bass band's beat strength rising through {@value #TRIGGER}. There
 *       is no separate beat tracking, so every kick is reported as a beat too.</li>
 *   <li>Tempo, once a second = 60 / median interval between the last
 *       {@value #TEMPO_WINDOW_SECONDS} s of kicks, folded into 70–180 BPM.</li>
 * </ul>
 */
final class BaselineAnalyser implements BeatAnalyser {

    private static final int FFT_SIZE = 4096;
    private static final float UPDATE_RATE = 60f;
    private static final float TRIGGER = 0.5f;
    private static final double TEMPO_WINDOW_SECONDS = 8.0;
    private static final double TEMPO_INTERVAL_SECONDS = 1.0;
    private static final double MIN_BPM = 70;
    private static final double MAX_BPM = 180;

    private final float sampleRate;
    private final BeatListener listener;
    private final FFTProcessor fft;
    private final BeatDetector detector;
    private final int hop;
    private final float[] ring = new float[FFT_SIZE];
    private final float[] window = new float[FFT_SIZE];
    private final float[] bars = new float[12];
    private final Deque<Double> recentKicks = new ArrayDeque<>();

    private int writePos;
    private int sinceHop;
    private long samplesConsumed;
    private float lastStrength;
    private double nextTempoAt = TEMPO_INTERVAL_SECONDS;

    BaselineAnalyser(float sampleRate, BeatListener listener) {
        this.sampleRate = sampleRate;
        this.listener = listener;
        this.fft = new FFTProcessor(FFT_SIZE, bars.length, sampleRate, 20f, Math.min(20_000f, sampleRate / 2), -80f, 0f);
        this.detector = new BeatDetector(List.of(new FrequencyBand("bass", 40f, 200f)), FFT_SIZE, sampleRate,
                120, 1.15f, 4.0f, BeatDetector.DEFAULT_NOISE_FLOOR, Duration.ofMillis(333));
        this.hop = Math.round(sampleRate / UPDATE_RATE);
    }

    @Override
    public float getSampleRate() {
        return sampleRate;
    }

    @Override
    public void process(float[] samples, int offset, int length) {
        for (int i = offset; i < offset + length; i++) {
            ring[writePos] = samples[i];
            writePos = (writePos + 1) % FFT_SIZE;
            samplesConsumed++;
            if (++sinceHop >= hop) {
                sinceHop = 0;
                analyse();
            }
        }
    }

    private void analyse() {
        int tail = FFT_SIZE - writePos;
        System.arraycopy(ring, writePos, window, 0, tail);
        System.arraycopy(ring, 0, window, tail, writePos);
        fft.process(window, bars);
        detector.update(fft.getRawMagnitudes(), hop / (double) sampleRate);

        double now = samplesConsumed / (double) sampleRate;
        float strength = detector.getBeatStrength(0);
        if (lastStrength < TRIGGER && strength >= TRIGGER) {
            listener.onKick(now, strength);
            listener.onBeat(now);
            recentKicks.addLast(now);
        }
        lastStrength = strength;

        while (!recentKicks.isEmpty() && recentKicks.peekFirst() < now - TEMPO_WINDOW_SECONDS) {
            recentKicks.removeFirst();
        }
        if (now >= nextTempoAt) {
            nextTempoAt += TEMPO_INTERVAL_SECONDS;
            reportTempo(now);
        }
    }

    private void reportTempo(double now) {
        if (recentKicks.size() < 4) return;
        Double[] kicks = recentKicks.toArray(new Double[0]);
        double[] intervals = new double[kicks.length - 1];
        for (int i = 1; i < kicks.length; i++) intervals[i - 1] = kicks[i] - kicks[i - 1];
        double median = Stats.median(intervals);
        double bpm = 60 / median;
        while (bpm < MIN_BPM) bpm *= 2;
        while (bpm >= MAX_BPM) bpm /= 2;
        int agreeing = 0;
        for (double interval : intervals) if (Math.abs(interval - median) <= 0.1 * median) agreeing++;
        listener.onTempo(now, bpm, agreeing / (float) intervals.length);
    }
}
