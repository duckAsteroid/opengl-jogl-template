package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElementType;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.resources.font.Glyph;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.awt.*;
import java.io.IOException;

public class TextExperiment extends CompositeRenderItem implements Experiment {
	private final String source = "ChrisSenior";
	private int index = 0;

	private FontTexture fontTexture;
	private VertexDataBuffer fontDataBuffer;
	private ShaderProgram shader;
	private final Vector4f color = new Vector4f(0,1,0,1);

	@Override
	public String getDescription() {
		return "Attempts to render text to screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('X'), this::nextChar, "Render next char in source string");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('Z'), this::prevChar, "Render previous char in source string");

		this.shader = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("text");

		// create a texture for our font
		Font font = new Font(Font.MONOSPACED, Font.PLAIN, 18);
		FontTextureFactory fac = new FontTextureFactory(font, true);
		this.fontTexture = fac.createFontTexture();
		TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
		textureUnit.bind(fontTexture.getTexture());


		// create vertex data structure to render a part of the font texture to screen
		VertexDataStructure structure = new VertexDataStructure(
						new VertexElement(VertexElementType.VEC_2F, "position"),
						new VertexElement(VertexElementType.VEC_2F, "texturePosition"),
						new VertexElement(VertexElementType.VEC_4F, "color"));
		// two triangles = one rect
		this.fontDataBuffer = new VertexDataBuffer(structure, 6);
		fontDataBuffer.init(ctx);


		// setup the shader
		shader.use();
		fontDataBuffer.setup(shader);
		textureUnit.useInShader(shader, "image");
	}

	private void nextChar() {
		index++;
		if (index >= source.length()) {
			index = 0;
		}
	}

	private void prevChar() {
		index--;
		if (index < 0) {
			index = source.length() - 1;
		}
	}

	@Override
	public void doRender(RenderContext ctx) {
		createStringData(ctx);
		shader.use();
		fontDataBuffer.render(0, 6);
		shader.unuse();
	}

	private void createStringData(RenderContext ctx) {
		Glyph glyph = fontTexture.getGlyph(source.charAt(index));
		Vector4f e = fontTexture.getGlyphTextureExtent(glyph);
		Vector2f dims = ctx.getWindowDimensions();
		Vector2f g = glyph.dimension().div(dims);

		fontDataBuffer.clear();
		// screen position of coord
		fontDataBuffer.set(0, new Vector2f(0,0), new Vector2f(0,0), color);
		fontDataBuffer.set(1, new Vector2f(0,g.y), new Vector2f(0,e.y), color);
		fontDataBuffer.set(2, new Vector2f(g.x,0), new Vector2f(e.x,0), color);

		fontDataBuffer.set(3, new Vector2f(g.x,0), new Vector2f(e.x,0), color);
		fontDataBuffer.set(4, new Vector2f(0,g.y), new Vector2f(0,e.y), color);
		fontDataBuffer.set(5, new Vector2f(g.x,g.y), new Vector2f(e.x,e.y), color);
	}

	@Override
	public void dispose() {
		shader.destroy();
		fontDataBuffer.destroy();
		fontTexture.destroy();
	}
}
