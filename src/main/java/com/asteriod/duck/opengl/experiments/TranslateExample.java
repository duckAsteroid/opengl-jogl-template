package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.PassthruTextureRenderer;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.resources.texture.*;

import java.awt.*;
import java.io.IOException;

public class TranslateExample extends CompositeRenderItem implements Experiment {
	@Override
	public String getDescription() {
		return "Just uses a translate map shader on a picture";
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		Rectangle screen = ctx.getWindow();
		// load the test card image
		Texture texture = ctx.getResourceManager().GetTexture("texture", "test-card.jpeg", ImageOptions.DEFAULT);
		// load the translation map - it's a matrix (screen sized) of 2 * 16 bit floats
		Texture translateMap = ctx.getResourceManager().GetTexture("translate", "translate/bighalfwheel.1024x800.tab", ImageOptions.DEFAULT.withType(DataFormat.TWO_CHANNEL_16_BIT));

		// setup a renderer to use the translate shader
		PassthruTextureRenderer renderer = new PassthruTextureRenderer("texture", "translate", shaderProgram -> {
			// let the shader know about the map as a texture
			shaderProgram.use();
			Texture map = ctx.getResourceManager().GetTexture("translate");
			TextureUnit textureUnit = ctx.getResourceManager().NextTextureUnit();
			textureUnit.bind(map);
			textureUnit.useInShader(shaderProgram, "map");
		});
		addItem(renderer);
		super.init(ctx);
	}
}
