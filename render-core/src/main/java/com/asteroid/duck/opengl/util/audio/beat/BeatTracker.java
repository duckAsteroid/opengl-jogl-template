package com.asteroid.duck.opengl.util.audio.beat;

/**
 * The full tracker: kicks from a {@link KickDetector}; the tempo from a {@link TempoEstimator}
 * fed by {@link SpectralFlux}, re-estimated every {@value #TEMPO_INTERVAL_SECONDS} s; and beats
 * predicted by a {@link BeatPhaseTracker} locked to the flux and the kicks. Beats are reported
 * at their predicted times as the clock reaches them, so they arrive on time, not late.
 *
 * <p>Spectral flux is the frame-to-frame increase in windowed energy, so it peaks as an onset
 * crosses the steepest part of the Hann window, a quarter of a window in from its newest end:
 * flux frames are timed that far before the end of the window that produced them.</p>
 */
public final class BeatTracker implements BeatAnalyser {

    static final double TEMPO_INTERVAL_SECONDS = 0.5;
    /** Tempo prior: centred on dance-music tempo, a wide octave either way. */
    static final double PRIOR_BPM = 120;
    static final double PRIOR_OCTAVES = 1.0;

    private final float sampleRate;
    private final KickDetector kicks;
    private final SpectralFlux flux;
    private final TempoEstimator tempo;
    private final BeatPhaseTracker phase;
    private final BeatListener listener;
    private final int framesPerEstimate;
    private long frames;

    /**
     * @param sampleRate sample rate of the audio, in Hz
     * @param listener   receives beats, kicks and tempo estimates
     */
    public BeatTracker(float sampleRate, BeatListener listener) {
        this(sampleRate, listener, PRIOR_BPM, PRIOR_OCTAVES);
    }

    /**
     * @param sampleRate   sample rate of the audio, in Hz
     * @param listener     receives beats, kicks and tempo estimates
     * @param priorBpm     centre of the tempo prior
     * @param priorOctaves width of the tempo prior, in octaves
     */
    public BeatTracker(float sampleRate, BeatListener listener, double priorBpm, double priorOctaves) {
        this.sampleRate = sampleRate;
        this.flux = new SpectralFlux(sampleRate, this::onFrame);
        this.phase = new BeatPhaseTracker(flux.frameRate(), listener);
        this.listener = listener;
        this.kicks = new KickDetector(sampleRate, new BeatListener() {
            @Override
            public void onKick(double time, float strength) {
                listener.onKick(time, strength);
                phase.addKick(time, strength);
            }
        });
        this.tempo = new TempoEstimator(flux.frameRate(), priorBpm, priorOctaves);
        this.framesPerEstimate = (int) Math.round(TEMPO_INTERVAL_SECONDS * flux.frameRate());
    }

    private void onFrame(double time, float value) {
        double onsetTime = time - quarterWindow();
        phase.addFrame(onsetTime, value);
        tempo.add(value);
        if (++frames % framesPerEstimate == 0 && tempo.estimate()) {
            listener.onTempo(time, tempo.bpm(), tempo.confidence());
            phase.onTempo(tempo.bpm(), onsetTime);
        }
    }

    private double quarterWindow() {
        return flux.fftSize() / 4.0 / sampleRate;
    }

    /**
     * The beat period the phase tracker is currently running at — the tempo estimate as refined
     * by the phase-locked loop.
     * @return seconds per beat, or {@code NaN} before the first tempo estimate
     */
    public double period() {
        return phase.period();
    }

    @Override
    public float getSampleRate() {
        return sampleRate;
    }

    @Override
    public void process(float[] samples, int offset, int length) {
        kicks.process(samples, offset, length);
        flux.process(samples, offset, length);
    }
}
