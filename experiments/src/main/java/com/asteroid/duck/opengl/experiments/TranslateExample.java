package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.resources.texture.*;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import com.asteroid.duck.opengl.util.toggle.Frequency;
import com.asteroid.duck.opengl.util.toggle.ToggledRenderItem;

import java.io.IOException;

public class TranslateExample extends CompositeRenderItem implements Experiment {

	@Override
	public String getDescription() {
		return "Just uses a translate map shader on a picture";
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		ctx.setClearScreen(true);
		ctx.setDesiredUpdatePeriod(1.0 / 35.0);
		ctx.getResourceManager().getTexture("testcard", "test-card.jpeg", ImageLoadingOptions.DEFAULT);
		ctx.getResourceManager().getTexture("translate", "translate/bighalfwheel.1024x800.tab",
				ImageLoadingOptions.DEFAULT.withType(DataFormat.TWO_CHANNEL_16_BIT));

		RenderedItem source = new ToggledRenderItem(new Frequency(1, 0.1d), new PassthruTextureRenderer("testcard"));
		add(new MapTransformRenderItem(source, "translate", "yabadabado",
				new TextureOptions(DataFormat.GRAY, Filter.LINEAR, Wrap.REPEAT)));

		super.init(ctx);
	}
}
