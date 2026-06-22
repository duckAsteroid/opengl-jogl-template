package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.audio.PboAudioSink;
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
import java.util.Objects;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

/**
 * Real-time stereo audio waveform visualiser in a radial (polar) layout.
 *
 * <p>Reads from a shared {@link PboAudioSink}: the sink's audio texture is sampled in the vertex
 * shader to displace {@value #SAMPLE_COUNT} vertices arranged around a circle radially. The caller
 * must invoke {@link PboAudioSink#upload()} exactly once per frame before calling
 * {@link #doRender}, so that multiple visualisers sharing the same sink see identical data.</p>
 *
 * <h2>Buffer sizing</h2>
 * Pass {@link #AUDIO_BUFFER_SIZE} to {@link PboAudioSink#create}.
 */
public class RadialWave implements RenderedItem {

    /** Number of vertices (and audio samples) around the circle. */
    public static final int SAMPLE_COUNT = 1024;

    /**
     * Required audio ring buffer size in stereo frames.
     * Pass this to {@link PboAudioSink#create}.
     */
    public static final int AUDIO_BUFFER_SIZE = SAMPLE_COUNT * 2;

    private static final VertexElement DIRECTION = new VertexElement(VertexElementType.VEC_2F, "direction");

    /** Visualise the L+R average as a single line. */
    public static final int CHANNEL_BLEND = 0;
    /** Visualise the left channel only. */
    public static final int CHANNEL_LEFT  = 1;
    /** Visualise the right channel only. */
    public static final int CHANNEL_RIGHT = 2;

    private final PboAudioSink audioSink;

    private ShaderProgram shader;
    private VertexArrayObject vao;

    private Uniform<Integer> uHead;
    private Uniform<Integer> uChannel;
    private Uniform<Float>   uRadius;
    private Uniform<Float>   uAmplitude;
    private Uniform<Float>   uAspect;
    private Uniform<Vector2f> uCenter;
    private Uniform<Vector4f> uColour;

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
            in vec2 direction;
            uniform sampler1D uAudioTex;
            uniform int uHead;
            uniform int uChannel;
            uniform vec2 uCenter;
            uniform float uRadius;
            uniform float uAmplitude;
            uniform float uAspect;

            void main() {
                int sampleIndex = (2048 + uHead + gl_VertexID) % 2048;
                vec2 stereo = texelFetch(uAudioTex, sampleIndex, 0).rg;
                float sample = (uChannel == 0) ? (stereo.r + stereo.g) * 0.5
                             : (uChannel == 1) ? stereo.r : stereo.g;
                float r = uRadius + sample * uAmplitude;
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

    public RadialWave(PboAudioSink audioSink) {
        this.audioSink = Objects.requireNonNull(audioSink);
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        initVbo(ctx);
        initShader(ctx);

        Rectangle win = ctx.getWindow();
        currentAspect = (float) win.width / win.height;
        ctx.addResizeListener((w, h) -> currentAspect = (float) w / h);

        glLineWidth(3.0f);
        ctx.setDesiredUpdateFrequency(60.0);
    }

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
        glBindTexture(GL_TEXTURE_1D, audioSink.getTextureId());
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

    @Override
    public void doRender(RenderContext ctx) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use(ctx);
        renderActions.processAll(ctx);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_1D, audioSink.getTextureId());
        uHead.set(audioSink.getHead());
        uAspect.set(currentAspect);
        vao.bind(ctx);
        vao.doRender(ctx);
    }

    @Override
    public void dispose() {
        vao.dispose();
        shader.dispose();
    }

    public void setLineWidth(float v) {
        renderActions.enqueue(ACTION_LINE_WIDTH, ctx -> glLineWidth(v));
    }

    public void setLineColour(Vector4f colour) {
        Vector4f copy = new Vector4f(colour);
        renderActions.enqueue(ACTION_LINE_COLOUR, ctx -> uColour.set(copy));
    }

    public void setChannelMode(int mode) {
        renderActions.enqueue(ACTION_CHANNEL_MODE, ctx -> uChannel.set(mode));
    }

    public void setRadius(float r) {
        renderActions.enqueue(ACTION_RADIUS, ctx -> uRadius.set(r));
    }

    public void setAmplitude(float a) {
        renderActions.enqueue(ACTION_AMPLITUDE, ctx -> uAmplitude.set(a));
    }

    public void setCenter(Vector2f center) {
        Vector2f copy = new Vector2f(center);
        renderActions.enqueue(ACTION_CENTER, ctx -> uCenter.set(copy));
    }
}
