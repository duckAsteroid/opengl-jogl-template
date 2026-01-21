package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VertexBufferVisualiser;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
import org.joml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;

public class TextExperiment extends CompositeRenderItem implements Experiment {
	private static final Logger LOG = LoggerFactory.getLogger(TextExperiment.class);
	private FontTexture fontTexture;
	private final VertexArrayObject vao = new VertexArrayObject();
	private VertexBufferObject fontDataBuffer;
	private ElementBufferObject elementBufferObject;
	private ShaderProgram shader;
	private final Vector4f backgroundColor = StandardColors.BLACK.color;
	private final VertexElement screenPosition = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
	private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

	private final PassthruTextureRenderer backgroundTexture = new PassthruTextureRenderer("test-card");

	private final VertexArrayObject debugVao = new VertexArrayObject();
	private ShaderProgram debugShader;
	private VertexBufferObject debugLineBuffer;


	@Override
	public String getDescription() {
		return "Attempts to render a single character to the screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(false);
		//ctx.setBackgroundColor(backgroundColor);
		ctx.getResourceManager().getTexture("test-card", "test-card.jpeg");
		backgroundTexture.init(ctx);
		this.shader = ctx.getResourceManager().getSimpleShader("passthru2");
		this.debugShader = ctx.getResourceManager().getSimpleShader("line");

		// create a texture for our font
		var ftf = new FontTextureFactory(new Font("Times New Roman", Font.PLAIN,100), true);
		fontTexture = ftf.createFontTexture();
		var tex = fontTexture.getTexture();
		TextureUnit textureUnit = ctx.getResourceManager().nextTextureUnit();
		textureUnit.bind(tex);

		initText(ctx, new Point(10, 200), "Hello World!");
		// setup the shader
		shader.use(ctx);
		textureUnit.useInShader(shader, "tex");

		// put the ortho matrix into the shader
		Matrix4f ortho = ctx.ortho();
		shader.uniforms().get("projection", Matrix4f.class).set(ortho);
		// set the text color for the shader
		shader.uniforms().get("textColor", Vector4f.class).set(StandardColors.LIGHTBLUE.color);

		// setup the debug shader
		debugShader.use(ctx);
		debugShader.uniforms().get("projection", Matrix4f.class).set(ortho);
		debugShader.uniforms().get("lineColor", Vector4f.class).set(StandardColors.REBECCAPURPLE.color);
	}

	protected void initText(RenderContext ctx, Point cursor, String text) {
		vao.init(ctx);
		// create vertex data structure to render a part of the font texture to screen
		// Four vertices - we will convert to two triangles via the index buffer
		var fourCorners = Vertice.standardFourVertices().toList();
		int size = fourCorners.size() * text.length();
		// create an index buffer to point at the vertices of the triangles
		int[] indices = Vertice.standardSixVertices().mapToInt(fourCorners::indexOf).toArray();
		this.elementBufferObject = vao.createEbo(indices.length * text.length());
		elementBufferObject.init(ctx);
		elementBufferObject.clear();
		// vertex data structure to hold the screen position and texture position
		VertexDataStructure structure = new VertexDataStructure(screenPosition, texturePosition);
		this.fontDataBuffer = vao.createVbo(structure, size);
		fontDataBuffer.init(ctx);
		shader.use(ctx);
		fontDataBuffer.setup(shader);

		// the screen size
		float width = ctx.getWindow().width;
		float height = ctx.getWindow().height;
		// vertex data elements for the debug lines (colr, start, end)
		VertexElement position = new VertexElement(VertexElementType.VEC_2F, "position");
		// create a vertex data buffer to hold the debug lines
		debugVao.init(ctx);
		VertexDataStructure debugLineStructure = new VertexDataStructure(position);
		debugLineBuffer = debugVao.createVbo(debugLineStructure, 2 + (text.length() * 2));
		debugLineBuffer.init(ctx);
		// add the baseline
		int debugIndex = 0;
		debugLineBuffer.setElement(debugIndex++, position, new Vector2f(0f, cursor.y));
		debugLineBuffer.setElement(debugIndex++, position, new Vector2f(width, cursor.y));
		debugShader.use(ctx);
		debugLineBuffer.setup(debugShader);

		for(int i = 0; i < text.length(); i++) {
			var c = text.charAt(i);
			// lets try to draw a glyph with it's datum on a given screen position
			var glyph = fontTexture.getGlyph(c);

			// the bounds of the glyph in texture coordinates
			Vector4f texture = glyph.normalBounds();
			// the screen bounds (where to draw the glyph)
			final var screen = glyph.rawBounds(cursor);
			// add the debug line for the glyph datum
			final Vector2f datum = glyph.datum(cursor);

			if (LOG.isTraceEnabled()) {
				LOG.trace("glyph[{}]: {} = glyph {} @ datum {}",i, c, glyph, datum);
			}

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
				elementBufferObject.put((short)((i * 4) + indices[j]));
			}
			// advance the cursor for the next glyph
			cursor.x += glyph.advance();
		}
		debugVao.setDrawMode(BufferDrawMode.LINES);
		debugLineBuffer.update(UpdateHint.DYNAMIC);

		vao.bind(ctx);
		fontDataBuffer.update(UpdateHint.DYNAMIC);

		elementBufferObject.update();

		dumpBuffers();
	}

	private void dumpBuffers() {
		System.out.println("Font Data Buffer:");
		System.out.println(new VertexBufferVisualiser(vao));
		System.out.println("Debug Data Buffer:");
		System.out.println(new VertexBufferVisualiser(debugVao));
	}

	@Override
	public void doRender(RenderContext ctx) {
		backgroundTexture.doRender(ctx);

		debugShader.use(ctx);
		debugVao.bind(ctx);
		debugVao.doRender(ctx);

		shader.use(ctx);
		vao.bind(ctx);
		vao.doRender(ctx);

	}

	@Override
	public void dispose() {
		shader.dispose();
		fontDataBuffer.dispose();

		debugShader.dispose();
		debugLineBuffer.dispose();
	}
}
