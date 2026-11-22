package com.asteroid.duck.opengl.util.audio.music;

import com.asteroid.duck.opengl.util.audio.beat.BeatListener;
import com.asteroid.duck.opengl.util.audio.beat.KickDetector;
import com.asteroid.duck.opengl.util.audio.beat.SpectralFlux;
import org.jtransforms.fft.FloatFFT_1D;

import java.util.Arrays;

/**
 * Describes music as a few slow-moving signals, for scripting visuals: how intense, bright,
 * noisy and busy it is, whether the kick is in, its key and mood, and which section it is in.
 *
 * <p>Every {@value #FRAME_SECONDS} s it Hann-windows the last ~{@value #WINDOW_SECONDS} s and takes
 * one FFT, which gives the level, band shares, spectral centroid (brightness), spectral flatness
 * (noisiness) and chroma (for the key). A {@link KickDetector} gives kicks and a
 * {@link SpectralFlux} gives onsets (busyness). The descriptors feed a {@link KeyEstimator},
 * the mood estimates and a {@link StructureDetector}, and the result is reported as a
 * {@link MusicState} to the {@link MusicListener} — with any {@link MusicEvent}s first.</p>
 *
 * <p>Tempo only feeds arousal, so the analyser doesn't track it itself: pass it in with
 * {@link #setTempo} from a {@link com.asteroid.duck.opengl.util.audio.beat.BeatTracker}, if there
 * is one. Without it, arousal is estimated from the other descriptors.</p>
 *
 * <p>Like the beat analysers, time is the sample clock (samples consumed / sample rate) and the
 * analyser is single-threaded: call {@link #process} from one thread. Cost is about 1% of real
 * time at 48 kHz.</p>
 */
public final class MusicAnalyser {

    /** Seconds between states. */
    public static final double FRAME_SECONDS = 0.05;
    static final double WINDOW_SECONDS = 0.17;
    static final double SILENCE_DBFS = -60;
    static final double SILENCE_SECONDS = 1.5;
    static final double SMOOTH_SECONDS = 0.5;
    static final double FAST_SECONDS = 0.1;
    static final double RATE_WINDOW_SECONDS = 3;
    static final double AROUSAL_SECONDS = 3;
    static final double VALENCE_SECONDS = 8;
    static final double LOW_HZ = 250;
    static final double HIGH_HZ = 4000;
    static final double BASS_HZ = 150;
    static final double MIN_HZ = 30;
    static final double CHROMA_MIN_HZ = 100;
    static final double CHROMA_MAX_HZ = 4000;
    static final double FLATNESS_MIN_HZ = 100;
    static final double FLATNESS_MAX_HZ = 16_000;
    /** Onsets per second that count as fully busy. */
    static final double BUSY_RATE = 8;
    /** Kicks per second that count as a full kick rate. */
    static final double KICK_RATE = 2.5;

    /** One frame's descriptors, passed to the {@link StructureDetector}; reused every frame. */
    static final class Frame {
        double time;
        boolean silent = true;
        float intensity, bass, bassFast, brightness, noisiness, busyness, kickRate, low, mid, high;
        double bpm = Double.NaN;
        final double[] chroma = new double[12];
    }

    private final float sampleRate;
    private final MusicListener listener;
    private final KickDetector kicks;
    private final SpectralFlux flux;
    private final KeyEstimator keys;
    private final StructureDetector structure;

    private final int size;
    private final int hop;
    private final double frameSeconds;
    private final FloatFFT_1D fft;
    private final float[] window;
    private final float[] ring;
    private final float[] work;
    private final double[] power;
    private final int[] pitchClass;
    private final double binHz;
    private final Frame frame = new Frame();

    private int writePos;
    private int sinceHop;
    private long samplesConsumed;

    // Level: mean square per hop, last 400 ms
    private final double[] hopLevels = new double[8];
    private double hopSum;
    private long hops;

    // Silence gate: peak 10 ms mean square over the last SILENCE_SECONDS
    private final int levelChunk;
    private final double[] chunkLevels;
    private double chunkSum;
    private int chunkCount;
    private long chunks;
    private boolean silent = true;

    // Event rings
    private final double[] onsetTimes = new double[64];
    private int onsetCount;
    private final double[] kickTimes = new double[16];
    private int kickCount;
    private double fluxMean;
    private double fluxDeviation;
    private float flux1;
    private float flux2;
    private double lastOnset = Double.NEGATIVE_INFINITY;

    // Smoothing
    private final AutoRange intensityRange = new AutoRange(12, 40, 1.0);
    private final AutoRange bassRange = new AutoRange(12, 40, 1.0);
    private final double smooth;
    private final double fast;
    private final double arousalAlpha;
    private final double valenceAlpha;
    private float intensity, bass, bassFast, brightness, noisiness, low, mid, high;
    private float arousal = 0.5f;
    private float valence = 0.5f;

    private double bpm = Double.NaN;

    /**
     * @param sampleRate sample rate of the audio, in Hz
     * @param listener   receives states and events
     */
    public MusicAnalyser(float sampleRate, MusicListener listener) {
        this.sampleRate = sampleRate;
        this.listener = listener;
        int size = Integer.highestOneBit((int) (sampleRate * WINDOW_SECONDS)) * 2;
        if (size > sampleRate * 1.5 * WINDOW_SECONDS) size /= 2;
        this.size = size;
        this.hop = Math.round(sampleRate * (float) FRAME_SECONDS);
        this.frameSeconds = hop / (double) sampleRate;
        this.fft = new FloatFFT_1D(size);
        this.window = new float[size];
        for (int i = 0; i < size; i++) window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / size));
        this.ring = new float[size];
        this.work = new float[size];
        this.power = new double[size / 2];
        this.binHz = sampleRate / (double) size;
        this.pitchClass = new int[size / 2];
        for (int k = 0; k < size / 2; k++) {
            double hz = k * binHz;
            pitchClass[k] = hz < CHROMA_MIN_HZ || hz > CHROMA_MAX_HZ ? -1
                    : (int) Math.floorMod(Math.round(12 * Math.log(hz / 440.0) / Math.log(2)) + 9, 12L);
        }
        this.levelChunk = Math.max(1, Math.round(sampleRate / 100));
        this.chunkLevels = new double[(int) Math.round(SILENCE_SECONDS * 100)];

        this.smooth = 1 - Math.exp(-frameSeconds / SMOOTH_SECONDS);
        this.fast = 1 - Math.exp(-frameSeconds / FAST_SECONDS);
        this.arousalAlpha = 1 - Math.exp(-frameSeconds / AROUSAL_SECONDS);
        this.valenceAlpha = 1 - Math.exp(-frameSeconds / VALENCE_SECONDS);

        this.keys = new KeyEstimator(frameSeconds);
        this.structure = new StructureDetector(frameSeconds, listener::onEvent);
        this.kicks = new KickDetector(sampleRate, new BeatListener() {
            @Override
            public void onKick(double time, float strength) {
                kickTimes[kickCount++ % kickTimes.length] = time;
                structure.onKick(time);
            }
        });
        this.flux = new SpectralFlux(sampleRate, this::onFlux);
    }

    /** @return the sample rate of the audio passed to {@link #process}, in Hz */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * The current tempo, e.g. from a {@link com.asteroid.duck.opengl.util.audio.beat.BeatTracker}.
     * Used only for arousal and for spotting track changes.
     * @param bpm tempo in BPM, or {@code NaN} if unknown
     */
    public void setTempo(double bpm) {
        this.bpm = bpm;
    }

    /**
     * Consume the next block of samples, reporting a state every {@value #FRAME_SECONDS} s.
     * @param samples mono samples in {@code [-1, 1]}, oldest first
     * @param offset  index of the first sample
     * @param length  number of samples
     */
    public void process(float[] samples, int offset, int length) {
        kicks.process(samples, offset, length);
        flux.process(samples, offset, length);
        for (int i = offset; i < offset + length; i++) {
            float s = samples[i];
            ring[writePos] = s;
            writePos = (writePos + 1) % size;
            samplesConsumed++;
            hopSum += s * s;
            trackSilence(s);
            if (++sinceHop == hop) {
                sinceHop = 0;
                hopLevels[(int) (hops++ % hopLevels.length)] = hopSum / hop;
                hopSum = 0;
                frame();
            }
        }
    }

    private void trackSilence(float s) {
        chunkSum += s * s;
        if (++chunkCount < levelChunk) return;
        chunkLevels[(int) (chunks++ % chunkLevels.length)] = chunkSum / chunkCount;
        chunkSum = 0;
        chunkCount = 0;
        if (chunks < chunkLevels.length) {
            if (10 * Math.log10(chunkLevels[(int) (chunks - 1)] + 1e-20) >= SILENCE_DBFS) silent = false;
            return;
        }
        double peak = 0;
        for (double level : chunkLevels) peak = Math.max(peak, level);
        silent = 10 * Math.log10(peak + 1e-20) < SILENCE_DBFS;
    }

    /** Peak-pick onsets from spectral flux: a local maximum well above the recent average. */
    private void onFlux(double time, float value) {
        if (flux1 > flux2 && flux1 >= value && flux1 > fluxMean + 1.5 * fluxDeviation + 1
                && time - lastOnset >= 0.05) {
            lastOnset = time;
            onsetTimes[onsetCount++ % onsetTimes.length] = time;
        }
        fluxMean += 0.02 * (value - fluxMean);
        fluxDeviation += 0.02 * (Math.abs(value - fluxMean) - fluxDeviation);
        flux2 = flux1;
        flux1 = value;
    }

    private void frame() {
        double now = samplesConsumed / (double) sampleRate;
        double meanSquare = 0;
        int levels = (int) Math.min(hops, hopLevels.length);
        for (int i = 0; i < levels; i++) meanSquare += hopLevels[i];
        double levelDb = 10 * Math.log10(meanSquare / Math.max(1, levels) + 1e-12);

        boolean wasSilent = frame.silent;
        frame.time = now;
        frame.silent = silent;
        frame.bpm = bpm;
        if (wasSilent && !silent) {
            // new music: forget the old ranges and key
            intensityRange.reset();
            bassRange.reset();
            keys.reset();
        }

        spectrum();
        double lowE = 0, midE = 0, highE = 0, bassE = 0, weighted = 0, total = 0;
        double logSum = 0, flatSum = 0;
        int flatBins = 0;
        Arrays.fill(frame.chroma, 0);
        for (int k = 1; k < power.length; k++) {
            double hz = k * binHz;
            double p = power[k];
            if (hz >= MIN_HZ) {
                if (hz < LOW_HZ) lowE += p; else if (hz < HIGH_HZ) midE += p; else highE += p;
                if (hz < BASS_HZ) bassE += p;
                double magnitude = Math.sqrt(p);
                weighted += hz * magnitude;
                total += magnitude;
            }
            if (hz >= FLATNESS_MIN_HZ && hz <= FLATNESS_MAX_HZ) {
                logSum += Math.log(p + 1e-20);
                flatSum += p;
                flatBins++;
            }
            if (pitchClass[k] >= 0) frame.chroma[pitchClass[k]] += Math.log1p(100 * Math.sqrt(p));
        }
        double norm = 0;
        for (double v : frame.chroma) norm += v * v;
        if (norm > 0) for (int i = 0; i < 12; i++) frame.chroma[i] /= Math.sqrt(norm);

        if (!silent) {
            float intensityNow = (float) intensityRange.update(levelDb, frameSeconds);
            float bassNow = (float) bassRange.update(10 * Math.log10(bassE + 1e-12), frameSeconds);
            double centroid = total > 0 ? weighted / total : 0;
            float brightnessNow = centroid > 0 ? (float) Math.clamp(Math.log(centroid / 200) / Math.log(40), 0, 1) : 0;
            double flatness = flatBins > 0 && flatSum > 0 ? Math.exp(logSum / flatBins) / (flatSum / flatBins) : 0;
            float noisinessNow = (float) Math.clamp((10 * Math.log10(flatness + 1e-12) + 35) / 32, 0, 1);
            double energy = lowE + midE + highE;
            intensity = ease(intensity, intensityNow, smooth);
            bass = ease(bass, bassNow, smooth);
            bassFast = ease(bassFast, bassNow, fast);
            brightness = ease(brightness, brightnessNow, smooth);
            noisiness = ease(noisiness, noisinessNow, smooth);
            if (energy > 0) {
                low = ease(low, (float) (lowE / energy), smooth);
                mid = ease(mid, (float) (midE / energy), smooth);
                high = ease(high, (float) (highE / energy), smooth);
            }
            keys.add(frame.chroma);
        } else {
            intensity = ease(intensity, 0, smooth);
            bass = ease(bass, 0, smooth);
            bassFast = ease(bassFast, 0, fast);
        }
        float busyness = (float) Math.min(1, countSince(onsetTimes, onsetCount, now - RATE_WINDOW_SECONDS)
                / RATE_WINDOW_SECONDS / BUSY_RATE);
        float kickRate = (float) Math.min(1, countSince(kickTimes, kickCount, now - RATE_WINDOW_SECONDS)
                / RATE_WINDOW_SECONDS / KICK_RATE);

        frame.intensity = intensity;
        frame.bass = bass;
        frame.bassFast = bassFast;
        frame.brightness = brightness;
        frame.noisiness = noisiness;
        frame.busyness = busyness;
        frame.kickRate = kickRate;
        frame.low = low;
        frame.mid = mid;
        frame.high = high;
        structure.update(frame);

        Key key = keys.key();
        float keyConfidence = keys.confidence();
        updateMood(key, keyConfidence, busyness, kickRate);

        listener.onState(new MusicState(now, levelDb, silent, intensity, bass, brightness, noisiness,
                busyness, kickRate, low, mid, high, key, keyConfidence, arousal, valence,
                structure.section(), structure.sectionStart(), structure.novelty()));
    }

    /**
     * Arousal is well founded — loudness, onset rate, brightness and tempo are what listeners'
     * energy ratings track. Valence is a weak hint: major/minor (scaled by key confidence) plus a
     * little brightness.
     */
    private void updateMood(Key key, float keyConfidence, float busyness, float kickRate) {
        if (silent) {
            arousal = ease(arousal, 0, arousalAlpha);
            return;
        }
        double sum = 0.3 * intensity + 0.2 * busyness + 0.15 * brightness + 0.15 * kickRate;
        double weights = 0.8;
        if (Double.isFinite(bpm) && bpm > 0) {
            sum += 0.2 * Math.clamp((bpm - 60) / 120, 0, 1);
            weights += 0.2;
        }
        arousal = ease(arousal, (float) (sum / weights), arousalAlpha);
        double mode = key == null ? 0 : (key.major() ? 1 : -1) * keyConfidence;
        valence = ease(valence, (float) Math.clamp(0.5 + 0.3 * mode + 0.2 * (brightness - 0.5), 0, 1), valenceAlpha);
    }

    private void spectrum() {
        for (int i = 0; i < size; i++) work[i] = ring[(writePos + i) % size] * window[i];
        fft.realForward(work);
        double scale = 2.0 / size;
        for (int k = 1; k < power.length; k++) {
            double re = work[2 * k] * scale;
            double im = work[2 * k + 1] * scale;
            power[k] = re * re + im * im;
        }
    }

    private static int countSince(double[] times, int count, double since) {
        int n = 0;
        for (int i = 0; i < Math.min(count, times.length); i++) if (times[i] >= since) n++;
        return n;
    }

    private static float ease(float current, float target, double alpha) {
        return (float) (current + alpha * (target - current));
    }
}
