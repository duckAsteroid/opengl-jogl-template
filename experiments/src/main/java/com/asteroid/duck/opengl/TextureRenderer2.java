package com.asteroid.duck.opengl;

import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import com.asteroid.duck.opengl.util.resources.io.PathBasedLoader;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * A simple renderer that displays a texture on a quad, demonstrating dynamic texture
 * coordinate updates.
 * This implementation uses the libraries provided by this project.
 * Notably
 * @see VertexBufferObject
 * @see ShaderProgram
 * @see Texture
 */
public class TextureRenderer2 {

	private long window;
	private ShaderProgram shaderProgram;
	private Texture texture;
	private VertexArrayObject vao = new VertexArrayObject();
	private VertexBufferObject vertexBufferObject;
	private Vector4f clearColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);

	private final Object[][] vertices = {
					// screen positions                              // colors                                   // texture coords
					{ new Vector3f(1.0f,  1.0f, 0.0f),     new Vector3f(1.0f, 0.0f, 0.0f),    new Vector2f(1.0f, 1.0f) },  // top right
					{ new Vector3f(1.0f, -1.0f, 0.0f),     new Vector3f(0.0f, 1.0f, 0.0f),    new Vector2f(1.0f, 0.0f) },  // bottom right
					{ new Vector3f(-1.0f, -1.0f, 0.0f),    new Vector3f(0.0f, 0.0f, 1.0f),    new Vector2f(0.0f, 0.0f) },  // bottom left

					{ new Vector3f(1.0f,  1.0f, 0.0f),     new Vector3f(1.0f, 0.0f, 0.0f),    new Vector2f(1.0f, 1.0f) },  // top right
					{ new Vector3f(-1.0f, -1.0f, 0.0f),    new Vector3f(0.0f, 0.0f, 1.0f),    new Vector2f(0.0f, 0.0f) },  // bottom left
					{ new Vector3f(-1.0f,  1.0f, 0.0f),    new Vector3f(1.0f, 1.0f, 0.0f),    new Vector2f(0.0f, 1.0f) } // top left
	};

    // Directly use the position
    private final String vertexShaderSource =
            //language=GLSL
            """
            #version 330 core
            in vec3 aPos;
            in vec3 aColor;
            in vec2 aTexCoord;
            out vec3 ourColor;
            out vec2 TexCoord;
            void main() {
                gl_Position = vec4(aPos, 1.0);
                ourColor = aColor;
                TexCoord = aTexCoord;
            }
            """;

	private final String fragmentShaderSource =
            //language=GLSL
            """
            #version 330 core
            out vec4 FragColor;
            in vec3 ourColor;
            in vec2 TexCoord;
            uniform sampler2D ourTexture;
            void main() {
                FragColor = vec4(ourColor, 1.0) * texture(ourTexture, TexCoord);
            }
            """;

	private ByteBuffer memBuffer;
	private final VertexDataStructure structure = new VertexDataStructure(
					new VertexElement(VertexElementType.VEC_3F, "aPos"),
					new VertexElement(VertexElementType.VEC_3F, "aColor"),
					new VertexElement(VertexElementType.VEC_2F, "aTexCoord")
	);
	private TextureUnit textureUnit;

	public void run() throws IOException {
		init();
		loop();
		cleanup();
	}

	private void init() throws IOException {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}


        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GL_TRUE); // Required for Mac OS X
        // This is required to run on NVIDIA with prime offload
        glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);

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
		});

		GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        // Don't try and center on Wayland
        if (glfwGetPlatform() != GLFW_PLATFORM_WAYLAND) {
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - 800) / 2,
                    (vidmode.height() - 600) / 2
            );
        }

		glfwMakeContextCurrent(window);
        GL.createCapabilities();

		glfwSwapInterval(1);
		glfwShowWindow(window);



        // Get OpenGL version and renderer info
        System.out.println("OpenGL Vendor: " + GL11.glGetString(GL11.GL_VENDOR));
        System.out.println("OpenGL Renderer: " + GL11.glGetString(GL11.GL_RENDERER));
        System.out.println("OpenGL Version: " + GL11.glGetString(GL11.GL_VERSION));
        System.out.println("GLSL Version: " + GL11.glGetString(GL20.GL_SHADING_LANGUAGE_VERSION));

		this.shaderProgram = ShaderProgram.compile(
				ShaderSource.fromClass(vertexShaderSource, TextureRenderer2.class),
				ShaderSource.fromClass(fragmentShaderSource, TextureRenderer2.class)
				, null);

		vertexBufferObject = vao.createVbo(structure, 6);
		vertexBufferObject.init();
		for (int i = 0; i < vertices.length; i++) {
			vertexBufferObject.set(i, vertices[i]);
		}
		updateBufferData(1.0f);

		shaderProgram.use();
		vertexBufferObject.setup(shaderProgram);

		texture = loadTexture("src/main/resources/textures/molly.jpg"); // texture

		this.textureUnit = TextureUnit.index(0);
	}

	private void updateBufferData(float scaleFactor) {
		// Modify texture coordinates in the vertex data
		for (int i = 0; i < 6; i++) {
			Vector2f old = (Vector2f) vertices[i][2];
			Vector2f updated = new Vector2f(old.x * scaleFactor, old.y * scaleFactor);
			//System.out.println(i+"= old: " + render(old) + "\tupdated: " + render(updated) +"              \r");
			vertexBufferObject.setElement(i, structure.get("aTexCoord"), updated);
		}
		vertexBufferObject.update(UpdateHint.STREAM);
	}

	private static String render(Vector2f vec) {
		return String.format("[%.4f,%.4f]", vec.x, vec.y);
	}

	private void loop() {
		glClearColor(clearColor.x, clearColor.y, clearColor.z, clearColor.w);
		final double startTime = GLFW.glfwGetTime();
		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			double elapsedTime = GLFW.glfwGetTime() - startTime;

			final double frequency = 0.5;
			vao.bind();
			updateBufferData((float)Math.abs(Math.sin(frequency * (2 * Math.PI * elapsedTime))));

			shaderProgram.use();

			textureUnit.bind(texture);
			//textureUnit.useInShader(shaderProgram, "ourTexture");

			vao.doRender(null);
			glfwSwapBuffers(window);
			glfwPollEvents();

			Thread.yield();
		}
	}

	private Texture loadTexture(String sPath) throws IOException {
		Path path = Path.of(sPath);
		Path dir = path.getParent();

		TextureFactory texFac = new TextureFactory(new PathBasedLoader(dir));

		Texture loaded = texFac.LoadTexture(path.getFileName().toString(), ImageLoadingOptions.DEFAULT);
		System.out.println("Loaded texture: " + loaded);

		return loaded;
	}

	private void cleanup() {
		vertexBufferObject.dispose();
		shaderProgram.dispose();
		glfwDestroyWindow(window);
		glfwTerminate();
		MemoryUtil.memFree(memBuffer);
		glfwSetErrorCallback(null).free();
	}

	public static void main(String[] args) throws IOException {
		new TextureRenderer2().run();
	}
}
