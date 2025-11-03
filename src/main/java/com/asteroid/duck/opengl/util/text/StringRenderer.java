package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElementType;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector2f;

import java.io.IOException;

public class StringRenderer implements RenderedItem {
	private FontTexture font;
	private Vector2f position;
	private String text;
	private VertexDataBuffer buffer;
	private ShaderProgram shaderProgram;
	public StringRenderer(FontTexture font, String text) {
    this.font = font;
    this.text = text;
		VertexDataStructure structure = new VertexDataStructure(
						new VertexElement(VertexElementType.VEC_2F, "position"),
						new VertexElement(VertexElementType.VEC_2F, "texCoords"),
						new VertexElement(VertexElementType.VEC_3F, "color"));
		this.buffer = new VertexDataBuffer(structure, 100);
  }

	public String getText() {
		return text;
	}

	public void setText(String text) {
    this.text = text;
  }

	public Vector2f getPosition() {
		return position;
	}

	public void setPosition(Vector2f position) {
    this.position = position;
  }


	@Override
	public void init(RenderContext ctx) throws IOException {
		this.shaderProgram = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("text");
		buffer.init();
		buffer.setup(shaderProgram);
	}

	@Override
	public void doRender(RenderContext ctx) {

	}

	@Override
	public void dispose() {
		shaderProgram.destroy();
		buffer.destroy();
	}
}
