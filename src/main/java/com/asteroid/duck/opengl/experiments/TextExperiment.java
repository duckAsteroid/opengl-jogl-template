package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.resources.buffer.*;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.resources.font.GlyphData;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.*;
import org.lwjgl.opengl.GL11C;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TextExperiment extends CompositeRenderItem implements Experiment {

	private FontTexture fontTexture;
	private VertexDataBuffer fontDataBuffer;
	private IndexBuffer indexBuffer;
	private ShaderProgram shader;
	private final Vector4f backgroundColor = StandardColors.BLACK.get();
	private final VertexElement screenPosition = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
	private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

	@Override
	public String getDescription() {
		return "Attempts to render a single character to the screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(true);
		ctx.setBackgroundColor(backgroundColor);
		this.shader = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("passthru2");

		// create a texture for our font
		var ftf = new FontTextureFactory(new Font("Times New Roman", Font.PLAIN,200), true);
		fontTexture = ftf.createFontTexture();
		var tex = fontTexture.getTexture();
		TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
		textureUnit.bind(tex);

		// create vertex data structure to render a part of the font texture to screen
		List<Vertice> vertices = Vertice.standardFourVertices().toList();
		VertexDataStructure structure = new VertexDataStructure(screenPosition, texturePosition);
		final Vector4f screen = new Vector4f(-1f,-1f,1f,1f);
		var glyph = fontTexture.getGlyph('C');
		Vector4f texture = glyph.normalBounds(tex.dimensions());
		//texture = new Vector4f(0.05f,1f,.1f,0f);
		// two triangles = one rect
		this.fontDataBuffer = new VertexDataBuffer(structure, vertices.size());
		fontDataBuffer.init(ctx);
		for (int i = 0; i < vertices.size(); i++) {
			Vertice v =  vertices.get(i);
			fontDataBuffer.setElement(i, screenPosition, v.from(screen));
			fontDataBuffer.setElement(i, texturePosition, v.from(texture));
		}
		// FIXME Add a dirty flag to warn if we don't flush the updated data to the GPU and we "use" this buffer
		fontDataBuffer.update(VertexDataBuffer.UpdateHint.STATIC);

		// create an index buffer to point at the vertices
		List<Integer> indices = Vertice.standardSixVertices().map(vertices::indexOf).toList();
		this.indexBuffer = new IndexBuffer(indices.size());
		indexBuffer.init(ctx);
		indexBuffer.update(indices);

		// setup the shader
		shader.use();
		fontDataBuffer.setup(shader);
		textureUnit.useInShader(shader, "tex");
	}

	@Override
	public void doRender(RenderContext ctx) {
		shader.use();
		fontDataBuffer.use();
		indexBuffer.use();

		GL11C.glDrawElements(GL11C.GL_TRIANGLES, indexBuffer.capacity(), IndexBuffer.GL_TYPE, 0L);

		shader.unuse();
	}

	@Override
	public void dispose() {
		shader.destroy();
		fontDataBuffer.destroy();
	}
}
