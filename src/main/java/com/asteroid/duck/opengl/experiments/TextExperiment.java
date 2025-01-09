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
import com.asteroid.duck.opengl.util.resources.font.GlyphData;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.*;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public class TextExperiment extends CompositeRenderItem implements Experiment {
	private int index = 0;

	private FontTexture fontTexture;
	private List<GlyphData> glyphs;
	private VertexDataBuffer fontDataBuffer;
	private ShaderProgram shader;
	private final Vector4f color = new Vector4f(1,1,1,1); // green!
	private final Vector4f backgroundColor = new Vector4f(.4f,0.2f,0f,1f);

	@Override
	public String getDescription() {
		return "Attempts to render text to screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(true);
		ctx.setBackgroundColor(backgroundColor);
		this.shader = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("text");

		// create a texture for our font
		Font font = new Font(Font.SERIF, Font.PLAIN, 100);
		FontTextureFactory fac = new FontTextureFactory(font, true);
		this.fontTexture = fac.createFontTexture();
		this.glyphs = fontTexture.glyphs();
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

	@Override
	public void doRender(RenderContext ctx) {
		createStringData(ctx);
		shader.use();
		shader.setVector4f("spriteColor", color);
		fontDataBuffer.render(0, 6);
		shader.unuse();
	}

	private final String TEXT = "ijqtQfyY";

	private void createStringData(RenderContext ctx) {
		// where on the screen to draw the char
		final Vector2f position = new Vector2f(150,150);
		final float scale = 1.2f;

		int index = (int) ctx.getTimer().linearFunction(TEXT.length(), 0.1);
		GlyphData glyph = fontTexture.getGlyph(TEXT.charAt(index));
		// figure out the area of the texture representing this char
		Matrix2f transform = fontTexture.getTexture().normalisationMatrix();
		Vector4f tex_extent = glyph.extent(transform);

		// now work out the area of the screen to draw the char onto
		Matrix4f normalisation = ctx.ortho();
		Vector4f screen_pos = new Vector4f(position, 0, 1.0f).mul(normalisation);
		Vector2f tex_size_model = new Vector2f(0).add(position).mul(scale);
		Vector4f screen_size = new Vector4f(tex_size_model.x, tex_size_model.y, 0, 1.0f).mul(normalisation);
		Vector4f screen_extent = new Vector4f(screen_pos.x, screen_size.y, screen_size.x, screen_pos.y);

		fontDataBuffer.clear();
		Corner[] triangles = new Corner[]{
						Corner.BOTTOM_LEFT, Corner.TOP_LEFT, Corner.TOP_RIGHT,
						Corner.BOTTOM_LEFT, Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT
		};

		// two triangles
		for (int i = 0; i < triangles.length; i++) {
			// position(2), texturePosition(2), color(4)
			Corner vertice = triangles[i];
			fontDataBuffer.set(i, vertice.from(screen_extent), vertice.from(tex_extent), color);
		}
		fontDataBuffer.update();
	}

	private enum H implements Function<Vector4f, Float> {
		LEFT,
		RIGHT;

		@Override
		public Float apply(Vector4f vector4f) {
			return switch(this) {
				case LEFT -> vector4f.x;
				case RIGHT -> vector4f.z;
			};
		}
	}

	private enum V implements Function<Vector4f, Float> {
		TOP, BOTTOM;

		@Override
		public Float apply(Vector4f vector4f) {
			return switch(this) {
				case TOP -> vector4f.w;
				case BOTTOM -> vector4f.y;
			};
		}
	}

	public enum Corner {
		TOP_LEFT(V.TOP, H.LEFT), TOP_RIGHT(V.TOP, H.RIGHT), BOTTOM_LEFT(V.BOTTOM, H.LEFT), BOTTOM_RIGHT(V.BOTTOM, H.RIGHT);

		private final V vertical;
		private final H horizontal;

		Corner(V vertical, H horizontal) {
			this.vertical = vertical;
			this.horizontal = horizontal;
		}

		public Vector2f from(Vector4f extent) {
			return new Vector2f(horizontal.apply(extent), vertical.apply(extent));
		}
	}

	@Override
	public void dispose() {
		shader.destroy();
		fontDataBuffer.destroy();
		fontTexture.destroy();
	}
}
