package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioReader;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyBand;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyProcessor;
import com.asteroid.duck.opengl.util.audio.analysis.BeatDetector;
import com.asteroid.duck.opengl.util.wave.RadialSpectrumAnalyser;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Radial spectrum analyser experiment: a smooth filled shape radiates inward and outward
 * from a base circle as audio energy varies across the frequency spectrum.
 *
 * <p>A {@link FrequencyProcessor} at 128 bins feeds a {@link BeatDetector} for beat-strength
 * data. A separate display processor at {@link RadialSpectrumAnalyser#DEFAULT_RING_VERTS}
 * vertices drives the {@link RadialSpectrumAnalyser}, which uses {@code GL_LINEAR} texture
 * filtering to produce a smooth curve between FFT bins — no hard bar edges.
 * A continuous white peak-hold line traces the historical outer maximum.</p>
 *
 * <p>Key bindings: <kbd>J</kbd> / <kbd>H</kbd> cycle audio inputs; <kbd>P</kbd> saves a screenshot;
 * <kbd>]</kbd> / <kbd>[</kbd> increase or decrease the repeat count (1–16, default 1).</p>
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

    // ── Beat bands (same tuning as SpectrumWave) ─────────────────────────────────
    private static final List<FrequencyBand> BEAT_BANDS = List.of(
            new FrequencyBand("bass",   40f,   200f),
            new FrequencyBand("snare",  200f, 2_000f),
            new FrequencyBand("hihat", 4_000f, 16_000f)
    );

    // ── Colours: inner tip → base circle → outer tip → peak line ─────────────────
    private static final Vector3f COLOR_INNER = new Vector3f(0.0f, 0.2f, 0.6f);
    private static final Vector3f COLOR_BASE  = new Vector3f(0.0f, 0.7f, 0.3f);
    private static final Vector3f COLOR_OUTER = new Vector3f(0.9f, 0.1f, 0.0f);
    private static final Vector3f COLOR_PEAK  = new Vector3f(1.0f, 1.0f, 1.0f);

    // ── Pipeline ─────────────────────────────────────────────────────────────────
    private final FrequencyProcessor freqProc =
            new FrequencyProcessor(FFT_SIZE, FFT_BINS, SAMPLE_RATE, F_MIN, F_MAX, DB_FLOOR, DB_CEILING);

    private final RadialSpectrumAnalyser radial =
            new RadialSpectrumAnalyser(freqProc)
                    .withColors(COLOR_INNER, COLOR_BASE, COLOR_OUTER)
                    .withPeakColor(COLOR_PEAK);

    private final BeatDetector beats = new BeatDetector(
            BEAT_BANDS, freqProc.getNumBins(), freqProc.getFMin(), freqProc.getFMax(),
            120, 1.15f, 4.0f, 1f / 20f);

    private final LineAcquirer lineAcquirer = new LineAcquirer();
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
        freqProc.addSink(beats);
        radial.init(ctx);

        audioReader = new AudioReader(List.of(freqProc));
        audioReaderThread = new Thread(audioReader, "radial-spectrum-audio-reader");
        audioReaderThread.setDaemon(true);
        audioReaderThread.start();

        lineAcquirer.init(ctx, LineAcquirer.IDEAL);
        audioReader.setLine(lineAcquirer.getSelectedSource());

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), () -> {
            lineAcquirer.next();
            audioReader.setLine(lineAcquirer.getSelectedSource());
        }, "Switch to the next audio input");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('H'), () -> {
            lineAcquirer.previous();
            audioReader.setLine(lineAcquirer.getSelectedSource());
        }, "Switch to the previous audio input");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("RIGHT_BRACKET"), () ->
                radial.withRepeats(Math.min(16, radial.getRepeats() + 1)),
                "Increase spectrum repeats");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("LEFT_BRACKET"), () ->
                radial.withRepeats(Math.max(1, radial.getRepeats() - 1)),
                "Decrease spectrum repeats");
    }

    @Override
    public void doRender(RenderContext ctx) {
        freqProc.process();
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
