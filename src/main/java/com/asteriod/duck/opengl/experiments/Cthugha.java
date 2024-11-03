package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.*;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.blur.BlurKernel;
import com.asteriod.duck.opengl.util.blur.DiscreteSampleKernel;
import com.asteriod.duck.opengl.util.resources.texture.ImageOptions;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureUnit;
import com.asteriod.duck.opengl.util.resources.texture.DataFormat;
import org.joml.Vector2f;

import java.awt.*;
import java.io.IOException;

import static org.lwjgl.opengl.GL11C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL30C.GL_R8UI;
import static org.lwjgl.opengl.GL30C.GL_RED_INTEGER;

public class Cthugha extends CompositeRenderItem implements Experiment {
	@Override
	public String getDescription() {
		return "An attempt to do 90% of Cthugha";
	}
	@Override
	public void init(RenderContext ctx) throws IOException {
		Rectangle screen = ctx.getWindow();
		Texture[] offscreen = new Texture[3];
		for (int i = 0; i < offscreen.length ; i++) {
			offscreen[i] = new Texture();
			offscreen[i].setInternalFormat(GL_R8UI);
			offscreen[i].setImageFormat(GL_RED_INTEGER);
			offscreen[i].setDataType(GL_UNSIGNED_BYTE);
			offscreen[i].Generate(screen.width, screen.height, 0);
			ctx.getResourceManager().PutTexture("offscreen"+i, offscreen[i]);
		}
		// translate map
		Texture translateMap = ctx.getResourceManager().GetTexture("translateMap", "translate/bighalfwheel.1024x800.tab", ImageOptions.DEFAULT.withType(DataFormat.TWO_CHANNEL_16_BIT).withNoFlip());
		TextureUnit translateMapUnit = ctx.getResourceManager().NextTextureUnit();
		translateMapUnit.bind(translateMap);
		PassthruTextureRenderer renderer = new PassthruTextureRenderer("offscreen2", "translate", shader -> {
			translateMapUnit.useInShader(shader, "map");
			shader.setVector2f("dimensions", new Vector2f(screen.width, screen.height));
		});
		OffscreenTextureRenderer translateRenderStage = new OffscreenTextureRenderer(renderer, offscreen[0]);

		// blur
		DiscreteSampleKernel blurKernel = new BlurKernel(21).getDiscreteSampleKernel();
		PassthruTextureRenderer blurX = new PassthruTextureRenderer("offscreen0", "blur-x", shader -> {
			shader.setFloatArray("offset", blurKernel.floatOffsets());
			shader.setFloatArray("weight", blurKernel.floatWeights());
		});
		OffscreenTextureRenderer blurXStage = new OffscreenTextureRenderer(blurX , offscreen[1]);
		PassthruTextureRenderer blurY = new PassthruTextureRenderer("offscreen1", "blur-y", shader -> {
			shader.setFloatArray("offset", blurKernel.floatOffsets());
			shader.setFloatArray("weight", blurKernel.floatWeights());
		});
		OffscreenTextureRenderer blurYStage = new OffscreenTextureRenderer(blurY , offscreen[2]);

		// wave
		Polyline polyline = new Polyline();
		polyline.setLineWidth(1.0f);
		OffscreenTextureRenderer waveRenderStage = new OffscreenTextureRenderer(polyline, offscreen[2]);

		// palette
		Texture palette = ctx.getResourceManager().GetTexture("palette", "palettes/greyscale2.png", ImageOptions.DEFAULT.withSingleLine());
		PaletteRenderer paletteRenderer = new PaletteRenderer("offscreen2");

		addItems(translateRenderStage, waveRenderStage, paletteRenderer);
		super.init(ctx);
	}
}
