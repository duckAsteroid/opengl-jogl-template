package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
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
import com.asteroid.duck.opengl.util.stats.Stats;
import com.asteroid.duck.opengl.util.stats.StatsFactory;
import org.joml.Vector2f;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL21.GL_PIXEL_UNPACK_BUFFER;
import static org.lwjgl.opengl.GL30.GL_MAP_WRITE_BIT;
import static org.lwjgl.opengl.GL30.glMapBufferRange;
import static org.lwjgl.opengl.GL44.*;

public class SoundWave implements Experiment {
	private static final int SCREEN_WIDTH = 1024;

	// 32 stereo samples (2 channels, 2 bytes per sample)
	private static final int CHUNK_SIZE = 32 * 2 * 2;

	// how many samples we want to store in our texture.
	// We need at least 1024 to fill the screen, but we use 2048 to have a "sliding window" effect
	private static final int AUDIO_BUFFER_SIZE = SCREEN_WIDTH * 2;
	// there are 2 channels (stereo) (Red and Green in the texture),
	// so we need to multiply by 2 to get the total number of floats we can store
	private static final int AUDIO_TEXTURE_WIDTH = (AUDIO_BUFFER_SIZE * 2);
	//  each sample is a signed short (2 bytes), so we need to multiply by 2 to get the texture width in bytes
	private static final int AUDIO_TEXTURE_BYTE_SIZE = AUDIO_TEXTURE_WIDTH * 2;

	private static final float F_AUDIO_TEXTURE_WIDTH = (float) AUDIO_TEXTURE_WIDTH;

	private ShaderProgram shader;
	private VertexArrayObject vao;
	private int audioTextureId;
	private int pboId;

	@Override
	public String getDescription() {
		return "Renders an audio wave on screen";
	}

	private TargetDataLine line;
	private int head = 0;
	private boolean running = true;
	private Uniform<Integer> uHead;

	private class AudioReader implements Runnable {
		// debug the chunk size to check we're reading in the expected sizes (except maybe the first and last reads)
		private Stats chunkSize = StatsFactory.stats("Audio: Chunk Size");
		private Stats available = StatsFactory.stats("Audio: Available");
		private byte[] audioChunk = new byte[CHUNK_SIZE];
		private final ByteBuffer gpuMapped;

        private AudioReader(ByteBuffer gpuMapped) {
            this.gpuMapped = gpuMapped;
        }

        public void run() {
			// Read in small chunks for low lag
			line.start();
			while (running) {
				available.add(line.available());
				// read a chunk from audio line
				int read = line.read(audioChunk, 0, CHUNK_SIZE);
				chunkSize.add(read);

				// write it into the mapped buffer at the current head position
				gpuMapped.position(head);
				gpuMapped.put(audioChunk, 0, read);
				// move write pointer and wrap around if needed
				head = (head + read) % AUDIO_TEXTURE_BYTE_SIZE;
			}
			line.stop();
			System.out.println("chunk=" + chunkSize);
			System.out.println("available=" + available);
		}
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		// create an audio line to read from
		initAudio(ctx);
		// an area of memory shared between the Producer thread (filling it with audio data)
		// and the GPU (rendering it)
		ByteBuffer gpuMapped = initAudioBuffer(ctx);
		// a VBO to draw the lines
		initVbo(ctx);
		// a shader that takes the audio data from the texture and multiplies it with the y coordinate of
		// the vertices to create a wave effect
		initShader(ctx);

		// Start a Producer thread to continuously fill the GPU buffer with audio data
		Thread audioReader = new Thread(new AudioReader(gpuMapped));
		audioReader.start();
		glLineWidth(6.0f);
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
	
			// Fetch the amplitude.
			vec2 stereo = texelFetch(uAudioTex, sampleIndex, 0).rg;
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
				ShaderSource.fromClass(VERTEX_SHADER, SoundWave.class),
				ShaderSource.fromClass(FRAGMENT_SHADER, SoundWave.class),
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

	private void initAudio(RenderContext ctx) throws IOException {
		LineAcquirer audio = new LineAcquirer();
		audio.dump();
		try {
			var optLine = audio.allLinesMatching(LineAcquirer.IDEAL)
					.filter(line -> line.mixer().getMixerInfo().getName().startsWith("alsa")).findFirst().orElseThrow();
			this.line = optLine.getTargetDataLine().raw();
			line.open(LineAcquirer.IDEAL, CHUNK_SIZE);
			System.out.println("Audio line acquired: " + optLine);
			System.out.println("Audio format: " + line.getFormat());
		} catch (LineUnavailableException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void doRender(RenderContext ctx) {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		shader.use(ctx);
		uHead.set(head); // pass the current head position as a normalized offset to the shader

		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, pboId);
		glBindTexture(GL_TEXTURE_1D, audioTextureId);

		// This is the "Actual" Flush.
		// It tells the GPU to copy the PBO memory into the Texture memory.
		glTexSubImage1D(GL_TEXTURE_1D, 0, 0, AUDIO_BUFFER_SIZE, GL_RG, GL_SHORT, 0);

		glBindBuffer(GL_PIXEL_UNPACK_BUFFER, 0);

		vao.bind(ctx);
		vao.doRender(ctx);
	}

	@Override
	public void dispose() {
		running = false;

		line.stop();
		line.close();

		shader.dispose();
	}
}
