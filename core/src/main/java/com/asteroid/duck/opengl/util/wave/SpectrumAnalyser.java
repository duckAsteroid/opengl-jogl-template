package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.audio.RollingFloatBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.UpdateHint;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import org.joml.Vector2f;

import java.io.IOException;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Real-time audio spectrum analyser — classic bar-chart style.
 *
 * <h2>Signal path</h2>
 * <ol>
 *   <li>A background thread ({@link AudioReader}) reads PCM from a {@link AudioDataSource} and
 *       writes it into a {@link RollingFloatBuffer}.</li>
 *   <li>Each frame, {@link #doRender} pulls the latest {@code fftSize} samples, applies a Hann
 *       window, and runs a real FFT via {@link FFTProcessor}.</li>
 *   <li>The {@code numBins} log-frequency magnitude values (in dB, normalised to [0, 1]) are
 *       uploaded to a 1-D {@code GL_RG32F} texture — R = current magnitude, G = peak-hold level.
 *       </li>
 *   <li>Two draw calls read from that texture:
 *       <ul>
 *         <li><b>Bar shader</b> ({@code GL_TRIANGLES}, {@code numBins × 6} vertices) — draws
 *             filled bars with a green → yellow → red gradient based on screen height.</li>
 *         <li><b>Peak shader</b> ({@code GL_LINES}, {@code numBins × 2} vertices) — draws a
 *             white horizontal tick at the peak-hold level for each bar.</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h2>Key parameters (set at construction time)</h2>
 * <ul>
 *   <li>{@code numBins} — number of visual bars (independent of FFT size)</li>
 *   <li>{@code fftSize} — FFT window size in samples; determines frequency resolution</li>
 *   <li>{@code fMin} / {@code fMax} — visible frequency range in Hz</li>
 *   <li>{@code dBFloor} / {@code dBCeiling} — dB range mapped to bar height [0, 1]</li>
 *   <li>{@code gapFraction} — fraction of each bar's slot width used as a gap</li>
 * </ul>
 */
public class SpectrumAnalyser implements RenderedItem {

    // ── Defaults ────────────────────────────────────────────────────────────────

    /** Default number of visual bars ({@value}). */
    public static final int   DEFAULT_NUM_BINS    = 128;

    /** Default FFT window size in samples ({@value}). Must be a power of two. */
    public static final int   DEFAULT_FFT_SIZE    = 2048;

    /** Default capture sample rate in Hz ({@value}). Matches {@link com.asteroid.duck.opengl.util.audio.LineAcquirer#IDEAL}. */
    public static final float DEFAULT_SAMPLE_RATE = 48_000f;

    /** Default lower frequency bound in Hz ({@value}) — bottom of the audible range. */
    public static final float DEFAULT_F_MIN       = 20f;

    /** Default upper frequency bound in Hz ({@value}) — top of the audible range. */
    public static final float DEFAULT_F_MAX       = 20_000f;

    /** Default dB floor: signals at or below this level map to bar height 0 ({@value} dB). */
    public static final float DEFAULT_DB_FLOOR    = -80f;

    /** Default dB ceiling: signals at or above this level map to bar height 1 ({@value} dB). */
    public static final float DEFAULT_DB_CEILING  = 0f;

    /** Default gap fraction between adjacent bars ({@value}): 15% of each slot is empty. */
    public static final float DEFAULT_GAP         = 0.15f;

    /** Peak level falls by this amount per frame (at 60 fps → ~1 second from full to zero). */
    private static final float PEAK_DECAY_PER_FRAME = 1.0f / 60f;

    // ── Vertex attribute shared by both bar and peak VAOs ───────────────────────
    private static final VertexElement VERTEX =
            new VertexElement(VertexElementType.VEC_2F, "vertex");

    // ── Construction-time parameters ────────────────────────────────────────────
    private final int   numBins;
    private final int   fftSize;
    private final float gapFraction;

    // ── Audio pipeline ──────────────────────────────────────────────────────────
    private final FFTProcessor     fftProcessor;
    private final RollingFloatBuffer audioBuffer;
    private AudioReader audioReader;
    private Thread      audioReaderThread;

    // ── GL resources ────────────────────────────────────────────────────────────
    /** 1-D GL_RG32F texture: R = magnitude, G = peak level (both in [0, 1]). */
    private int         fftTextureId;
    /** Direct float buffer used for texture upload each frame; length = numBins * 2. */
    private FloatBuffer fftUploadBuffer;
    private ShaderProgram barShader;
    private ShaderProgram peakShader;
    private VertexArrayObject barVao;
    private VertexArrayObject peakVao;

    // ── Per-frame working arrays (pre-allocated) ────────────────────────────────
    private final float[] sampleBuffer;
    private final float[] magnitudes;
    private final float[] peaks;

    // ── Shaders ─────────────────────────────────────────────────────────────────

    // language=GLSL
    private static final String VERTEX_BAR = """
            #version 330 core
            // x = NDC x position; y = role (0 = bar bottom, 1 = bar top)
            in vec2 vertex;
            uniform sampler1D uFFTTex;   // GL_RG32F: R = magnitude, G = peak

            out float vNdcY;

            void main() {
                int barIndex = gl_VertexID / 6;
                float magnitude = texelFetch(uFFTTex, barIndex, 0).r;
                // Bottom vertex stays at -1; top vertex is displaced by magnitude.
                float y = (vertex.y > 0.5) ? magnitude * 2.0 - 1.0 : -1.0;
                vNdcY = y;
                gl_Position = vec4(vertex.x, y, 0.0, 1.0);
            }
        """;

    // language=GLSL
    private static final String FRAGMENT_BAR = """
            #version 330 core
            in float vNdcY;
            out vec4 fragColor;

            void main() {
                // Classic spectrum analyser gradient: green → yellow → red with screen height.
                float t = (vNdcY + 1.0) * 0.5;   // NDC [-1,1] → [0,1]
                vec3 color = (t < 0.5)
                    ? mix(vec3(0.0, 0.8, 0.0), vec3(0.8, 0.8, 0.0), t * 2.0)
                    : mix(vec3(0.8, 0.8, 0.0), vec3(0.8, 0.0, 0.0), (t - 0.5) * 2.0);
                fragColor = vec4(color, 1.0);
            }
        """;

    // language=GLSL
    private static final String VERTEX_PEAK = """
            #version 330 core
            // x = NDC x position; y is unused (peak y is read from texture)
            in vec2 vertex;
            uniform sampler1D uFFTTex;

            void main() {
                int barIndex = gl_VertexID / 2;
                float peakLevel = texelFetch(uFFTTex, barIndex, 0).g;
                float y = peakLevel * 2.0 - 1.0;
                gl_Position = vec4(vertex.x, y, 0.0, 1.0);
            }
        """;

    // language=GLSL
    private static final String FRAGMENT_PEAK = """
            #version 330 core
            out vec4 fragColor;
            void main() {
                fragColor = vec4(1.0);   // white peak ticks
            }
        """;

    // ── Constructors ─────────────────────────────────────────────────────────────

    /** Default constructor — 128 bars, 2048-point FFT, 20 Hz–20 kHz, −80 to 0 dB, 48 kHz. */
    public SpectrumAnalyser() {
        this(DEFAULT_NUM_BINS, DEFAULT_FFT_SIZE, DEFAULT_SAMPLE_RATE,
             DEFAULT_F_MIN, DEFAULT_F_MAX, DEFAULT_DB_FLOOR, DEFAULT_DB_CEILING, DEFAULT_GAP);
    }

    /**
     * Full constructor.
     *
     * @param numBins     number of visual bars
     * @param fftSize     FFT window size (power of two; determines frequency resolution)
     * @param sampleRate  capture sample rate in Hz
     * @param fMin        lowest displayed frequency in Hz
     * @param fMax        highest displayed frequency in Hz
     * @param dBFloor     dB level that maps to bar height 0
     * @param dBCeiling   dB level that maps to bar height 1
     * @param gapFraction fraction [0, 1) of each bar slot used as a gap between bars
     */
    public SpectrumAnalyser(int numBins, int fftSize, float sampleRate,
                            float fMin, float fMax, float dBFloor, float dBCeiling,
                            float gapFraction) {
        this.numBins     = numBins;
        this.fftSize     = fftSize;
        this.gapFraction = gapFraction;
        this.fftProcessor = new FFTProcessor(fftSize, numBins, sampleRate,
                                             fMin, fMax, dBFloor, dBCeiling);
        // Ring buffer holds 4× the FFT window so the write head never laps the read head.
        this.audioBuffer  = new RollingFloatBuffer(fftSize * 4);
        this.sampleBuffer = new float[fftSize];
        this.magnitudes   = new float[numBins];
        this.peaks        = new float[numBins];
    }

    // ── RenderedItem lifecycle ───────────────────────────────────────────────────

    @Override
    public void init(RenderContext ctx) throws IOException {
        initFftTexture(ctx);
        initVbos(ctx);
        initShaders(ctx);

        this.audioReader = new AudioReader(null, 0);
        audioReader.setCpuBuffer(audioBuffer);
        this.audioReaderThread = new Thread(audioReader, "spectrum-audio-reader");
        audioReaderThread.setDaemon(true);
        audioReaderThread.start();

        glLineWidth(2.0f);
        ctx.setDesiredUpdateFrequency(60.0);
    }

    /**
     * Sets the audio input source.  Safe to call from any thread.
     *
     * @param line audio data source to capture from
     */
    public void setLine(AudioDataSource line) {
        audioReader.setLine(line);
    }

    /**
     * Render one frame of the spectrum analyser.
     *
     * <p>Each call: reads the latest {@code fftSize} samples from the CPU ring buffer, runs the
     * FFT pipeline, updates peak-hold values, uploads the result to the 1-D texture, then issues
     * two draw calls — bars ({@code GL_TRIANGLES}) followed by peak ticks ({@code GL_LINES}).</p>
     *
     * <p>Must be called on the GL/render thread.</p>
     */
    @Override
    public void doRender(RenderContext ctx) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 1. Compute FFT from the latest audio samples.
        audioBuffer.readSamples(sampleBuffer, fftSize);
        fftProcessor.process(sampleBuffer, magnitudes);

        // 2. Update peak-hold: rise instantly, fall at PEAK_DECAY_PER_FRAME per frame.
        for (int i = 0; i < numBins; i++) {
            peaks[i] = Math.max(magnitudes[i], peaks[i] - PEAK_DECAY_PER_FRAME);
        }

        // 3. Upload combined magnitude + peak data to the 1-D texture (R=mag, G=peak).
        fftUploadBuffer.clear();
        for (int i = 0; i < numBins; i++) {
            fftUploadBuffer.put(magnitudes[i]);
            fftUploadBuffer.put(peaks[i]);
        }
        fftUploadBuffer.flip();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_1D, fftTextureId);
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, numBins, GL_RG, GL_FLOAT, fftUploadBuffer);

        // 4. Draw filled bars.
        barShader.use(ctx);
        barVao.doRender(ctx);

        // 5. Draw peak-hold ticks (texture still bound to unit 0).
        peakShader.use(ctx);
        peakVao.doRender(ctx);
    }

    /**
     * Stop the audio reader thread and release all GL and native resources.
     *
     * <p>Joins the audio thread with a 2-second timeout, then disposes both VAOs, both shader
     * programs, the native {@code fftUploadBuffer} ({@link org.lwjgl.system.MemoryUtil#memFree}),
     * and the 1-D FFT texture. Safe to call from the GL thread at any time after {@link #init}.</p>
     */
    @Override
    public void dispose() {
        audioReader.setRunning(false);
        audioReader.setLine(null);
        try {
            if (audioReaderThread != null) {
                audioReaderThread.join(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        barVao.dispose();
        peakVao.dispose();
        barShader.dispose();
        peakShader.dispose();
        if (fftUploadBuffer != null) {
            memFree(fftUploadBuffer);
            fftUploadBuffer = null;
        }
        glDeleteTextures(fftTextureId);
        fftTextureId = 0;
    }

    // ── Initialisation helpers ───────────────────────────────────────────────────

    private void initFftTexture(RenderContext ctx) {
        fftTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, fftTextureId);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        // NEAREST: no interpolation between bars — each texel is exactly one bar.
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // GL_RG32F: R = magnitude, G = peak (both written as GL_FLOAT)
        glTexImage1D(GL_TEXTURE_1D, 0, GL_RG32F, numBins, 0, GL_RG, GL_FLOAT, (FloatBuffer) null);
        glBindTexture(GL_TEXTURE_1D, 0);

        // Pre-allocate the upload buffer once; freed in dispose().
        fftUploadBuffer = memAllocFloat(numBins * 2);

        ctx.getResourceManager().register(() -> {
            glDeleteTextures(fftTextureId);
            if (fftUploadBuffer != null) memFree(fftUploadBuffer);
        });
    }

    /**
     * Builds the bar VBO ({@code numBins × 6} vertices, {@code GL_TRIANGLES}) and the peak VBO
     * ({@code numBins × 2} vertices, {@code GL_LINES}).
     *
     * <p>Both use the same {@code vec2 vertex} layout: {@code vertex.x} is the NDC x coordinate
     * of the left or right edge of the bar; {@code vertex.y} is a role flag (0 = bottom, 1 = top)
     * used by the bar shader to choose between y = −1 (bottom) and y from the FFT texture (top).
     * The peak shader ignores {@code vertex.y} entirely.</p>
     */
    private void initVbos(RenderContext ctx) {
        float slotWidth = 2.0f / numBins;          // NDC width per bar slot
        float barWidth  = slotWidth * (1.0f - gapFraction);
        float halfGap   = slotWidth * gapFraction / 2.0f;

        // ── Bar VAO (GL_TRIANGLES, 6 vertices per bar) ──────────────────────────
        barVao = new VertexArrayObject();
        barVao.setDrawMode(BufferDrawMode.TRIANGLES);
        barVao.init(ctx);
        var barVbo = barVao.createVbo(new VertexDataStructure(VERTEX), numBins * 6);
        barVbo.init(ctx);
        for (int i = 0; i < numBins; i++) {
            float xL = -1.0f + i * slotWidth + halfGap;
            float xR = xL + barWidth;
            int base = i * 6;
            barVbo.setElement(base + 0, VERTEX, new Vector2f(xL, 0.0f)); // BL
            barVbo.setElement(base + 1, VERTEX, new Vector2f(xR, 0.0f)); // BR
            barVbo.setElement(base + 2, VERTEX, new Vector2f(xL, 1.0f)); // TL
            barVbo.setElement(base + 3, VERTEX, new Vector2f(xR, 0.0f)); // BR
            barVbo.setElement(base + 4, VERTEX, new Vector2f(xR, 1.0f)); // TR
            barVbo.setElement(base + 5, VERTEX, new Vector2f(xL, 1.0f)); // TL
        }
        barVbo.update(UpdateHint.STATIC);

        // ── Peak VAO (GL_LINES, 2 vertices per bar) ─────────────────────────────
        peakVao = new VertexArrayObject();
        peakVao.setDrawMode(BufferDrawMode.LINES);
        peakVao.init(ctx);
        var peakVbo = peakVao.createVbo(new VertexDataStructure(VERTEX), numBins * 2);
        peakVbo.init(ctx);
        for (int i = 0; i < numBins; i++) {
            float xL = -1.0f + i * slotWidth + halfGap;
            float xR = xL + barWidth;
            peakVbo.setElement(i * 2,     VERTEX, new Vector2f(xL, 0.0f));
            peakVbo.setElement(i * 2 + 1, VERTEX, new Vector2f(xR, 0.0f));
        }
        peakVbo.update(UpdateHint.STATIC);
    }

    private void initShaders(RenderContext ctx) {
        // ── Bar shader ───────────────────────────────────────────────────────────
        barShader = ShaderProgram.compile(
                ShaderSource.fromClass(VERTEX_BAR,   SpectrumAnalyser.class),
                ShaderSource.fromClass(FRAGMENT_BAR, SpectrumAnalyser.class),
                null);
        barShader.use(ctx);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_1D, fftTextureId);
        barShader.uniforms().get("uFFTTex", Integer.class).set(0);
        barVao.bind(ctx);
        barVao.getVbo().update(UpdateHint.STATIC); // re-bind barVbo to GL_ARRAY_BUFFER before setup
        barVao.getVbo().setup(barShader);

        // ── Peak shader ──────────────────────────────────────────────────────────
        peakShader = ShaderProgram.compile(
                ShaderSource.fromClass(VERTEX_PEAK,   SpectrumAnalyser.class),
                ShaderSource.fromClass(FRAGMENT_PEAK, SpectrumAnalyser.class),
                null);
        peakShader.use(ctx);
        peakShader.uniforms().get("uFFTTex", Integer.class).set(0);
        peakVao.bind(ctx);
        peakVao.getVbo().update(UpdateHint.STATIC); // re-bind peakVbo to GL_ARRAY_BUFFER before setup
        peakVao.getVbo().setup(peakShader);
    }
}
