package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.shader.vars.Variable;
import com.asteriod.duck.opengl.util.resources.shader.vars.VariableType;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

public class PassthruTextureRenderer implements RenderedItem {

	private static final Logger LOG = LoggerFactory.getLogger(PassthruTextureRenderer.class);

	private ShaderProgram shaderProgram = null;

	private int vbo;
	private int vao;
	private final Supplier<Integer> textureIdSupplier;

	public PassthruTextureRenderer(Supplier<Integer> textureIdSupplier) {
		this.textureIdSupplier = textureIdSupplier;
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();
	}

	private void initShaderProgram(RenderContext ctx) throws IOException {
		// load the GLSL Shaders
		this.shaderProgram =ctx.getResourceManager().GetShader("passthru", "passthru/vertex.glsl", "passthru/frag.glsl", null);
		LOG.info("Using shader program {}", shaderProgram);
	}

	private void initTextures(RenderContext ctx) {
		shaderProgram.use();
	}


	private void initBuffers() {

		// Define the vertices of the rectangle
		float[] quadVertices = {
						// First triangle
						-1.0f,  1.0f,  // Top Left
						1.0f,  1.0f,  // Top Right
						-1.0f, -1.0f,  // Bottom Left
						// Second triangle
						1.0f, -1.0f,  // Bottom Right
						-1.0f, -1.0f,  // Bottom Left
						1.0f,  1.0f   // Top Right
		};


		try(MemoryStack stack = MemoryStack.stackPush()) {
			// Create a VAO and VBO
			vao = glGenVertexArrays();
			vbo = glGenBuffers();
			// bind them
			glBindVertexArray(vao);
			glBindBuffer(GL_ARRAY_BUFFER, vbo);

			// Store the vertex data in the VBO
			FloatBuffer vertexBuffer = stack.floats(quadVertices);
			glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

			glEnableVertexAttribArray(0);
			glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);

		}
	}

	@Override
	public void doRender(RenderContext ctx) {
		shaderProgram.use();
		//glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_2D, textureIdSupplier.get());
		glDrawArrays(GL_TRIANGLES, 0, 6);

		glUseProgram(0);
	}

	@Override
	public void dispose() {
		shaderProgram.destroy();
	}
}
