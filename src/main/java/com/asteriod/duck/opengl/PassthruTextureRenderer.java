package com.asteriod.duck.opengl;

import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteriod.duck.opengl.util.resources.shader.vars.Variable;
import com.asteriod.duck.opengl.util.resources.shader.vars.VariableType;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F5;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;
import static org.lwjgl.system.MemoryUtil.NULL;

public class PassthruTextureRenderer implements RenderedItem {



	private ShaderProgram shaderProgram = null;

	private final AtomicBoolean shaderDispose = new AtomicBoolean(false);

	private int vbo;
	private int ibo;
	private int vao;
	private final String textureName;

	public PassthruTextureRenderer(String textureName) {
		this.textureName = textureName;
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.registerKeyAction(GLFW_KEY_F5, () -> shaderDispose.set(true));
		initShaderProgram(ctx);
		initTextures(ctx);
		initBuffers();
	}

	private void initTextures(RenderContext ctx) {
		Texture texture = ctx.getResourceManager().GetTexture(textureName);
		TextureUnit textureUnit = TextureUnit.index(0);
		shaderProgram.use();
		textureUnit.bind(texture);
		textureUnit.useInShader(shaderProgram, "tex");
	}


	private void initBuffers() {

		// Define the vertices of the rectangle
		float[] vertices = {
						-1.0f, -1.0f, // bottom left
						1.0f, -1.0f, // bottom right
						1.0f, 1.0f, // top right
						-1.0f, 1.0f // top left
		};

		short[] indices = new short[]{0, 1, 2, 0, 2, 3};

		try(MemoryStack stack = MemoryStack.stackPush()) {
			vao = glGenVertexArrays();
			glBindVertexArray(vao);

			// Create a VBO and bind it
			vbo = glGenBuffers();
			glBindBuffer(GL_ARRAY_BUFFER, vbo);

			// Store the vertex data in the VBO
			FloatBuffer vertexBuffer = stack.floats(vertices);
			glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

			// Create an IBO and bind it
			ibo = glGenBuffers();
			glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

			// Store the index data in the IBO - create two triangles
			ShortBuffer indexBuffer = stack.shorts(indices);
			glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
		}
	}


	public void initShaderProgram(RenderContext ctx) throws IOException {
		if (shaderProgram != null) {
			shaderProgram.destroy();
			System.out.println("Shader disposed");
		}
		// load the GLSL Shaders
		this.shaderProgram =ctx.getResourceManager().GetShader("passthru", "passthru/vertex.glsl", "passthru/frag.glsl", null);
		System.out.println("PassThru shaders loaded:"+shaderProgram.toString());
	}

	@Override
	public void doRender(RenderContext ctx) {

		if (shaderDispose.get()) {
			try {
				initShaderProgram(ctx);
				shaderDispose.set(false);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (shaderProgram != null && shaderProgram.id() > NULL) {
			shaderProgram.use();
			shaderProgram.setVertexAttribPointer("position", 2, GL_FLOAT, false, 0, 0);
		}

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);

		glUseProgram(0);
	}

	@Override
	public void dispose() {

	}
}
