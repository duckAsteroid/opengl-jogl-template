package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.geom.Corner;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElementType;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.resources.font.GlyphData;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class TextExperiment extends CompositeRenderItem implements Experiment {
	private int index = 0;

	private FontTexture fontTexture;
	private List<GlyphData> glyphs;
	private VertexDataBuffer fontDataBuffer;
	private ShaderProgram shader;
	private final Vector4f color = new Vector4f(1,1,1,1); // green!
	private final Vector4f backgroundColor = new Vector4f(.2f,0.2f,0.2f,1f);
	private final int maxStringLength = 20;
	private final String TEXT = "ijqtQfyY";
	private final VertexElement screenPosition = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
	private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

	@Override
	public String getDescription() {
		return "Attempts to render text to screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(true);
		ctx.setBackgroundColor(backgroundColor);
		this.shader = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("passthru2");

		// create a texture for our font
		Font font = new Font(Font.SERIF, Font.PLAIN, 100);
		FontTextureFactory fac = new FontTextureFactory(font, true);
		fac.debugBackground = true;
		fac.imageDumpPath = Path.of("font-images");
		this.fontTexture = fac.createFontTexture();
		this.glyphs = fontTexture.glyphs();
		TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
		textureUnit.bind(fontTexture.getTexture());


		// create vertex data structure to render a part of the font texture to screen
		VertexDataStructure structure = new VertexDataStructure(screenPosition, texturePosition);
		// two triangles = one rect
		this.fontDataBuffer = new VertexDataBuffer(structure, 3 * 2 * TEXT.length());
		fontDataBuffer.init(ctx);

		// setup the shader
		shader.use();
		fontDataBuffer.setup(shader);
		textureUnit.useInShader(shader, "tex");
	}

	@Override
	public void doRender(RenderContext ctx) {
		createStringData(ctx);
		shader.use();
		fontDataBuffer.render(0, 6);
		shader.unuse();
	}



	private void createStringData(RenderContext ctx) {
		// where on the screen to draw the char
		final Vector2f position = new Vector2f(150,150);
		final float scale = 1.0f;

		int index = (int) ctx.getTimer().linearFunction(TEXT.length(), 0.1);
		char c = TEXT.charAt(index);
		System.out.println("Char:"+c);
		GlyphData glyph = fontTexture.getGlyph(c);
		// figure out the area of the font strip texture representing this char
		Matrix3x2f transform = fontTexture.getTexture().normalisationMatrix();
		Vector4f tex_extent = new Vector4f(0);// glyph.bounds(transform);

		// now work out the area of the screen to draw the char onto
		Matrix4f normalisation = ctx.ortho();
		Vector4f screen_pos = new Vector4f(position, 0, 1.0f).mul(normalisation);
		Vector2f tex_size_model = new Vector2f(0).add(position).mul(scale);
		Vector4f screen_size = new Vector4f(tex_size_model.x, tex_size_model.y, 0, 1.0f).mul(normalisation);
		Vector4f screen_extent = new Vector4f(screen_pos.x, screen_size.y, screen_size.x, screen_pos.y);

		fontDataBuffer.clear();

		List<Map<VertexElement, Vector2f>> list = Corner.standardSixVertices()
						.map(corner -> Map.of(
										screenPosition, corner.from(screen_extent),
										texturePosition, corner.from(tex_extent)))
						.toList();
		for (int i = 0; i < list.size(); i++) {
			Map<VertexElement, Vector2f> map = list.get(i);
			fontDataBuffer.set(i, map);
		}
		fontDataBuffer.update(VertexDataBuffer.UpdateHint.STREAM);
	}

	@Override
	public void dispose() {
		shader.destroy();
		fontDataBuffer.destroy();
		fontTexture.destroy();
	}
}
