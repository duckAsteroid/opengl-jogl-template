package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioReader;
import com.asteroid.duck.opengl.util.audio.AudioSources;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.audio.simulated.SimulatedSources;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyProcessor;
import com.asteroid.duck.opengl.util.audio.beat.BeatTrackerSink;
import com.asteroid.duck.opengl.util.wave.RadialSpectrumAnalyser;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Radial spectrum analyser experiment: a smooth filled shape radiates inward and outward
 * from a base circle as audio energy varies across the frequency spectrum.
 *
 * <p>A single {@link FrequencyProcessor} at 128 bins drives the {@link RadialSpectrumAnalyser},
 * which uses {@code GL_LINEAR} texture filtering to produce a smooth curve between FFT bins — no
 * hard bar edges. A continuous white peak-hold line traces the historical outer maximum.</p>
 *
 * <p>A {@link BeatTrackerSink} on the same audio feed makes the whole shape pulse outward on
 * each tracked beat (scaled by tempo confidence) and kick a little further on each detected
 * kick drum.</p>
 *
 * <p>Key bindings: <kbd>J</kbd> / <kbd>H</kbd> cycle audio inputs; <kbd>P</kbd> saves a screenshot;
 * <kbd>]</kbd> / <kbd>[</kbd> increase or decrease the repeat count (1–16, default 1);
 * <kbd>.</kbd> / <kbd>,</kbd> move the beat pulse 10 ms earlier / later.</p>
 */
public class RadialSpectrumWave implements Experiment {

    // ── FFT parameters ───────────────────────────────────────────────────────────
    private static final int   FFT_BINS    = 128;
    private static final int   FFT_SIZE    = 4096;
    private static final float SAMPLE_RATE = 48_000f;
    private static final float F_MIN       = 20f;
    private static final float F_MAX       = 20_000f;
    private static final float DB_FLOOR    = -80f;
    private static final float DB_CEILING  = 0f;

    // ── Beat pulse ───────────────────────────────────────────────────────────────
    /** Extra scale on the beat at full confidence, decaying through the beat. */
    private static final float BEAT_PULSE = 0.06f;
    /** Extra scale from a full-strength kick, decaying with the kick envelope. */
    private static final float KICK_PULSE = 0.04f;
    /** Pulse decay rate through the beat: {@code exp(-k * phase)}. */
    private static final double PULSE_DECAY = 6;
    private static final Duration LATENCY_STEP = Duration.ofMillis(10);

    // ── Colours: inner tip → base circle → outer tip → peak line ─────────────────
    private static final Vector4f COLOR_INNER = new Vector4f(0.0f, 0.2f, 0.6f, 1.0f);
    private static final Vector4f COLOR_BASE  = new Vector4f(0.0f, 0.7f, 0.3f, 1.0f);
    private static final Vector4f COLOR_OUTER = new Vector4f(0.9f, 0.1f, 0.0f, 1.0f);
    private static final Vector4f COLOR_PEAK  = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

    // ── Pipeline ─────────────────────────────────────────────────────────────────
    private final FrequencyProcessor freqProc =
            new FrequencyProcessor(FFT_SIZE, FFT_BINS, SAMPLE_RATE, F_MIN, F_MAX, DB_FLOOR, DB_CEILING);

    private final RadialSpectrumAnalyser radial =
            new RadialSpectrumAnalyser(freqProc)
                    .withColors(COLOR_INNER, COLOR_BASE, COLOR_OUTER)
                    .withPeakColor(COLOR_PEAK);

    private final BeatTrackerSink beatTracker = new BeatTrackerSink(SAMPLE_RATE);
    /** Reused every frame; {@code setTransform} copies it. */
    private final Matrix4f pulse = new Matrix4f();

    private final AudioSources audioSources = new AudioSources();
    private int selectedSource = 0;
    private AudioReader audioReader;
    private Thread      audioReaderThread;

    @Override
    public String getTitle() {
        return "RadialSpectrumWave";
    }

    @Override
    public String getDescription() {
        return "Radial spectrum analyser — smooth filled shape extending inward and outward from a base circle";
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        freqProc.addSink(radial);
        radial.init(ctx);

        audioReader = new AudioReader(List.of(freqProc, beatTracker));
        audioReaderThread = new Thread(audioReader, "radial-spectrum-audio-reader");
        audioReaderThread.setDaemon(true);
        audioReaderThread.start();

        audioSources.add(SimulatedSources.middleC(ctx.getClock()));
        LineAcquirer.allLinesMatching(LineAcquirer.IDEAL)
                .map(LineAcquirer.MixerLine::toAudioDataSource)
                .forEach(audioSources::add);
        audioReader.setLine(audioSources.list().get(selectedSource));

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), () -> {
            selectedSource = (selectedSource + 1) % audioSources.size();
            audioReader.setLine(audioSources.list().get(selectedSource));
        }, "Switch to the next audio input");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
            selectedSource = (selectedSource - 1 + audioSources.size()) % audioSources.size();
            audioReader.setLine(audioSources.list().get(selectedSource));
        }, "Switch to the previous audio input");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("RIGHT_BRACKET"), () ->
                radial.withRepeats(Math.min(16, radial.getRepeats() + 1)),
                "Increase spectrum repeats");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("LEFT_BRACKET"), () ->
                radial.withRepeats(Math.max(1, radial.getRepeats() - 1)),
                "Decrease spectrum repeats");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("PERIOD"), () ->
                beatTracker.setLatency(beatTracker.latency().plus(LATENCY_STEP)),
                "Beat pulse earlier (more latency compensation)");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("COMMA"), () ->
                beatTracker.setLatency(beatTracker.latency().minus(LATENCY_STEP)),
                "Beat pulse later (less latency compensation)");
    }

    @Override
    public void doRender(RenderContext ctx) {
        freqProc.process();
        float scale = 1
                + BEAT_PULSE * beatTracker.confidence() * (float) Math.exp(-PULSE_DECAY * beatTracker.beatPhase())
                + KICK_PULSE * beatTracker.kickStrength();
        radial.setTransform(pulse.identity().scale(scale));
        radial.doRender(ctx);
    }

    @Override
    public void dispose() {
        audioReader.setRunning(false);
        audioReader.setLine(null);
        try {
            audioReaderThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        radial.dispose();
    }
}
