package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.GLWindow;
import com.asteriod.duck.opengl.util.audio.LineAcquirer;
import com.asteriod.duck.opengl.util.audio.RollingFloatBuffer;
import com.asteriod.duck.opengl.util.keys.KeyCombination;
import com.asteriod.duck.opengl.util.keys.Keys;
import com.asteriod.duck.opengl.util.resources.ResourceManager;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.timer.Timer;
import org.joml.Random;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.awt.*;
import java.io.IOException;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.asteriod.duck.opengl.util.audio.LineAcquirer.IDEAL;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Polyline extends GLWindow {

	private static final float[] LINE_VERTICES = { 0.0f, 0.0f, 0.5f, 0.5f, 1.0f, 1.0f };
	private ShaderProgram shaderProgram = null;
	private final AtomicBoolean shaderDispose = new AtomicBoolean(false);

	private final Timer timer = Timer.glfwGetTimeInstance();
	private int vbo;
	private int vao;

	private final MemoryStack mem;
	private final FloatBuffer pointBuffer;
	private ByteBuffer audioBuffer;
	private RollingFloatBuffer rollingFloatBuffer;
	private TargetDataLine openLine;
	private int bytesPerSample;

	private float lineWidth = 2.0f;
	private Vector4f lineColour = new Vector4f(0.0f,1.0f, 0.0f, 1.0f);
	private Random rnd = new Random();


	public Polyline(int width, int height) {
		super("Polyline", width, height, "icon16.png");
		mem = MemoryStack.stackPush();
		int BUFFER_SIZE = 2048;
		pointBuffer = mem.mallocFloat(BUFFER_SIZE * 2);
		rollingFloatBuffer = new RollingFloatBuffer(BUFFER_SIZE);
		rollingFloatBuffer.setMax(200);
	}


	public static void main(String[] args) throws IOException {
		Polyline polylineRenderer = new Polyline(600,600);
		polylineRenderer.displayLoop();
	}

	private int fillPoints(FloatBuffer pointBuffer) {
		// how many samples are available?
		int available = openLine.available() / bytesPerSample;

		audioBuffer.clear();
		audioBuffer.limit(Math.min(available * bytesPerSample, audioBuffer.capacity()));
		int read = openLine.read(audioBuffer.array(), 0, Math.min(audioBuffer.limit(), openLine.available()));
		audioBuffer.position(read);
		audioBuffer.flip();

		rollingFloatBuffer.write(audioBuffer.asShortBuffer());

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
	public void registerKeys() {
		registerKeyAction(GLFW.GLFW_KEY_UP, () -> rollingFloatBuffer.incMax(100));
		registerKeyAction(GLFW.GLFW_KEY_UP, GLFW.GLFW_MOD_SHIFT, () -> rollingFloatBuffer.incMax(1000));
		registerKeyAction(GLFW.GLFW_KEY_DOWN, () -> rollingFloatBuffer.decMax(100));
		registerKeyAction(GLFW.GLFW_KEY_DOWN, GLFW.GLFW_MOD_SHIFT, () -> rollingFloatBuffer.decMax(1000));
		registerKeyAction(GLFW.GLFW_KEY_Q, this::increaseLineWidth);
		registerKeyAction(GLFW.GLFW_KEY_A, this::decreaseLineWidth);
		registerKeyAction(GLFW.GLFW_KEY_C, this::randomiseLineColor);
	}

	private void randomiseLineColor() {
		lineColour = new Vector4f(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat(), 1.0f);
		System.out.println(lineColour);
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
	public void init() throws IOException {

		LineAcquirer laq = new LineAcquirer();
		List<LineAcquirer.MixerLine> mixerLines = laq.allLinesMatching(TargetDataLine.class, IDEAL);
		for (int i = 0; i < mixerLines.size(); i++) {
			System.out.println(i +"="+mixerLines.get(i));
		}
		this.bytesPerSample = IDEAL.getChannels() * (IDEAL.getSampleSizeInBits() / 8);
		double seconds = 200 / 1000.0d; // 200 ms
		int numSamples =  (int)Math.round(seconds * IDEAL.getSampleRate());
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


		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		glEnable(GL_LINE_SMOOTH);
		glHint(GL_LINE_SMOOTH_HINT, GL_NICEST);

		IntBuffer vaoPtr = mem.ints(vao);
		glGenVertexArrays(vaoPtr);
		vao = vaoPtr.get(0);
		glBindVertexArray(vao);

		IntBuffer vboPtr = mem.ints(vbo);
		glGenBuffers(vboPtr);
		vbo = vboPtr.get(0);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);

		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);


		shaderProgram = ResourceManager.instance().GetShader("polyline", "polyline/vertex.glsl","polyline/frag.glsl", null);


		timer.reset();
	}

	@Override
	public void render() throws IOException {
		timer.update();
		int points = fillPoints(pointBuffer);
		Rectangle window = getWindow();
		shaderProgram.setVector2f("resolution", window.width, window.height, false);
		shaderProgram.setVector4f("lineColor", lineColour, true);

		glBindVertexArray(vao);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);

		glBufferData(GL_ARRAY_BUFFER, pointBuffer, GL_DYNAMIC_DRAW);
		glLineWidth(lineWidth);

		glDrawArrays(GL_LINE_STRIP, 0, points);

		glBindVertexArray(0);
	}

	@Override
	public void dispose() {
		shaderProgram.destroy();
		glDeleteBuffers(vbo);
		mem.close();
	}
}
