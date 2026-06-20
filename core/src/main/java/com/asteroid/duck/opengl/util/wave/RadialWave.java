package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.renderaction.RenderActionQueue;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.UpdateHint;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.shader.Uniform;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL44.*;

/**
 * Real-time stereo audio waveform visualiser in a radial (polar) layout.
 *
 * <p>Uses the same audio capture pipeline as {@link AudioWave}: a persistently-mapped PBO feeds
 * a 1-D {@code RG16_SNORM} texture read by the vertex shader. Instead of a horizontal line strip,
 * 1 024 vertices are arranged around a circle; each vertex is displaced radially by the
 * corresponding audio sample — outward for positive amplitudes, inward for negative.</p>
 *
 * <p>The {@code uAspect} uniform (window width / height) ensures the base circle appears round
 * on non-square displays. It is updated each frame from a volatile field written by a resize
 * listener, avoiding uniform calls outside the render thread.</p>
 */
public class RadialWave implements RenderedItem {

    /** Number of vertices (and audio samples) around the circle. */
    private static final int SAMPLE_COUNT = 1024;

    /**
     * Circular audio buffer in stereo sample-pairs — double {@link #SAMPLE_COUNT} so the
     * write head can lap the read head without a visible glitch.
     */
    private static final int AUDIO_BUFFER_SIZE = SAMPLE_COUNT * 2;

    /**
     * Byte size of the PBO and 1-D audio texture: each texel is an RG16_SNORM stereo pair
     * (2 signed shorts = 4 bytes).
     */
    private static final int AUDIO_TEXTURE_BYTE_SIZE = AUDIO_BUFFER_SIZE * 2 * 2;

    private static final VertexElement DIRECTION = new VertexElement(VertexElementType.VEC_2F, "direction");

    /** Visualise the L+R average as a single line. */
    public static final int CHANNEL_BLEND = 0;
    /** Visualise the left channel only. */
    public static final int CHANNEL_LEFT  = 1;
    /** Visualise the right channel only. */
    public static final int CHANNEL_RIGHT = 2;

    private ShaderProgram shader;
    private VertexArrayObject vao;

    /** OpenGL handle for the 1-D {@code RG16_SNORM} audio texture. */
    private int audioTextureId;

    /** Pixel Buffer Object used for zero-copy audio-to-texture transfer. */
    private int pboId;

    private AudioReader audioReader;
    private Thread audioReaderThread;

    private Uniform<Integer> uHead;
    private Uniform<Integer> uChannel;
    private Uniform<Float>   uRadius;
    private Uniform<Float>   uAmplitude;
    private Uniform<Float>   uAspect;
    private Uniform<Vector2f> uCenter;
    private Uniform<Vector4f> uColour;

    /**
     * Current window aspect ratio (width / height). Written by a resize listener on the render
     * thread; read each frame in {@link #doRender} to update {@code uAspect}.
     */
    private volatile float currentAspect = 1.0f;

    private static final String ACTION_LINE_WIDTH   = "lineWidth";
    private static final String ACTION_LINE_COLOUR  = "lineColour";
    private static final String ACTION_CHANNEL_MODE = "channelMode";
    private static final String ACTION_RADIUS       = "radius";
    private static final String ACTION_AMPLITUDE    = "amplitude";
    private static final String ACTION_CENTER       = "center";
    private final RenderActionQueue renderActions = new RenderActionQueue(
            ACTION_LINE_WIDTH, ACTION_LINE_COLOUR, ACTION_CHANNEL_MODE,
            ACTION_RADIUS, ACTION_AMPLITUDE, ACTION_CENTER);

    // language=GLSL
    private static final String VERTEX_SHADER = """
            #version 330 core
            // Unit direction vector (cos θ, sin θ) for this vertex's position on the circle.
            in vec2 direction;
            uniform sampler1D uAudioTex;
            // Current write-head position in the circular buffer.
            uniform int uHead;
            // Channel: 0 = blend, 1 = left, 2 = right.
            uniform int uChannel;
            // Circle centre in NDC.
            uniform vec2 uCenter;
            // Base circle radius in NDC-y units.
            uniform float uRadius;
            // Audio displacement scale (NDC-y units per normalised sample unit).
            uniform float uAmplitude;
            // Window width / height — corrects x so the circle is round in pixel space.
            uniform float uAspect;

            void main() {
                int sampleIndex = (2048 + uHead + gl_VertexID) % 2048;
                vec2 stereo = texelFetch(uAudioTex, sampleIndex, 0).rg;
                float sample = (uChannel == 0) ? (stereo.r + stereo.g) * 0.5
                             : (uChannel == 1) ? stereo.r : stereo.g;

                // Displace radially: positive sample pushes outward, negative pulls inward.
                float r = uRadius + sample * uAmplitude;
                // Compress x by aspect ratio so the circle appears round on screen.
                vec2 pos = uCenter + vec2(direction.x * r / uAspect, direction.y * r);
                gl_Position = vec4(pos, 0.0, 1.0);
            }
        """;

    // language=GLSL
    private static final String FRAGMENT_SHADER = """
            #version 330 core
            uniform vec4 uColour;
            out vec4 fragColor;

            void main() {
                fragColor = uColour;
            }
        """;

    /**
     * Initialises all GL resources and starts the audio capture thread.
     */
    @Override
    public void init(RenderContext ctx) throws IOException {
        ByteBuffer gpuMapped = initAudioBuffer(ctx);
        initVbo(ctx);
        initShader(ctx);

        Rectangle win = ctx.getWindow();
        currentAspect = (float) win.width / win.height;
        ctx.addResizeListener((w, h) -> currentAspect = (float) w / h);

        this.audioReader = new AudioReader(gpuMapped, AUDIO_TEXTURE_BYTE_SIZE);
        this.audioReaderThread = new Thread(audioReader, "radial-audio-reader");
        this.audioReaderThread.setDaemon(true);
        this.audioReaderThread.start();
        glLineWidth(3.0f);
        ctx.setDesiredUpdateFrequency(60.0);
    }

    /**
     * Switches the audio input source at runtime.
     *
     * @param line the new audio data source to capture from
     */
    public void setLine(AudioDataSource line) {
        audioReader.setLine(line);
    }

    /**
     * Builds the VBO: one vertex per sample position around the circle, each storing the unit
     * direction vector {@code (cos θ, sin θ)}. The circle is closed automatically by
     * {@link BufferDrawMode#LINE_LOOP}.
     */
    private void initVbo(RenderContext ctx) {
        this.vao = new VertexArrayObject();
        vao.setDrawMode(BufferDrawMode.LINE_LOOP);
        vao.init(ctx);
        var vbo = vao.createVbo(new VertexDataStructure(DIRECTION), SAMPLE_COUNT);
        vbo.init(ctx);
        IntStream.range(0, SAMPLE_COUNT).forEach(i -> {
            float angle = (float) (2.0 * Math.PI * i / SAMPLE_COUNT);
            vbo.setElement(i, DIRECTION, new Vector2f((float) Math.cos(angle), (float) Math.sin(angle)));
        });
        vbo.update(UpdateHint.STATIC);
    }

    private void initShader(RenderContext ctx) {
        this.shader = ShaderProgram.compile(
                ShaderSource.fromClass(VERTEX_SHADER, RadialWave.class),
                ShaderSource.fromClass(FRAGMENT_SHADER, RadialWave.class),
                null);
        shader.use(ctx);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_1D, audioTextureId);
        shader.uniforms().get("uAudioTex", Integer.class).set(0);
        this.uHead      = shader.uniforms().get("uHead",      Integer.class);
        this.uChannel   = shader.uniforms().get("uChannel",   Integer.class);
        this.uRadius    = shader.uniforms().get("uRadius",    Float.class);
        this.uAmplitude = shader.uniforms().get("uAmplitude", Float.class);
        this.uAspect    = shader.uniforms().get("uAspect",    Float.class);
        this.uCenter    = shader.uniforms().get("uCenter",    Vector2f.class);
        this.uColour    = shader.uniforms().get("uColour",    Vector4f.class);
        uChannel.set(CHANNEL_BLEND);
        uRadius.set(0.5f);
        uAmplitude.set(0.3f);
        uAspect.set(1.0f);
        uCenter.set(new Vector2f(0.0f, 0.0f));
        uColour.set(new Vector4f(1.0f));
        vao.getVbo().setup(shader);
    }

    /**
     * Creates the 1-D audio texture and persistently-mapped PBO — same approach as
     * {@link AudioWave#initAudioBuffer} but sized for {@link #SAMPLE_COUNT} vertices.
     *
     * @return the persistently-mapped {@link ByteBuffer} passed to the audio producer thread
     */
    private ByteBuffer initAudioBuffer(RenderContext ctx) {
        this.audioTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, audioTextureId);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexImage1D(GL_TEXTURE_1D, 0, GL_RG16_SNORM, AUDIO_BUFFER_SIZE, 0, GL_RG, GL_SHORT, (ByteBuffer) null);
        glBindTexture(GL_TEXTURE_1D, 0);

        this.pboId = glGenBuffers();
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
        int flags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
        glBufferStorage(GL_PIXEL_UNPACK_BUFFER, AUDIO_TEXTURE_BYTE_SIZE, flags);
        ByteBuffer mapped = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, AUDIO_TEXTURE_BYTE_SIZE, flags);

        ctx.getResourceManager().register(() -> {
            glDeleteTextures(audioTextureId);
            glDeleteBuffers(pboId);
        });
        return mapped;
    }

    /**
     * Renders one frame: uploads the PBO to the audio texture, updates the write-head and aspect
     * ratio uniforms, then draws the circle as a {@code GL_LINE_LOOP}.
     */
    @Override
    public void doRender(RenderContext ctx) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use(ctx);
        renderActions.processAll(ctx);
        uHead.set(audioReader.getHead());
        uAspect.set(currentAspect);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
        glBindTexture(GL_TEXTURE_1D, audioTextureId);
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        vao.bind(ctx);
        vao.doRender(ctx);
    }

    /**
     * Stops the audio capture thread and releases all GL resources.
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
        vao.dispose();
        shader.dispose();
        glDeleteBuffers(pboId);
        glDeleteTextures(audioTextureId);
        pboId = 0;
        audioTextureId = 0;
    }

    /**
     * Sets the GL line width for the waveform circle.
     * Applied on the render thread on the next frame.
     *
     * @param v line width in pixels
     */
    public void setLineWidth(float v) {
        renderActions.enqueue(ACTION_LINE_WIDTH, ctx -> glLineWidth(v));
    }

    /**
     * Sets the colour of the waveform circle.
     * Applied on the render thread on the next frame.
     *
     * @param colour RGBA colour (each component in [0, 1])
     */
    public void setLineColour(Vector4f colour) {
        Vector4f copy = new Vector4f(colour);
        renderActions.enqueue(ACTION_LINE_COLOUR, ctx -> uColour.set(copy));
    }

    /**
     * Sets the channel display mode.
     * Applied on the render thread on the next frame.
     *
     * @param mode one of {@link #CHANNEL_BLEND}, {@link #CHANNEL_LEFT}, {@link #CHANNEL_RIGHT}
     */
    public void setChannelMode(int mode) {
        renderActions.enqueue(ACTION_CHANNEL_MODE, ctx -> uChannel.set(mode));
    }

    /**
     * Sets the base circle radius in NDC-y units (0 = centre, 1 = screen edge).
     * Applied on the render thread on the next frame.
     *
     * @param r radius in NDC-y units
     */
    public void setRadius(float r) {
        renderActions.enqueue(ACTION_RADIUS, ctx -> uRadius.set(r));
    }

    /**
     * Sets the audio amplitude scale — how far a full-scale sample displaces the circle edge.
     * Applied on the render thread on the next frame.
     *
     * @param a amplitude in NDC-y units per normalised sample unit
     */
    public void setAmplitude(float a) {
        renderActions.enqueue(ACTION_AMPLITUDE, ctx -> uAmplitude.set(a));
    }

    /**
     * Sets the circle centre position in NDC.
     * Applied on the render thread on the next frame.
     *
     * @param center centre point in NDC (default {@code (0, 0)})
     */
    public void setCenter(Vector2f center) {
        Vector2f copy = new Vector2f(center);
        renderActions.enqueue(ACTION_CENTER, ctx -> uCenter.set(copy));
    }
}
