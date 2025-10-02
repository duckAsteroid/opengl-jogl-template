package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.buffer.*;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VdbVisualizer;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TextExperiment extends CompositeRenderItem implements Experiment {

	private FontTexture fontTexture;
	private VertexDataBuffer fontDataBuffer;
	private IndexBuffer indexBuffer;
	private ShaderProgram shader;
	private final Vector4f backgroundColor = StandardColors.BLACK.color;
	private final VertexElement screenPosition = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
	private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

	private final PassthruTextureRenderer backgroundTexture = new PassthruTextureRenderer("test-card");
	private ShaderProgram debugShader;
	private VertexDataBuffer debugLineBuffer;


	@Override
	public String getDescription() {
		return "Attempts to render a single character to the screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		//ctx.setClearScreen(true);
		//ctx.setBackgroundColor(backgroundColor);
		ctx.getResourceManager().GetTexture("test-card", "test-card.jpeg");
		backgroundTexture.init(ctx);
		this.shader = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("passthru2");
		this.debugShader = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("line");

		// create a texture for our font
		var ftf = new FontTextureFactory(new Font("Times New Roman", Font.PLAIN,100), true);
		fontTexture = ftf.createFontTexture();
		var tex = fontTexture.getTexture();
		TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
		textureUnit.bind(tex);

		initText(ctx, new Point(10, 200), "Hello World!");
		// setup the shader
		shader.use();
		textureUnit.useInShader(shader, "tex");

		// put the ortho matrix into the shader
		Matrix4f ortho = ctx.ortho();
		shader.uniforms().get("projection", Matrix4f.class).set(ortho);
		// set the text color for the shader
		shader.uniforms().get("textColor", Vector4f.class).set(StandardColors.LIGHTBLUE.color);

		// setup the debug shader
		debugShader.use();
		debugShader.uniforms().get("projection", Matrix4f.class).set(ortho);
		debugShader.uniforms().get("lineColor", Vector4f.class).set(StandardColors.REBECCAPURPLE.color);
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
		fontDataBuffer.setup(shader);

		// the screen size
		float width = ctx.getWindow().width;
		float height = ctx.getWindow().height;
		// vertex data elements for the debug lines (colr, start, end)
		VertexElement position = new VertexElement(VertexElementType.VEC_2F, "position");
		// create a vertex data buffer to hold the debug lines
		VertexDataStructure debugLineStructure = new VertexDataStructure(position);
		debugLineBuffer = new VertexDataBuffer(debugLineStructure, 2 + (text.length() * 2));
		debugLineBuffer.init(ctx);
		// add the baseline
		int debugIndex = 0;
		debugLineBuffer.setElement(debugIndex++, position, new Vector2f(0f, cursor.y));
		debugLineBuffer.setElement(debugIndex++, position, new Vector2f(width, cursor.y));
		debugLineBuffer.setup(debugShader);

		for(int i = 0; i < text.length(); i++) {
			// lets try to draw a glyph with it's datum on a given screen position
			var glyph = fontTexture.getGlyph(text.charAt(i));
			System.out.println("Glyph: " + glyph);
			// the bounds of the glyph in texture coordinates
			Vector4f texture = glyph.normalBounds();
			// the screen bounds (where to draw the glyph)
			final var screen = glyph.rawBounds(cursor);
			// add the debug line for the glyph datum
			final Vector2f datum = glyph.datum(cursor);
			System.out.println("Datum: " + datum);
			debugLineBuffer.setElement(debugIndex++, position, new Vector2f(datum.x, 0f));
			debugLineBuffer.setElement(debugIndex++, position, new Vector2f(datum.x, height));
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
				indexBuffer.put((i * 4) + indices[j]);
			}
			// advance the cursor for the next glyph
			cursor.x += glyph.advance();
		}
		debugLineBuffer.use();
		debugLineBuffer.update(VertexDataBuffer.UpdateHint.DYNAMIC);

		fontDataBuffer.use();
		fontDataBuffer.update(VertexDataBuffer.UpdateHint.DYNAMIC);

		indexBuffer.use();
		indexBuffer.update();
		indexBuffer.unuse();

		dumpBuffers();
	}

	private void dumpBuffers() {
		System.out.println("Index Buffer:");
		IntStream ib = indexBuffer.stream();
		System.out.println("\t"+ib.mapToObj(Integer::toString).collect(Collectors.joining(",", "[", "]")));

		System.out.println("Font Data Buffer:");
		VdbVisualizer viz = new VdbVisualizer(fontDataBuffer);
		System.out.println("\t"+viz.verticeString());
		System.out.println("\t"+viz.headerString());
		System.out.println("\t"+viz.dataString());
		System.out.println("\t"+viz.byteString());

		System.out.println("Debug Line Buffer:");
		VdbVisualizer debug = new VdbVisualizer(this.debugLineBuffer);
		System.out.println("\t"+debug.verticeString());
		System.out.println("\t"+debug.headerString());
		System.out.println("\t"+debug.dataString());
		System.out.println("\t"+debug.byteString());
	}

	@Override
	public void doRender(RenderContext ctx) {
		backgroundTexture.doRender(ctx);

		debugShader.use();
		debugLineBuffer.use();
		GL11C.glDrawArrays(GL11C.GL_LINES, 0, debugLineBuffer.size());
		debugLineBuffer.unuse();
		debugShader.unuse();

		shader.use();
		fontDataBuffer.use();
		indexBuffer.use();

		GL11C.glDrawElements(GL11C.GL_TRIANGLES, indexBuffer.capacity(), IndexBuffer.GL_TYPE, 0L);

		indexBuffer.unuse();
		fontDataBuffer.unuse();
		shader.unuse();
	}

	@Override
	public void dispose() {
		shader.destroy();
		fontDataBuffer.destroy();

		debugShader.destroy();
		debugLineBuffer.destroy();
	}
}
