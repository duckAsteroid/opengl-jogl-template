package com.asteroid.duck.opengl;

import com.asteroid.duck.opengl.util.resources.io.ClasspathLoader;
import com.asteroid.duck.opengl.util.resources.io.PathBasedLoader;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * This is a really simple renderer of a single texture full screen
 * It does not use any of the libraries in this project.
 */
public class TextureRenderer {

	private long window;
	private int shaderProgram;
	private int vao, vbo, texture;

	private final int[] updateLocations = new int[]{6,7,14,30,31,47};
	private final float[] vertices = {
					// screen positions           // colors            // texture coords
					0.5f,  0.5f, 0.0f,     1.0f, 0.0f, 0.0f,    1.0f, 1.0f,  // top right
					0.5f, -0.5f, 0.0f,     0.0f, 1.0f, 0.0f,    1.0f, 0.0f,  // bottom right
					-0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f,    0.0f, 0.0f,  // bottom left

					0.5f,  0.5f, 0.0f,     1.0f, 0.0f, 0.0f,    1.0f, 1.0f,  // top right
					-0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f,    0.0f, 0.0f,  // bottom left
					-0.5f,  0.5f, 0.0f,    1.0f, 1.0f, 0.0f,    0.0f, 1.0f   // top left
	};

	private final String vertexShaderSource =
					"#version 330 core\n" +
									"in vec3 aPos;\n" +
									"in vec3 aColor;\n" +
									"in vec2 aTexCoord;\n" +
									"out vec3 ourColor;\n" +
									"out vec2 TexCoord;\n" +
									"void main() {\n" +
									"    gl_Position = vec4(aPos, 1.0);\n" + // Directly use the position
									"    ourColor = aColor;\n" +
									"    TexCoord = aTexCoord;\n" +
									"}\n";

	private final String fragmentShaderSource =
					"#version 330 core\n" +
									"out vec4 FragColor;\n" +
									"in vec3 ourColor;\n" +
									"in vec2 TexCoord;\n" +
									"uniform sampler2D ourTexture;\n" +
									"void main() {\n" +
									"    FragColor = vec4(ourColor, 1.0) * texture(ourTexture, TexCoord);\n" +
									"}\n";

	private ByteBuffer memBuffer;

	public void run() throws IOException {
		init();
		loop();
		cleanup();
	}

	boolean ready = false;

	private void init() throws IOException {

		for (int i = 0; i < updateLocations.length; i++) {
			System.out.println(vertices[updateLocations[i]]);
		}
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(800, 600, "Texture Renderer", NULL, NULL);
		if (window == NULL) {
			throw new RuntimeException("Failed to create the GLFW window");
		}

		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
				glfwSetWindowShouldClose(window, true);
			}
		});

		glfwSetFramebufferSizeCallback(window, new GLFWFramebufferSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				if (ready && width > 0 && height > 0) {
					glViewport(0, 0, width, height);

					// Calculate the aspect ratio
					float aspectRatio = (float) width / height;

					// Adjust the vertex data based on the aspect ratio
					/*
					for (int i = 0; i < vertices.length; i += 8) {
						// Adjust x-coordinate while keeping in NDC
						vertices[i] = vertices[i] * aspectRatio;
					}*/

					// Update the vertex buffer with the modified data
					// ... (same as before) ...
				}
			}
		});

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
		if (Platform.get() != Platform.LINUX) {
			glfwSetWindowPos(
					window,
					(vidmode.width() - 800) / 2,
					(vidmode.height() - 600) / 2
			);
		}

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);
		glfwShowWindow(window);

		GL.createCapabilities();
		ready = true;

		ShaderProgram prog = ShaderProgram.compile(
				ShaderSource.fromClass(vertexShaderSource, TextureRenderer.class),
				ShaderSource.fromClass(fragmentShaderSource, TextureRenderer.class),
				null);
		shaderProgram = prog.id();

		vao = glGenVertexArrays();
		glBindVertexArray(vao);

		vbo = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vbo);

		this.memBuffer = MemoryUtil.memAlloc(vertices.length * Float.BYTES);
		memBuffer.asFloatBuffer().put(vertices);
		updateBufferData(1.0f);

		// Position attribute
		int attribLocation = glGetAttribLocation(shaderProgram, "aPos");
		glVertexAttribPointer(attribLocation, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
		glEnableVertexAttribArray(attribLocation);

		// Color attribute
		attribLocation = glGetAttribLocation(shaderProgram, "aColor");
		glVertexAttribPointer(attribLocation, 3, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
		glEnableVertexAttribArray(attribLocation);

		// Texture coord attribute
		attribLocation = glGetAttribLocation(shaderProgram, "aTexCoord");
		glVertexAttribPointer(attribLocation, 2, GL_FLOAT, false, 8 * Float.BYTES, 6 * Float.BYTES);
		glEnableVertexAttribArray(attribLocation);

		texture = loadTexture("molly.jpg"); // texture

	}

	private void updateBufferData(float scaleFactor) {
		// Modify texture coordinates in the vertex data
		FloatBuffer floatBuffer = memBuffer.asFloatBuffer();
		for (int i = 0; i < updateLocations.length; i++) {
			floatBuffer.put(updateLocations[i], scaleFactor);
		}
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, memBuffer, GL_DYNAMIC_DRAW);
	}

	private void loop() {
		glClearColor(0.2f, 0.3f, 0.3f, 1.0f);
		final double startTime = GLFW.glfwGetTime();
		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			double elapsedTime = GLFW.glfwGetTime() - startTime;

			final double frequency = 0.5;
			updateBufferData((float)Math.abs(Math.sin(frequency * (2 * Math.PI * elapsedTime))));

			glUseProgram(shaderProgram);

			glActiveTexture(GL_TEXTURE0);
			glBindTexture(GL_TEXTURE_2D, texture);

			glBindVertexArray(vao);
			glDrawArrays(GL_TRIANGLES, 0, 6);

			glfwSwapBuffers(window);
			glfwPollEvents();

			Thread.yield();
		}
	}

	private int loadTexture(String sPath) throws IOException {
		ClasspathLoader loader = new ClasspathLoader(TextureRenderer.class, "/textures/");
		TextureFactory texFac = new TextureFactory(loader);

		Texture loaded = texFac.LoadTexture(sPath, ImageLoadingOptions.DEFAULT);
		System.out.println("Loaded texture: " + loaded);
		this.texture = loaded.getId();

		return texture;
	}

	private void cleanup() {
		glDeleteVertexArrays(vao);
		glDeleteBuffers(vbo);
		glDeleteProgram(shaderProgram);
		glfwDestroyWindow(window);
		glfwTerminate();
		MemoryUtil.memFree(memBuffer);
		glfwSetErrorCallback(null).free();
	}

	public static void main(String[] args) throws IOException {
		new TextureRenderer().run();
	}
}
