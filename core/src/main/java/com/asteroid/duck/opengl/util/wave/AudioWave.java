package com.asteroid.duck.opengl.util.wave;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.audio.AudioDataSource;
import com.asteroid.duck.opengl.util.audio.LineAcquirer;
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

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
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

public class AudioWave implements RenderedItem {
    private static final int SCREEN_WIDTH = 1024;

    // how many samples we want to store in our texture.
    // We need at least 1024 to fill the screen, but we use 2048 to have a "sliding window" effect
    private static final int AUDIO_BUFFER_SIZE = SCREEN_WIDTH * 2;
    // there are 2 channels (stereo) (Red and Green in the texture),
    // so we need to multiply by 2 to get the total number of floats we can store
    private static final int AUDIO_TEXTURE_WIDTH = (AUDIO_BUFFER_SIZE * 2);
    //  each sample is a signed short (2 bytes), so we need to multiply by 2 to get the texture width in bytes
    private static final int AUDIO_TEXTURE_BYTE_SIZE = AUDIO_TEXTURE_WIDTH * 2;


    private ShaderProgram shader;
    private VertexArrayObject vao;
    private int audioTextureId;
    private int pboId;

    private Uniform<Integer> uHead;

    private AudioReader audioReader;


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
        Thread audioReaderThread = new Thread(audioReader);
        audioReaderThread.start();
        glLineWidth(6.0f);
        ctx.setDesiredUpdateFrequency(60.0);
    }

    public void setLine(AudioDataSource line) {
        System.out.println("Setting audio line to: " + line.getName());
        audioReader.setLine(line);
    }

    /**
     * Our VBO contains 1024 vertices, each with a 2D position.
     * The x coordinate is fixed and goes from 0 to 1 across the screen,
     * while the y coordinate will be multiplied with audio data (from a texture) in the shader.
     */
    private void initVbo(RenderContext ctx) {
        this.vao = new VertexArrayObject();
        vao.setDrawMode(BufferDrawMode.LINE_STRIP);
        vao.init(ctx);
        // create VBO
        var POSITION = new VertexElement(VertexElementType.VEC_2F, "position");
        VertexDataStructure dataStructure = new VertexDataStructure(POSITION);
        var vbo = vao.createVbo(dataStructure, SCREEN_WIDTH);
        vbo.init(ctx);
        final float y = 10f;
        IntStream.range(0, SCREEN_WIDTH).forEach(i -> {
            float x = (((float) i / SCREEN_WIDTH ) * 2f) - 1f; // scale to -1 to 1 for NDC
            vbo.setElement(i, POSITION, new Vector2f(x,y));
        });
        vbo.update(UpdateHint.STATIC);
    }

    // language=GLSL
    private static final String VERTEX_SHADER = """
		#version 330 core
		// Static 0.0 to 1.0 from VBO
		in vec2 position;
		// The audio texture (2048 samples, but we only use 1024 at a time)
		uniform sampler1D uAudioTex;
		// current write head / 2048
		uniform int uHead;
		// which channel to visualize 0 = blend, 1 = left, 2 = right
		uniform int uChannel = 0;
	
		void main() {
			// work out which sample to read from the texture for this vertex.
			int sampleIndex = (2048 + uHead + gl_VertexID) % 2048;
	
			// Fetch the audio data.
			vec2 stereo = texelFetch(uAudioTex, sampleIndex, 0).rg;
			// now convert into an amplitude
			float amplitude = position.y *
				((uChannel == 0) ? (stereo.r + stereo.g) * 0.5 :
					(uChannel == 1) ? stereo.r : stereo.g);
	
			// Center the wave vertically and scale X to fill the screen (-1 to 1)
			gl_Position = vec4(position.x, amplitude, 0.0, 1.0);
		}
	""";

    // language=GLSL
    private static final String FRAGMENT_SHADER = """
		#version 330 core
		out vec4 fragColor;
	
		void main() {
			fragColor = vec4(1.0); // white
		}
	""";

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
        this.uHead = shader.uniforms().get("uHead", Integer.class);

        // setup the VAO
        vao.getVbo().setup(shader);
    }

    private ByteBuffer initAudioBuffer(RenderContext ctx) {
        this.audioTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_1D, audioTextureId);

        // 1. Set Wrapping to REPEAT so the uHead offset wraps automatically
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);

        // 2. Set Filtering (LINEAR for smooth waves, NEAREST for raw steps)
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // 3. Initialize the storage for
        // Format: Internal Format (RG16_SNORM), Width (2048), Format (RED+GREEN), Type (SHORT)
        glTexImage1D(GL_TEXTURE_1D, 0, GL_RG16_SNORM, AUDIO_BUFFER_SIZE, 0, GL_RG, GL_SHORT, (ByteBuffer)null);

        glBindTexture(GL_TEXTURE_1D, 0);


        // 1. Create a Pixel Buffer Object (PBO)
        this.pboId = glGenBuffers();
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);

        // 2. Allocate the buffer with Persistent Mapping flags
        int flags = GL_MAP_WRITE_BIT | GL_MAP_PERSISTENT_BIT | GL_MAP_COHERENT_BIT;
        glBufferStorage(GL_PIXEL_UNPACK_BUFFER, AUDIO_TEXTURE_BYTE_SIZE, flags);

        // 3. Map it once and keep the reference
        return glMapBufferRange(GL_PIXEL_UNPACK_BUFFER, 0, AUDIO_TEXTURE_BYTE_SIZE, flags);
    }

    @Override
    public void doRender(RenderContext ctx) {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        shader.use(ctx);
        uHead.set(audioReader.getHead()); // pass the current head position to the shader

        // transfer audio data from the PBO to the Texture on the GPU.
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
        glBindTexture(GL_TEXTURE_1D, audioTextureId);

        // This is the "Actual" Flush.
        // It tells the GPU to copy the PBO memory into the Texture memory.
        glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);

        glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

        // now render the VAO (VBO) using the shader
        vao.bind(ctx);
        vao.doRender(ctx);
    }

    @Override
    public void dispose() {
        audioReader.setRunning(false);
        shader.dispose();
    }

    public void setLineWidth(float v) {
    }

    public void setLineColour(Vector4f vector4f) {
    }
}
