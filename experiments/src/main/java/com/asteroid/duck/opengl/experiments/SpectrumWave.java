package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioReader;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.text.StringRenderer;
import com.asteroid.duck.opengl.util.audio.analysis.BeatDetector;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyBand;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyProcessor;
import com.asteroid.duck.opengl.util.wave.SpectrumAnalyser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.awt.*;
import java.io.IOException;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * Spectrum analyser experiment: log-frequency bar chart (20 Hz–20 kHz) with retro peak-hold ticks
 * and beat-detection letter flash overlaid on the bars.
 *
 * <p>A {@link FrequencyProcessor} acts as the shared FFT source, feeding both a
 * {@link SpectrumAnalyser} (bar and tick renderer) and a {@link BeatDetector}. On each beat in the
 * bass / mid / treble bands the corresponding letter (B / M / T) flashes at full brightness over
 * the spectrum bars; between beats each letter stays visible at {@value #LETTER_DIM_ALPHA} alpha.</p>
 *
 * <p>Key bindings: <kbd>J</kbd> / <kbd>H</kbd> step through audio inputs; <kbd>P</kbd> saves a
 * screenshot.</p>
 */
public class SpectrumWave implements Experiment {

	// ── FFT / display parameters ──────────────────────────────────────────────
	private static final int   FFT_BINS     = 128; // resolution for beat detection
	private static final int   DISPLAY_BINS = 12;  // bars shown on screen (~1 octave each)
	private static final int   FFT_SIZE     = 4096;
	private static final float SAMPLE_RATE  = 48_000f;
	private static final float F_MIN        = 20f;
	private static final float F_MAX        = 20_000f;
	private static final float DB_FLOOR     = -80f;
	private static final float DB_CEILING   = 0f;
	private static final float GAP          = SpectrumAnalyser.DEFAULT_GAP;

	// ── Colour gradient: green (low) → red (high) ────────────────────────────
	private static final Vector3f COLOR_LOW  = new Vector3f(0.0f, 0.8f, 0.0f);
	private static final Vector3f COLOR_HIGH = new Vector3f(0.8f, 0.0f, 0.0f);

	// ── Beat frequency bands (tighter bass range to skip sub-bass rumble) ────
	private static final List<FrequencyBand> BEAT_BANDS = List.of(
			new FrequencyBand("bass",   40f,   200f),
			new FrequencyBand("snare",  200f, 2_000f),
			new FrequencyBand("hihat", 4_000f, 16_000f)
	);

	// ── Beat letter display ───────────────────────────────────────────────────
	/** Alpha of each letter when no beat is detected — keeps them faintly visible. */
	private static final float LETTER_DIM_ALPHA = 0.15f;
	/** Font point size used for the shared font atlas. Scaled up at render time. */
	private static final int FONT_PT = 150;

	// ── Pipeline ──────────────────────────────────────────────────────────────
	// High-resolution processor: 128 bins → BeatDetector (more bins = better kick/snare separation)
	private final FrequencyProcessor freqProc =
			new FrequencyProcessor(FFT_SIZE, FFT_BINS, SAMPLE_RATE, F_MIN, F_MAX, DB_FLOOR, DB_CEILING);

	// Display processor: 12 bins → SpectrumAnalyser (~1 octave per bar)
	private final FrequencyProcessor displayProc =
			new FrequencyProcessor(FFT_SIZE, DISPLAY_BINS, SAMPLE_RATE, F_MIN, F_MAX, DB_FLOOR, DB_CEILING);

	private final SpectrumAnalyser analyser =
			new SpectrumAnalyser(displayProc, GAP).withBarColors(COLOR_LOW, COLOR_HIGH);

	private final BeatDetector beats = new BeatDetector(
			BEAT_BANDS, freqProc.getNumBins(), freqProc.getFMin(), freqProc.getFMax(),
			120,    // ~2 s history — stable baseline across 4/4 kick pattern at 120 BPM
			1.15f,  // lower threshold: sustained bass line means kick only clears ~15–25% above avg
			4.0f,   // sensitivity: 25% spike → full beat strength
			1f/20f  // fast decay — clears in ~0.3 s so next kick can re-trigger
	);

	private final LineAcquirer lineAcquirer = new LineAcquirer();

	private AudioReader audioReader;
	private Thread      audioReaderThread;

	// ── Beat letter renderers (shared font texture) ───────────────────────────
	private FontTexture    fontTexture;
	private StringRenderer bassLabel;
	private StringRenderer midLabel;
	private StringRenderer trebleLabel;

	@Override
	public String getDescription() {
		return "Traditional bar-chart spectrum analyser — log frequency (20 Hz–20 kHz), dB amplitude, peak hold";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		displayProc.addSink(analyser);
		freqProc.addSink(beats);
		analyser.init(ctx);

		audioReader = new AudioReader(List.of(freqProc, displayProc));
		audioReaderThread = new Thread(audioReader, "spectrum-audio-reader");
		audioReaderThread.setDaemon(true);
		audioReaderThread.start();

		lineAcquirer.init(ctx, LineAcquirer.IDEAL);
		audioReader.setLine(lineAcquirer.getSelectedSource());

		initBeatLabels(ctx);

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
	}

	private void initBeatLabels(RenderContext ctx) throws IOException {
		fontTexture = new FontTextureFactory(new Font("Arial", Font.BOLD, FONT_PT), true)
				.createFontTexture();

		bassLabel   = new StringRenderer(fontTexture);
		midLabel    = new StringRenderer(fontTexture);
		trebleLabel = new StringRenderer(fontTexture);

		bassLabel.init(ctx);
		midLabel.init(ctx);
		trebleLabel.init(ctx);

		bassLabel.setText("B");
		midLabel.setText("M");
		trebleLabel.setText("T");

		positionLabels(ctx.getWindow());

		bassLabel.setTextColor(  StandardColors.WHITE.withAlpha(LETTER_DIM_ALPHA));
		midLabel.setTextColor(   StandardColors.WHITE.withAlpha(LETTER_DIM_ALPHA));
		trebleLabel.setTextColor(StandardColors.WHITE.withAlpha(LETTER_DIM_ALPHA));
	}

	private void positionLabels(Rectangle win) {
		// Scale so letters fill 75% of the screen height using the real rendered font height.
		float fontH = fontTexture.getHeight("B");
		float scale = win.height * 0.75f / fontH;

		// rawBounds places the glyph at (cursor.y - datumOffset.y), so datumOffset.y is
		// the ascent in font pixels. We want the letter top at 10% from the screen top.
		float ascent = fontTexture.getGlyph('B').datumOffset().y;
		float baseline = win.height * 0.10f + ascent * scale;

		// Centre each letter in its own third of the screen width.
		float wB = fontTexture.getWidth("B");
		float wM = fontTexture.getWidth("M");
		float wT = fontTexture.getWidth("T");
		float thirdW = win.width / 3.0f;
		float xB = thirdW * 0.5f - (wB * scale) / 2.0f;
		float xM = thirdW * 1.5f - (wM * scale) / 2.0f;
		float xT = thirdW * 2.5f - (wT * scale) / 2.0f;

		bassLabel.setTransform(  new Matrix4f().translate(xB, baseline, 0).scale(scale, scale, 1));
		midLabel.setTransform(   new Matrix4f().translate(xM, baseline, 0).scale(scale, scale, 1));
		trebleLabel.setTransform(new Matrix4f().translate(xT, baseline, 0).scale(scale, scale, 1));
	}

	@Override
	public void doRender(RenderContext ctx) {
		freqProc.process();
		displayProc.process();
		analyser.doRender(ctx);

		bassLabel.setTextColor(  StandardColors.WHITE.withAlpha(letterAlpha(beats.getBeatStrength(0))));
		midLabel.setTextColor(   StandardColors.WHITE.withAlpha(letterAlpha(beats.getBeatStrength(1))));
		trebleLabel.setTextColor(StandardColors.WHITE.withAlpha(letterAlpha(beats.getBeatStrength(2))));

		bassLabel.doRender(ctx);
		midLabel.doRender(ctx);
		trebleLabel.doRender(ctx);
	}

	/** Maps beat strength [0,1] to alpha [LETTER_DIM_ALPHA, 1]. */
	private static float letterAlpha(float beatStrength) {
		return LETTER_DIM_ALPHA + beatStrength * (1.0f - LETTER_DIM_ALPHA);
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
		analyser.dispose();
		bassLabel.dispose();
		midLabel.dispose();
		trebleLabel.dispose();
		fontTexture.dispose();
	}
}
