package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.buffer.*;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.*;
import org.lwjgl.opengl.GL11C;

import java.awt.*;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;
import java.util.stream.Stream;

public class TextExperiment extends CompositeRenderItem implements Experiment {

	private FontTexture fontTexture;
	private VertexDataBuffer fontDataBuffer;
	private IndexBuffer indexBuffer;
	private ShaderProgram shader;
	private final Vector4f backgroundColor = StandardColors.REBECCAPURPLE.color;
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
		var ftf = new FontTextureFactory(new Font("Times New Roman", Font.PLAIN,100), true);
		fontTexture = ftf.createFontTexture();
		var tex = fontTexture.getTexture();
		TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
		textureUnit.bind(tex);

		initText(ctx, new Point(10, 200), "Hello Alice! xx");
		// setup the shader
		shader.use();
		fontDataBuffer.setup(shader);
		textureUnit.useInShader(shader, "tex");

		// put the ortho matrix into the shader
		Matrix4f ortho = ctx.ortho();
		shader.setMatrix4f("projection", ortho);
		// set the text color for the shader
		shader.setVector4f("textColor", new Vector4f(0f, 0f, 1f, 1f));
	}

	protected void initText(RenderContext ctx, Point cursor, String text) {
		// create vertex data structure to render a part of the font texture to screen
		// Four vertices - we will convert to two triangles via the index buffer
		var fourCorners = Vertice.standardFourVertices().toList();
		int size = fourCorners.size() * text.length();
		// create an index buffer to point at the vertices of the triangles
		int[] indices = Vertice.standardSixVertices().mapToInt(fourCorners::indexOf).toArray();
		this.indexBuffer = new IndexBuffer(indices.length * text.length());
		indexBuffer.init(ctx);
		indexBuffer.clear();
		// vertex data structure to hold the screen position and texture position
		VertexDataStructure structure = new VertexDataStructure(screenPosition, texturePosition);
		this.fontDataBuffer = new VertexDataBuffer(structure, size);
		fontDataBuffer.init(ctx);
		for(int i = 0; i < text.length(); i++) {
			// lets try to draw a glyph with it's datum on a given screen position
			var glyph = fontTexture.getGlyph(text.charAt(i));
			System.out.println("Glyph: " + glyph);
			// the bounds of the glyph in texture coordinates
			Vector4f texture = glyph.normalBounds();
			// the screen bounds (where to draw the glyph)
			final var screen = glyph.rawBounds(cursor);
			// populate the vertex data buffer with the screen and texture positions of each vertice
			for(int j = 0; j < fourCorners.size(); j++) {
				Vertice v = fourCorners.get(j);
				// create an index buffer to point at the vertices of the triangles
				// put the screen position in the vertex data element
				fontDataBuffer.setElement((i * 4) + j, screenPosition, v.from(screen));
				// put the texture position in the vertex data element
				fontDataBuffer.setElement((i * 4) + j, texturePosition, v.from(texture));
			}
			// add the indices for this glyph to the index buffer
			for(int j = 0; j < indices.length; j++) {
				indexBuffer.put((i * 6) + indices[j]);
			}
			// advance the cursor for the next glyph
			cursor.x += glyph.advance();
		}
		// FIXME Add a dirty flag to warn if we don't flush the updated data to the GPU and we "use" this buffer
		fontDataBuffer.update(VertexDataBuffer.UpdateHint.DYNAMIC);
		indexBuffer.use();
		indexBuffer.update();

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
