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
import org.joml.Matrix2f;
import org.joml.Matrix3f;
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
	private final Vector4f color = new Vector4f(0,1,0,1); // green!

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
		shader.setVector4f("spriteColor", new Vector4f(color));
		fontDataBuffer.render(0, 6);
		shader.unuse();
	}

	private void createStringData(RenderContext ctx) {
		char c = source.charAt(index);
		Glyph glyph = fontTexture.getGlyph(c);
		Matrix2f transform = fontTexture.getTexture().normalisationMatrix();
		Vector2f tex_pos = transform.transform(glyph.position());
		Vector2f tex_size = transform.transform(glyph.dimension());

		Vector2f dims = ctx.getWindowDimensions();
		Vector2f screen_pos = new Vector2f(0.0f, 0.0f);
		Vector2f screen_size = new Vector2f(0.5f,0.5f);

		fontDataBuffer.clear();

		// two triangles
		fontDataBuffer.set(0, screen_pos, tex_pos, color);
		fontDataBuffer.set(1, new Vector2f(screen_pos.x,screen_size.y), new Vector2f(tex_pos.x,tex_size.y), color);
		fontDataBuffer.set(2, new Vector2f(screen_size.x,screen_pos.y), new Vector2f(tex_size.x,tex_pos.y), color);

		fontDataBuffer.set(3, new Vector2f(screen_pos.x,screen_size.y), new Vector2f(tex_pos.x,tex_size.y), color);
		fontDataBuffer.set(4, new Vector2f(screen_size.x,screen_pos.y), new Vector2f(tex_size.x,tex_pos.y), color);
		fontDataBuffer.set(5, screen_size, tex_size, color);
	}

	@Override
	public void dispose() {
		shader.destroy();
		fontDataBuffer.destroy();
		fontTexture.destroy();
	}
}
