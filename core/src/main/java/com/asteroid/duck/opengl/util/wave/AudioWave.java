package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
import com.asteroid.duck.opengl.util.renderaction.RenderActionQueue;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.UpdateHint;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.shader.Uniform;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_SHORT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage1D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage1D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30.GL_RG;
import static org.lwjgl.opengl.GL30.glMapBufferRange;
import static org.lwjgl.opengl.GL31.GL_RG16_SNORM;
import static org.lwjgl.opengl.GL44.*;

/**
 * Real-time stereo audio waveform visualiser.
 *
 * <p>Audio samples are captured on a background producer thread ({@link AudioReader}) and written
 * into a persistently-mapped {@link org.lwjgl.opengl.GL44#GL_MAP_PERSISTENT_BIT PBO}. Each frame
 * that PBO is uploaded to a 1-D GL texture via {@code glTexSubImage1D}, and the vertex shader reads
 * individual samples from that texture using {@code texelFetch} to displace a horizontal strip of
 * 1 024 vertices vertically, producing a scrolling waveform.
 *
 * <h2>Rendering pipeline</h2>
 * <ol>
 *   <li>A background thread fills the PBO with stereo 16-bit PCM samples in a circular buffer.</li>
 *   <li>{@link #doRender} copies the PBO into the 1-D texture and passes the current write-head
 *       position to the vertex shader as {@code uHead}.</li>
 *   <li>The vertex shader reads each sample at index {@code (2048 + uHead + gl_VertexID) % 2048},
 *       blending left/right channels according to {@code uChannel}, and uses the amplitude to set
 *       the vertex's Y coordinate.</li>
 *   <li>The fragment shader outputs a solid white colour; line width and colour can be adjusted
 *       via {@link #setLineWidth} and {@link #setLineColour}.</li>
 * </ol>
 *
 * <h2>Thread safety</h2>
 * The PBO is mapped with {@code GL_MAP_COHERENT_BIT}, so writes from the audio thread are visible
 * to the GPU without an explicit flush. The write-head position is read with
 * {@link AudioReader#getHead()}, which must be atomic or volatile on the producer side.
 */
public class AudioWave implements RenderedItem {
    /** Number of vertices drawn per frame — one per horizontal pixel at the target resolution. */
    private static final int SCREEN_WIDTH = 1024;

    /**
     * Circular audio buffer size in stereo sample-pairs.
     * Double the screen width so the write head can lap the read head without a visible glitch.
     */
    private static final int AUDIO_BUFFER_SIZE = SCREEN_WIDTH * 2;

    /**
     * Total number of 16-bit values in the 1-D texture (left + right channel per sample-pair).
     * The texture format is {@code RG16_SNORM}: R = left channel, G = right channel.
     */
    private static final int AUDIO_TEXTURE_WIDTH = (AUDIO_BUFFER_SIZE * 2);

    /** Byte size of the PBO and audio texture — two bytes per 16-bit sample value. */
    private static final int AUDIO_TEXTURE_BYTE_SIZE = AUDIO_TEXTURE_WIDTH * 2;

    private static final VertexElement POSITION = new VertexElement(VertexElementType.VEC_2F, "position");

    /** Default amplitude: 10× exaggeration of the normalised audio signal. */
    private AmplitudeFunction amplitudeFunction = AmplitudeFunction.constant(10f);
    private volatile boolean amplitudeDirty = false;

    private ShaderProgram shader;
    private VertexArrayObject vao;

    /** OpenGL handle for the 1-D {@code RG16_SNORM} audio texture. */
    private int audioTextureId;

    /** Pixel Buffer Object used for zero-copy audio-to-texture transfer. */
    private int pboId;

    /** Visualise the L+R average as a single centred line. */
    public static final int CHANNEL_BLEND  = 0;
    /** Visualise the left channel only as a single centred line. */
    public static final int CHANNEL_LEFT   = 1;
    /** Visualise the right channel only as a single centred line. */
    public static final int CHANNEL_RIGHT  = 2;
    /** Visualise both channels simultaneously — left above centre, right below. */
    public static final int CHANNEL_STEREO = 3;

    /** Cached handle to the {@code uHead} uniform — updated every frame with the write-head position. */
    private Uniform<Integer> uHead;

    /** Cached handle to the {@code uChannel} uniform — selects which audio channel the shader reads. */
    private Uniform<Integer> uChannel;

    /** Cached handle to the {@code uYOffset} uniform — vertically offsets a line in stereo mode. */
    private Uniform<Float> uYOffset;

    /** Cached handle to the {@code uColour} uniform — set via {@link #setLineColour}. */
    private Uniform<Vector4f> uColour;

    private static final String ACTION_LINE_WIDTH   = "lineWidth";
    private static final String ACTION_LINE_COLOUR  = "lineColour";
    private static final String ACTION_CHANNEL_MODE = "channelMode";
    private final RenderActionQueue renderActions = new RenderActionQueue(ACTION_LINE_WIDTH, ACTION_LINE_COLOUR, ACTION_CHANNEL_MODE);

    /** Current channel display mode — one of the {@code CHANNEL_*} constants. */
    private int channelMode = CHANNEL_BLEND;

    /** Background thread that reads from the audio line and fills the PBO. */
    private AudioReader audioReader;
    private Thread audioReaderThread;


    /**
     * Initialises all GL resources and starts the audio capture thread.
     *
     * <p>Call order: audio buffer (PBO + texture) → VBO → shader. The shader setup references the
     * audio texture, so the texture must exist before the shader is compiled and bound.</p>
     */
    @Override
    public void init(RenderContext ctx) throws IOException {
        // an area of memory shared between the Producer thread (filling it with audio data)
        // and the GPU (rendering it)
        ByteBuffer gpuMapped = initAudioBuffer(ctx);
        // a VBO to draw the lines
        initVbo(ctx);
        // a shader that takes the audio data from the texture and multiplies it with the y coordinate of
        // the vertices to create a wave effect
        initShader(ctx);

        // Start a Producer thread to continuously fill the GPU buffer with audio data
        this.audioReader = new AudioReader(gpuMapped, AUDIO_TEXTURE_BYTE_SIZE);
        this.audioReaderThread = new Thread(audioReader, "audio-reader");
        this.audioReaderThread.setDaemon(true);
        this.audioReaderThread.start();
        glLineWidth(6.0f);
        ctx.setDesiredUpdateFrequency(60.0);
    }

    /**
     * Switches the audio input source at runtime.
     *
     * @param line the new audio data source to capture from
     */
    public void setLine(AudioDataSource line) {
        System.out.println("Setting audio line to: " + line.getName());
        audioReader.setLine(line);
    }

    /**
     * Our VBO contains 1024 vertices, each with a 2D position.
     * The x coordinate is fixed and goes from 0 to 1 across the screen,
     * while the y coordinate (amplitude) is set by {@link #amplitudeFunction} and multiplied
     * with audio data in the shader.
     */
    private void initVbo(RenderContext ctx) {
        this.vao = new VertexArrayObject();
        vao.setDrawMode(BufferDrawMode.LINE_STRIP);
        vao.init(ctx);
        VertexDataStructure dataStructure = new VertexDataStructure(POSITION);
        var vbo = vao.createVbo(dataStructure, SCREEN_WIDTH);
        vbo.init(ctx);
        fillVboAmplitude(vbo);
        vbo.update(UpdateHint.STATIC);
    }

    private void fillVboAmplitude(VertexBufferObject vbo) {
        AmplitudeFunction fn = this.amplitudeFunction;
        IntStream.range(0, SCREEN_WIDTH).forEach(i -> {
            float x = (((float) i / SCREEN_WIDTH) * 2f) - 1f;
            vbo.setElement(i, POSITION, new Vector2f(x, fn.amplitudeAt(i, x)));
        });
    }

    private void rebuildVboAmplitude() {
        fillVboAmplitude(vao.getVbo());
        vao.getVbo().update(UpdateHint.DYNAMIC);
        amplitudeDirty = false;
    }

    /**
     * Set how amplitude varies across the wave.
     * The new function takes effect on the next rendered frame.
     *
     * @param fn per-vertex amplitude function; must not be null
     * @see AmplitudeFunction#constant(float)
     * @see AmplitudeFunction#ellipse(float)
     */
    public void setAmplitudeFunction(AmplitudeFunction fn) {
        this.amplitudeFunction = Objects.requireNonNull(fn);
        this.amplitudeDirty = true;
    }

    /** Return the current amplitude function. */
    public AmplitudeFunction getAmplitudeFunction() {
        return amplitudeFunction;
    }

    // language=GLSL
    private static final String VERTEX_SHADER = """
		#version 330 core
		// Static 0.0 to 1.0 from VBO
		in vec2 position;
		// The audio texture (2048 samples, but we only use 1024 at a time)
		uniform sampler1D uAudioTex;
		// current write head position in the circular buffer
		uniform int uHead;
		// which channel to visualize: 0 = blend, 1 = left, 2 = right
		uniform int uChannel;
		// vertical offset in NDC — used to separate lines in stereo mode
		uniform float uYOffset;

		void main() {
			// work out which sample to read from the texture for this vertex.
			int sampleIndex = (2048 + uHead + gl_VertexID) % 2048;

			// Fetch the audio data.
			vec2 stereo = texelFetch(uAudioTex, sampleIndex, 0).rg;
			// now convert into an amplitude
			float amplitude = position.y *
				((uChannel == 0) ? (stereo.r + stereo.g) * 0.5 :
					(uChannel == 1) ? stereo.r : stereo.g);

			// Apply vertical offset (non-zero only in stereo mode)
			gl_Position = vec4(position.x, amplitude + uYOffset, 0.0, 1.0);
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
     * Compiles the waveform shader, binds the audio texture to unit 0, and wires up the VAO
     * vertex attribute pointers.
     */
    private void initShader(RenderContext ctx) {
        this.shader = ShaderProgram.compile(
                ShaderSource.fromClass(VERTEX_SHADER, AudioWave.class),
                ShaderSource.fromClass(FRAGMENT_SHADER, AudioWave.class),
                null); // no geometry shader
        shader.use(ctx);
        // 1. Activate Texture Unit 0 and bind our audio texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_1D, audioTextureId);

        // 2. Tell the 'uAudioTex' sampler in the shader to use Unit 0
        shader.uniforms().get("uAudioTex", Integer.class).set(0);
        this.uHead    = shader.uniforms().get("uHead",    Integer.class);
        this.uChannel = shader.uniforms().get("uChannel", Integer.class);
        this.uYOffset = shader.uniforms().get("uYOffset", Float.class);
        this.uColour  = shader.uniforms().get("uColour",  Vector4f.class);
        uChannel.set(CHANNEL_BLEND);
        uYOffset.set(0.0f);
        uColour.set(new Vector4f(1.0f)); // default: white

        // setup the VAO
        vao.getVbo().setup(shader);
    }

    /**
     * Creates the 1-D audio texture and a persistently-mapped PBO that backs it.
     *
     * <p>The PBO is allocated with {@code GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT |
     * GL_MAP_COHERENT_BIT} so the audio thread can write into it continuously without
     * unmapping or explicit flushes. Each call to {@link #doRender} then issues a single
     * {@code glTexSubImage1D} to copy the latest data from the PBO into the texture.</p>
     *
     * @return the persistently-mapped {@link ByteBuffer} shared with the audio producer thread
     */
    private ByteBuffer initAudioBuffer(RenderContext ctx) {
        // NOTE: ResourceManager does not expose a register(Resource) method on its interface —
        // it only provides named accessors for textures, shaders, and texture units. There is
        // therefore no way to enroll the raw audioTextureId and pboId into lifecycle tracking
        // here. Explicit cleanup via glDeleteTextures/glDeleteBuffers in dispose() is handled
        // separately (see issue #5 / PR #20).
        this.audioTextureId = glGenTextures();
        // Raw bind: GL_TEXTURE_1D is not yet covered by an ExclusivityGroup in this project.
        glBindTexture(GL_TEXTURE_1D, audioTextureId);

        // 1. Set Wrapping to REPEAT so the uHead offset wraps automatically
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);

        // 2. Set Filtering (LINEAR for smooth waves, NEAREST for raw steps)
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // 3. Initialize the storage for
        // Format: Internal Format (RG16_SNORM), Width (2048), Format (RED+GREEN), Type (SHORT)
        glTexImage1D(GL_TEXTURE_1D, 0, GL_RG16_SNORM, AUDIO_BUFFER_SIZE, 0, GL_RG, GL_SHORT, (ByteBuffer)null);

        // Unbind the texture after setup; the PBO stays bound until first render.
        glBindTexture(GL_TEXTURE_1D, 0);

        // The PBO is created with GL_MAP_PERSISTENT_BIT: it must remain mapped for the lifetime
        // of the AudioWave so the audio thread can write to it continuously. This precludes wrapping
        // it in a VertexBufferObject or ExclusivityGroup — both of which may unmap or rebind. Raw
        // GL calls are intentional here. Lifecycle cleanup is handled explicitly in dispose().

        // Raw bind: the PBO uses GL44 persistent mapping (GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT),
        // which requires glBufferStorage and a single persistent map for the audio producer thread.
        // This doesn't fit the existing VertexBufferObject abstraction, so we manage it directly.
        this.pboId = glGenBuffers();
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);

        // 2. Allocate the buffer with Persistent Mapping flags
        int flags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
        glBufferStorage(GL_PIXEL_UNPACK_BUFFER, AUDIO_TEXTURE_BYTE_SIZE, flags);

        // 3. Map it once and keep the reference
        ByteBuffer mapped = glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, AUDIO_TEXTURE_BYTE_SIZE, flags);

        // Register both raw GL resources with the ResourceManager so they are lifecycle-tracked
        // and cleaned up on shutdown even if dispose() is not called directly.
        ctx.getResourceManager().register(() -> {
            glDeleteTextures(audioTextureId);
            glDeleteBuffers(pboId);
        });

        return mapped;
    }

    /**
     * Renders one frame of the waveform.
     *
     * <p>Each frame this method:
     * <ol>
     *   <li>Clears the colour and depth buffers.</li>
     *   <li>Pushes the current write-head position to the {@code uHead} uniform so the vertex
     *       shader knows where in the circular buffer the most recent samples are.</li>
     *   <li>Uploads the PBO contents to the 1-D audio texture via {@code glTexSubImage1D}.</li>
     *   <li>Draws the 1 024-vertex line strip, with each vertex displaced vertically by the
     *       corresponding audio sample amplitude.</li>
     * </ol>
     */
    @Override
    public void doRender(RenderContext ctx) {
        if (amplitudeDirty) {
            rebuildVboAmplitude();
        }
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use(ctx);
        renderActions.processAll(ctx);
        uHead.set(audioReader.getHead()); // pass the current head position to the shader

        // Raw binds: the PBO and 1-D texture are managed outside ExclusivityGroup because the PBO
        // is persistently mapped (see initAudioBuffer). Always unbind the PBO after use (below) to
        // avoid corrupting subsequent glTexImage/glBufferData calls elsewhere.

        // transfer audio data from the PBO to the Texture on the GPU.
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
        glBindTexture(GL_TEXTURE_1D, audioTextureId);

        // This is the "Actual" Flush.
        // It tells the GPU to copy the PBO memory into the Texture memory.
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        // render the waveform — one draw call for single-channel modes, two for stereo
        vao.bind(ctx);
        if (channelMode == CHANNEL_STEREO) {
            uChannel.set(CHANNEL_LEFT);
            uYOffset.set(0.5f);
            vao.doRender(ctx);
            uChannel.set(CHANNEL_RIGHT);
            uYOffset.set(-0.5f);
            vao.doRender(ctx);
        } else {
            uChannel.set(channelMode);
            uYOffset.set(0.0f);
            vao.doRender(ctx);
        }
    }

    /**
     * Stops the audio capture thread and releases all GL resources.
     */
    @Override
    public void dispose() {
        // Signal the audio thread to stop and unblock it if it is waiting for a line
        audioReader.setRunning(false);
        audioReader.setLine(null);
        try {
            if (audioReaderThread != null) {
                audioReaderThread.join(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Release GL resources in reverse-init order
        vao.dispose();
        shader.dispose();
        glDeleteBuffers(pboId);
        glDeleteTextures(audioTextureId);
        pboId = 0;
        audioTextureId = 0;
    }

    /**
     * Sets the GL line width used when drawing the waveform strip.
     * The change is applied on the render thread on the next frame.
     *
     * @param v line width in pixels
     */
    public void setLineWidth(float v) {
        renderActions.enqueue(ACTION_LINE_WIDTH, ctx -> glLineWidth(v));
    }

    /**
     * Sets the channel display mode.
     * The change is applied on the render thread on the next frame.
     *
     * @param mode one of {@link #CHANNEL_BLEND}, {@link #CHANNEL_LEFT},
     *             {@link #CHANNEL_RIGHT}, or {@link #CHANNEL_STEREO}
     */
    public void setChannelMode(int mode) {
        renderActions.enqueue(ACTION_CHANNEL_MODE, ctx -> this.channelMode = mode);
    }

    /**
     * Sets the colour of the waveform line.
     * The change is applied on the render thread on the next frame.
     *
     * @param colour RGBA colour (each component in [0, 1])
     */
    public void setLineColour(Vector4f colour) {
        Vector4f copy = new Vector4f(colour);
        renderActions.enqueue(ACTION_LINE_COLOUR, ctx -> uColour.set(copy));
    }
}
