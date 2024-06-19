package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.OffscreenTextureRenderer;
import com.asteriod.duck.opengl.PaletteRenderer;
import com.asteriod.duck.opengl.PassthruTextureRenderer;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.resources.texture.ImageOptions;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureFactory;
import com.asteriod.duck.opengl.util.resources.texture.Type;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.opengl.GL11C.GL_RED;
import static org.lwjgl.opengl.GL30C.GL_R32F;

public class TranslateExample extends CompositeRenderItem implements Experiment {
	@Override
	public String getDescription() {
		return "Just uses a translate map shader on a picture";
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		Rectangle screen = ctx.getWindow();
		// load the test card as a grey scale image (32F per pixel)
		Texture texture = ctx.getResourceManager().GetTexture("texture", "test-card.jpeg", ImageOptions.DEFAULT.withType(Type.GRAY));

		PassthruTextureRenderer renderer = new PassthruTextureRenderer("texture", "passthru-mono");

		Texture offscreen =  new Texture();
		offscreen.setFilter(Texture.Filter.LINEAR);
		offscreen.setWrap(Texture.Wrap.CLAMP_TO_EDGE);
		offscreen.setInternalFormat(GL_R32F);
		offscreen.setImageFormat(GL_RED);
		offscreen.Generate(screen.width, screen.height, 0);
		ctx.getResourceManager().PutTexture("offscreen", offscreen);

		OffscreenTextureRenderer translateRenderStage = new OffscreenTextureRenderer(renderer, offscreen);

		Texture palette = TextureFactory.createTexture(ImageOptions.DEFAULT.withSingleLine(), PaletteRenderer.rbgTestScale());
		ctx.getResourceManager().PutTexture("palette", palette);
		PaletteRenderer paletteRenderer = new PaletteRenderer("offscreen");

		addItems(translateRenderStage, paletteRenderer);
		super.init(ctx);
	}
}
