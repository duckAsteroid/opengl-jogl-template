package com.asteroid.duck.opengl.util.audio;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.LineUnavailableException;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import static com.asteroid.duck.opengl.util.audio.LineAcquirer.IDEAL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * https://github.com/jackaudio/jackaudio.github.com/wiki
 */
public class Polyline implements RenderedItem {
	private ShaderProgram shaderProgram = null;
	private VertexArrayObject vao = new VertexArrayObject();
	private VertexBufferObject vbo;
	private FloatBuffer pointBuffer;
	// Buffer for raw audio data read from AudioDataSource
	private ByteBuffer audioBuffer;
	private RollingFloatBuffer rollingFloatBuffer;
	private AudioDataSource openLine;
	private int bytesPerSample;

	private float lineWidth = 4.0f;
	private Vector4f lineColour = StandardColors.RED.get();

	private Vector4f backgroundColour = StandardColors.BLACK.get();

	private boolean clear = true;

	private List<Consumer<RenderContext>> renderActions = Collections.synchronizedList(new LinkedList<>());

	private int fillPoints(FloatBuffer pointBuffer) {
		// how many samples are available?
		int available = openLine.available() / bytesPerSample;

		audioBuffer.clear();
		audioBuffer.limit(Math.min(available * bytesPerSample, audioBuffer.capacity()));
		int read = openLine.read(audioBuffer.array(), 0, Math.min(audioBuffer.limit(), openLine.available()));
		if (read > 0) {
			audioBuffer.position(read);
			audioBuffer.flip();
			rollingFloatBuffer.write(audioBuffer.asShortBuffer());
		}

		pointBuffer.clear();
		rollingFloatBuffer.read(pointBuffer);
		pointBuffer.flip();

		return pointBuffer.remaining() / 2;
	}


	@Override
	public void init(RenderContext ctx) throws IOException {
		addKeyHandlers(ctx);
		ctx.setDesiredUpdateFrequency(30.0);


		final int BUFFER_SIZE = ctx.getWindow().width;
		rollingFloatBuffer = new RollingFloatBuffer(BUFFER_SIZE);
		rollingFloatBuffer.setMax(15000);

		LineAcquirer laq = new LineAcquirer();
		this.bytesPerSample = IDEAL.getChannels() * (IDEAL.getSampleSizeInBits() / 8);
		//double seconds = 200 / 1000.0d; // 200 ms
		int numSamples =  ctx.getWindow().width + 100;
		this.audioBuffer = ByteBuffer.allocate(bytesPerSample * numSamples);
		this.audioBuffer.order(IDEAL.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		try {
			this.openLine = laq.acquire(ctx, IDEAL);
			System.out.println(openLine);
			this.openLine.open(IDEAL, audioBuffer.limit());
			this.openLine.start();
		} catch (LineUnavailableException e) {
			throw new IOException(e);
		}

		//glClearColor(backgroundColour.x, backgroundColour.y, backgroundColour.z, backgroundColour.w);
		glEnable(GL_LINE_SMOOTH);
		glHint(GL_LINE_SMOOTH_HINT, GL_FASTEST);

		shaderProgram = ctx.getResourceManager().getShader("polyline", "polyline/vertex.glsl","polyline/frag.glsl", null);

		vao.init(ctx);
		vao.setDrawMode(BufferDrawMode.LINE_STRIP);
		VertexElement vertex = new VertexElement(VertexElementType.VEC_2F, "vertex");
		VertexDataStructure structure = new VertexDataStructure(vertex);
		vbo = vao.createVbo(structure, BUFFER_SIZE);
		vbo.init(ctx);
		pointBuffer = vbo.memBuffer().asFloatBuffer();

		shaderProgram.use(ctx);

		vbo.setup(shaderProgram);

		shaderProgram.uniforms().get("lineColor", Vector4f.class).set(lineColour);
	}

	private void addKeyHandlers(final RenderContext renderContext) {
		var ctx = renderContext.getKeyRegistry();
		ctx.registerKeyAction(GLFW.GLFW_KEY_UP, () -> rollingFloatBuffer.incMax(100), "Increase max by 100");
		ctx.registerKeyAction(GLFW.GLFW_KEY_UP, GLFW.GLFW_MOD_SHIFT, () -> rollingFloatBuffer.incMax(1000), "Increase max by 1000");
		ctx.registerKeyAction(GLFW.GLFW_KEY_DOWN, () -> rollingFloatBuffer.decMax(100), "Decrease max by 100");
		ctx.registerKeyAction(GLFW.GLFW_KEY_DOWN, GLFW.GLFW_MOD_SHIFT, () -> rollingFloatBuffer.decMax(1000), "Decrease max by 1000");
		ctx.registerKeyAction(GLFW.GLFW_KEY_Q, this::increaseLineWidth, "Increase line width");
		ctx.registerKeyAction(GLFW.GLFW_KEY_A, this::decreaseLineWidth, "Decrease line width");
		ctx.registerKeyAction(GLFW.GLFW_KEY_C, this::toggleClear, "Toggle clear screen on render");
		ctx.registerKeyAction(GLFW.GLFW_KEY_L, () -> this.randomLineColour(renderContext), "Random line colour");
	}

	final StandardColors[] colors = StandardColors.values();

	private void randomLineColour(RenderContext ctx) {
		int rnd = ctx.getRandom().nextInt(colors.length);
		StandardColors color = colors[rnd];
		System.out.println("Setting line color to " + color.name());
		this.lineColour = color.get();
		shaderProgram.uniforms().get("lineColor", Vector4f.class).set(lineColour);
	}


	private void increaseLineWidth() {
		if(lineWidth < 50) {
			lineWidth += 0.1f;
			System.out.println(lineWidth);
		}
	}

	private void decreaseLineWidth() {
		if(lineWidth > 0.1) {
			lineWidth -= 0.1f;
			System.out.println(lineWidth);
		}
	}


	@Override
	public void doRender(RenderContext ctx) {
		int points = fillPoints(pointBuffer);

		if (clear) {
			glClearColor(backgroundColour.x, backgroundColour.y, backgroundColour.z, backgroundColour.w);
			glClear(GL_COLOR_BUFFER_BIT);
		}

		Rectangle window = ctx.getWindow();
		shaderProgram.use(ctx);
		//shaderProgram.uniforms().get("resolution", Vector2f.class).set(new Vector2f(window.width, window.height));


		vbo.update(UpdateHint.DYNAMIC);
		glLineWidth(lineWidth);
		vao.doRender(ctx);
	}

	@Override
	public void dispose() {
		openLine.stop();
		openLine.close();
		shaderProgram.dispose();
		vao.dispose();
	}

	public Vector4f getLineColour() {
		return lineColour;
	}

	public void setLineColour(Vector4f color) {
		this.lineColour = color;
	}

	public float getLineWidth() {
		return lineWidth;
	}

	public void setLineWidth(float v) {
		this.lineWidth = v;
	}

	public void toggleClear() {
		this.clear = !this.clear;
	}

	public boolean isClear() {
		return clear;
	}

	public void setClear(boolean clear) {
		this.clear = clear;
	}
}
