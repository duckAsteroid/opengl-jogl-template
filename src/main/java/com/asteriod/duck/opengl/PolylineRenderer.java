package com.asteriod.duck.opengl;


import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import static com.jogamp.opengl.GL4.*;
import static com.jogamp.opengl.GLES2.GL_VERTEX_ARRAY;

import javax.swing.JFrame;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public class PolylineRenderer implements GLEventListener {
	private static final String VERTEX_SHADER =
					"""
									#version 330 core
									layout (location = 0) in vec2 vertex;
									void main() {
									    gl_Position = vec4(vertex, 0.0, 1.0);
									}
									""";

	private static final String GEOMETRY_SHADER =
					"""
									#version 330 core
									layout (lines) in;
									layout (line_strip, max_vertices = 2) out;
									void main() {
									    gl_Position = gl_in[0].gl_Position;
									    EmitVertex();
									    gl_Position = gl_in[1].gl_Position;
									    EmitVertex();
									    EndPrimitive();
									}
									""";

	private static final String FRAGMENT_SHADER =
					"""
									#version 330 core
									out vec4 color;
									void main() {
									    color = vec4(1.0, 1.0, 1.0, 1.0);
									}
									""";

	private static final float[] LINE_VERTICES = { 0.0f, 0.0f, 0.5f, 0.5f, 1.0f, 1.0f };
	private ShaderProgram shaderProgram;
	private int[] vbo = new int[1];
	private int[] vao = new int[1];

	private final FloatBuffer pointBuffer = Buffers.newDirectFloatBuffer(512);

	private long start;

	@Override
	public void init(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL.GL_LINE_SMOOTH);
		gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);

		gl.glGenVertexArrays(1, vao, 0);
		gl.glBindVertexArray(vao[0]);

		gl.glGenBuffers(1, vbo, 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);

		gl.glEnableVertexAttribArray(0);
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);


		ShaderCode vertexShader = new ShaderCode(GL_VERTEX_SHADER, 1, new CharSequence[][] {{VERTEX_SHADER}});
		ShaderCode fragmentShader = new ShaderCode(GL_FRAGMENT_SHADER, 1, new CharSequence[][] {{FRAGMENT_SHADER}});

		this.shaderProgram = new ShaderProgram();
		shaderProgram.add(vertexShader);
		shaderProgram.add(fragmentShader);
		shaderProgram.init(gl);

		if (!shaderProgram.link(gl, System.err)) {
			ByteBuffer buffer = Buffers.newDirectByteBuffer(2 * 1024);
			gl.glGetShaderInfoLog(shaderProgram.program(), 1, IntBuffer.wrap(new int[]{buffer.limit()}), buffer);
			buffer.flip();
			System.err.println(StandardCharsets.UTF_8.decode(buffer));
		}

		start = System.currentTimeMillis();
	}



	@Override
	public void display(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

		long elapsed = System.currentTimeMillis() - start;

		int points = fillPoints(elapsed, pointBuffer);

		gl.glUseProgram(shaderProgram.id());
		gl.glBindVertexArray(vao[0]);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);

		gl.glBufferData(GL_ARRAY_BUFFER, (long) pointBuffer.remaining() * Float.BYTES, pointBuffer, GL_DYNAMIC_DRAW);
		gl.glLineWidth(2f);
		gl.glDrawArrays(GL_LINE_STRIP, 0, points);

		gl.glBindVertexArray(0);
	}


	private int fillPoints(long elapsed, FloatBuffer pointBuffer) {
		final int size = pointBuffer.limit() / 2;
		pointBuffer.clear();

		for(int i = 0; i < size; i ++) {
			double t = (elapsed + i) / 1000.0;
			float value = (float) Math.sin(2 * Math.PI * 10 * t);
			float x = (((float) i / size) * 2.0f ) - 1.0f;
			//System.out.println("X="+x+",Y="+value+" @ "+(elapsed+i));
			pointBuffer.put( x);
			pointBuffer.put( value);

		}

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
	public void dispose(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();
		gl.glDeleteBuffers(1, vbo, 0);
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
	}

	public static void main(String[] args) {
		PolylineRenderer polylineRenderer = new PolylineRenderer();

		GLProfile profile = GLProfile.getGL4ES3();
		GLCapabilities glCapabilities = new GLCapabilities(profile);
		final GLWindow window = GLWindow.create(glCapabilities);

		window.setSize(600, 600);
		window.setResizable(true);
		window.setTitle("PolylineRenderer");

		//window.setFullscreen(true);
		window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
		window.setVisible(true);

		window.addGLEventListener(polylineRenderer);

//		elapsed = 0;
//
		Animator animator = new Animator(window);
		animator.setUpdateFPSFrames(1, null);
		animator.setRunAsFastAsPossible(true);
		animator.start();

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowDestroyed(WindowEvent e) {
				animator.stop();
				System.exit(0);
			}
		});

		window.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
					window.destroy();
				}
				else if (KeyEvent.VK_F11 == e.getKeyCode()) {
					window.setFullscreen(!window.isFullscreen());
					//System.out.println("Size: "+windowSizeString());
				}
				else {
					System.out.println("Eh?");
				}
			}
		});

		//initDebug(window, window.getGL().getGL4());
	}

	private static void initDebug(GLWindow window, GL4 gl) {

		window.getContext().addGLDebugListener(new GLDebugListener() {
			@Override
			public void messageSent(GLDebugMessage event) {
				System.out.println(event);
				//throw new RuntimeException(event.getDbgMsg());
			}
		});

		gl.glDebugMessageControl(
						GL_DONT_CARE,
						GL_DONT_CARE,
						GL_DONT_CARE,
						0,
						null,
						true);

		gl.glDebugMessageControl(
						GL_DONT_CARE,
						GL_DONT_CARE,
						GL_DEBUG_SEVERITY_HIGH,
						0,
						null,
						true);

		gl.glDebugMessageControl(
						GL_DONT_CARE,
						GL_DONT_CARE,
						GL_DEBUG_SEVERITY_MEDIUM,
						0,
						null,
						true);
	}
}

