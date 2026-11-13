package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.audio.AudioReader;
import com.asteroid.duck.opengl.util.audio.AudioSources;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.audio.simulated.SimulatedSources;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.text.StringRenderer;
import com.asteroid.duck.opengl.util.audio.analysis.BeatDetector;
import com.asteroid.duck.opengl.util.audio.beat.BeatTrackerSink;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyBand;
import com.asteroid.duck.opengl.util.audio.analysis.FrequencyProcessor;
import com.asteroid.duck.opengl.util.wave.SpectrumAnalyser;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

import static org.lwjgl.opengl.GL11.*;

/**
 * Spectrum analyser experiment: log-frequency bar chart (20 Hz–20 kHz) with retro peak-hold ticks
 * and beat-detection letter flash overlaid on the bars.
 *
 * <p>A single {@link FrequencyProcessor} acts as the shared FFT source, feeding both a
 * {@link SpectrumAnalyser} (bar and tick renderer) and a {@link BeatDetector}. Since
 * {@link BeatDetector} reads {@link FrequencyProcessor}'s raw, un-coarsened FFT bins rather than
 * its display-sized bars, one processor now serves both consumers at full frequency resolution —
 * no need for a second, higher-resolution FFT dedicated to beat detection. On each beat in the
 * mid / treble bands the corresponding letter (M / T) flashes at full brightness over the spectrum
 * bars; between beats each letter stays visible at {@value #LETTER_DIM_ALPHA} alpha.</p>
 *
 * <p>A {@link BeatTrackerSink} on the same audio feed drives the rest: the B letter flashes with
 * each detected kick drum, and a BPM readout at the bottom pulses on each tracked beat, dimmed
 * when the tempo is uncertain.</p>
 *
 * <p>Key bindings: <kbd>J</kbd> / <kbd>H</kbd> step through audio inputs; <kbd>P</kbd> saves a
 * screenshot; <kbd>.</kbd> / <kbd>,</kbd> move the beat pulse 10 ms earlier / later (the offset is
 * shown next to the BPM).</p>
 */
public class SpectrumWave implements Experiment {

	// ── FFT / display parameters ──────────────────────────────────────────────
	private static final int   DISPLAY_BINS = 12;  // bars shown on screen (~1 octave each)
	private static final int   FFT_SIZE     = 4096;
	private static final float SAMPLE_RATE  = 48_000f;
	private static final float F_MIN        = 20f;
	private static final float F_MAX        = 20_000f;
	private static final float DB_FLOOR     = -80f;
	private static final float DB_CEILING   = 0f;
	private static final float GAP          = SpectrumAnalyser.DEFAULT_GAP;

	// ── Colour gradient: green (low) → red (high) ────────────────────────────
	private static final Vector4f COLOR_LOW  = new Vector4f(0.0f, 0.8f, 0.0f, 1.0f);
	private static final Vector4f COLOR_HIGH = new Vector4f(0.8f, 0.0f, 0.0f, 1.0f);

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
	// Single processor: 12 bins for the display bars, plus BeatDetector reads its raw FFT bins
	// directly — full resolution for beat detection at no extra FFT cost.
	private final FrequencyProcessor freqProc =
			new FrequencyProcessor(FFT_SIZE, DISPLAY_BINS, SAMPLE_RATE, F_MIN, F_MAX, DB_FLOOR, DB_CEILING);

	private final SpectrumAnalyser analyser =
			new SpectrumAnalyser(freqProc, GAP).withBarColors(COLOR_LOW, COLOR_HIGH);

	private final BeatDetector beats = new BeatDetector(
			BEAT_BANDS, freqProc.getFftSize(), freqProc.getSampleRate(),
			120,    // ~2 s history — stable baseline across 4/4 kick pattern at 120 BPM
			1.15f,  // lower threshold: sustained bass line means kick only clears ~15–25% above avg
			4.0f,   // sensitivity: 25% spike → full beat strength
			BeatDetector.DEFAULT_NOISE_FLOOR, // ignore ratio spikes during near-silence
			Duration.ofMillis(333) // fast decay — clears in ~0.3 s so next kick can re-trigger
	);

	private final AudioSources audioSources = new AudioSources();
	private int selectedSource = 0;

	/** How far one press of <kbd>.</kbd> / <kbd>,</kbd> moves the beat pulse. */
	private static final Duration LATENCY_STEP = Duration.ofMillis(10);

	/** Tempo, beat phase and kicks, tracked live on the audio thread. */
	private final BeatTrackerSink beatTracker = new BeatTrackerSink(SAMPLE_RATE);

	private AudioReader audioReader;
	private Thread      audioReaderThread;

	// ── Beat letter renderers (shared font texture) ───────────────────────────
	private FontTexture    fontTexture;
	private StringRenderer bassLabel;
	private StringRenderer midLabel;
	private StringRenderer trebleLabel;
	/** BPM readout that pulses on each tracked beat. */
	private StringRenderer tempoLabel;

	@Override
	public String getDescription() {
		return "Traditional bar-chart spectrum analyser — log frequency (20 Hz–20 kHz), dB amplitude, peak hold";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		freqProc.addSink(analyser);
		freqProc.addSink(beats);
		analyser.init(ctx);

		audioReader = new AudioReader(List.of(freqProc, beatTracker));
		audioReaderThread = new Thread(audioReader, "spectrum-audio-reader");
		audioReaderThread.setDaemon(true);
		audioReaderThread.start();

		audioSources.add(SimulatedSources.middleC(ctx.getClock()));
		LineAcquirer.allLinesMatching(LineAcquirer.IDEAL)
				.map(LineAcquirer.MixerLine::toAudioDataSource)
				.forEach(audioSources::add);
		audioReader.setLine(audioSources.list().get(selectedSource));

		initBeatLabels(ctx);

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
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("PERIOD"), () ->
				beatTracker.setLatency(beatTracker.latency().plus(LATENCY_STEP)),
				"Beat pulse earlier (more latency compensation)");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.named("COMMA"), () ->
				beatTracker.setLatency(beatTracker.latency().minus(LATENCY_STEP)),
				"Beat pulse later (less latency compensation)");
	}

	private void initBeatLabels(RenderContext ctx) throws IOException {
		fontTexture = new FontTextureFactory(new Font("Arial", Font.BOLD, FONT_PT), true)
				.createFontTexture();

		bassLabel   = new StringRenderer(fontTexture);
		midLabel    = new StringRenderer(fontTexture);
		trebleLabel = new StringRenderer(fontTexture);
		tempoLabel  = new StringRenderer(fontTexture);

		bassLabel.init(ctx);
		midLabel.init(ctx);
		trebleLabel.init(ctx);
		tempoLabel.init(ctx);
		tempoLabel.setText(tempoText());

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

		// BPM readout: 8% of the screen height, bottom centre (width of a typical "128.0 BPM").
		float tempoScale = win.height * 0.08f / fontH;
		float tempoW = fontTexture.getWidth("128.0 BPM") * tempoScale;
		tempoLabel.setTransform(new Matrix4f()
				.translate((win.width - tempoW) / 2.0f, win.height * 0.96f, 0)
				.scale(tempoScale, tempoScale, 1));
	}

	private String tempoText() {
		double bpm = beatTracker.bpm();
		String tempo = Double.isNaN(bpm) ? "--- BPM" : String.format(Locale.ROOT, "%.1f BPM", bpm);
		long latency = beatTracker.latency().toMillis();
		return latency == 0 ? tempo : String.format(Locale.ROOT, "%s %+dms", tempo, latency);
	}

	@Override
	public void doRender(RenderContext ctx) {
		freqProc.process();
		analyser.doRender(ctx);

		// "B" is the kick drum, from the beat tracker; M and T are BeatDetector bands
		bassLabel.setTextColor(  StandardColors.WHITE.withAlpha(letterAlpha(beatTracker.kickStrength())));
		midLabel.setTextColor(   StandardColors.WHITE.withAlpha(letterAlpha(beats.getBeatStrength(1))));
		trebleLabel.setTextColor(StandardColors.WHITE.withAlpha(letterAlpha(beats.getBeatStrength(2))));

		bassLabel.doRender(ctx);
		midLabel.doRender(ctx);
		trebleLabel.doRender(ctx);

		String tempo = tempoText();
		if (!tempo.equals(tempoLabel.getText())) tempoLabel.setText(tempo);
		float pulse = (float) Math.exp(-6 * beatTracker.beatPhase());
		tempoLabel.setTextColor(StandardColors.WHITE.withAlpha(
				LETTER_DIM_ALPHA + (1 - LETTER_DIM_ALPHA) * pulse * beatTracker.confidence()));
		tempoLabel.doRender(ctx);
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
		tempoLabel.dispose();
		fontTexture.dispose();
	}
}
