package com.asteroid.duck.opengl.util.audio;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

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
	private int vbo;
	private int vao;

	private MemoryStack mem;
	private FloatBuffer pointBuffer;
	private ByteBuffer audioBuffer;
	private RollingFloatBuffer rollingFloatBuffer;
	private TargetDataLine openLine;
	private int bytesPerSample;

	private float lineWidth = 4.0f;
	private Vector4f lineColour = new Vector4f(1.0f,0.0f,0.0f,1.0f);
	private Vector4f backgroundColour = new Vector4f(0.4f, 0.4f, 0.4f, 1.0f);
	private boolean clear = false;

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

	public static final boolean isEqual(double expected, float actual) {
		final double error = 0.001;
		double min = expected - error;
		double max = expected + error;
		return actual > min && actual < max;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		addKeyHandlers(ctx.getKeyRegistry());
		ctx.setDesiredUpdateFrequency(30.0);

		mem = MemoryStack.stackPush();
		int BUFFER_SIZE = ctx.getWindow().width;
		pointBuffer = mem.mallocFloat(BUFFER_SIZE * 2);
		rollingFloatBuffer = new RollingFloatBuffer(BUFFER_SIZE);
		rollingFloatBuffer.setMax(15000);

		LineAcquirer laq = new LineAcquirer();
		List<LineAcquirer.MixerLine> mixerLines = laq.allLinesMatching(TargetDataLine.class, IDEAL);
		for (int i = 0; i < mixerLines.size(); i++) {
			System.out.println(i +"="+mixerLines.get(i));
		}
		this.bytesPerSample = IDEAL.getChannels() * (IDEAL.getSampleSizeInBits() / 8);
		//double seconds = 200 / 1000.0d; // 200 ms
		int numSamples =  ctx.getWindow().width + 100;
		this.audioBuffer = ByteBuffer.allocate(bytesPerSample * numSamples);
		this.audioBuffer.order(IDEAL.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
		try {
			LineAcquirer.MixerLine mixerLine = mixerLines.get(1);
			System.out.println(mixerLine);
			this.openLine = mixerLine.getTargetDataLine();
			this.openLine.open(IDEAL, audioBuffer.limit());
			this.openLine.start();
		} catch (LineUnavailableException e) {
			throw new IOException(e);
		}

		//glClearColor(backgroundColour.x, backgroundColour.y, backgroundColour.z, backgroundColour.w);
		glEnable(GL_LINE_SMOOTH);
		glHint(GL_LINE_SMOOTH_HINT, GL_FASTEST);

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);

		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);


		shaderProgram = ctx.getResourceManager().GetShader("polyline", "polyline/vertex.glsl","polyline/frag.glsl", null);

	}

	private void addKeyHandlers(KeyRegistry ctx) {
		ctx.registerKeyAction(GLFW.GLFW_KEY_UP, () -> rollingFloatBuffer.incMax(100), "Increase max by 100");
		ctx.registerKeyAction(GLFW.GLFW_KEY_UP, GLFW.GLFW_MOD_SHIFT, () -> rollingFloatBuffer.incMax(1000), "Increase max by 1000");
		ctx.registerKeyAction(GLFW.GLFW_KEY_DOWN, () -> rollingFloatBuffer.decMax(100), "Decrease max by 100");
		ctx.registerKeyAction(GLFW.GLFW_KEY_DOWN, GLFW.GLFW_MOD_SHIFT, () -> rollingFloatBuffer.decMax(1000), "Decrease max by 1000");
		ctx.registerKeyAction(GLFW.GLFW_KEY_Q, this::increaseLineWidth, "Increase line width");
		ctx.registerKeyAction(GLFW.GLFW_KEY_A, this::decreaseLineWidth, "Decrease line width");
		ctx.registerKeyAction(GLFW.GLFW_KEY_C, this::toggleClear, "Toggle clear screen on render");
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
		shaderProgram.use();
		shaderProgram.setVector2f("resolution", window.width, window.height);
		shaderProgram.setVector4f("lineColor", lineColour);



		glBindVertexArray(vao);
		//glBindBuffer(GL_ARRAY_BUFFER, vbo);

		glBufferData(GL_ARRAY_BUFFER, pointBuffer, GL_DYNAMIC_DRAW);
		glLineWidth(lineWidth);

		glDrawArrays(GL_LINE_STRIP, 0, points);

		glBindVertexArray(0);
		//glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	@Override
	public void dispose() {
		openLine.stop();
		openLine.close();
		shaderProgram.destroy();
		glDeleteBuffers(vbo);
		mem.close();
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
