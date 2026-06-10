package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.text.StringRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;

public class TextExperiment implements Experiment {
	private static final Logger LOG = LoggerFactory.getLogger(TextExperiment.class);

	private final PassthruTextureRenderer backgroundTexture = new PassthruTextureRenderer("test-card");
	private StringRenderer stringRenderer;

	@Override
	public String getDescription() {
		return "Attempts to render a single character string to the screen";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(false);
		// Font texture must be created before backgroundTexture.init — Texture.generate() binds/unbinds
		// on the currently active texture unit, and backgroundTexture.init leaves GL_TEXTURE1 active.
		// Creating the font here (while GL_TEXTURE0 is still the default) avoids clobbering unit 1.
		FontTexture fontTexture = new FontTextureFactory(new Font("Times New Roman", Font.PLAIN, 100), true)
				.createFontTexture();

		ctx.getResourceManager().getTexture("test-card", "test-card.jpeg");
		backgroundTexture.init(ctx);
		stringRenderer = new StringRenderer(fontTexture);
		stringRenderer.init(ctx);
		stringRenderer.setPosition(new Point(10, 200));
		stringRenderer.setTextColor(StandardColors.BLACK.withAlpha(0.7f));
		stringRenderer.setText("Hello World!");
	}

	@Override
	public void doRender(RenderContext ctx) {
		backgroundTexture.doRender(ctx);
		stringRenderer.doRender(ctx);
	}

	@Override
	public void dispose() {
		backgroundTexture.dispose();
		stringRenderer.dispose();
	}
}
