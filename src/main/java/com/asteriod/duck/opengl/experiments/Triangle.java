package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.OffscreenTextureRenderer;
import com.asteriod.duck.opengl.PassthruTextureRenderer;
import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.Triangles;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import org.joml.Vector4f;

import java.awt.*;
import java.io.IOException;

public class Triangle extends CompositeRenderItem implements Experiment {
	@Override
	public String getDescription() {
		return "Render a coloured triangle on screen using an offscreen texture.";
	}

	public static RenderedItem basicTriangle() {
		return Triangles.centralTriangle().simpleRenderer(
						new Vector4f(1.0f, 0.75f, 0.5f, 1.0f),
						null);
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		RenderedItem triangle = basicTriangle();
		Rectangle screen = ctx.getWindow();

		Texture[] offscreen = new Texture[1];
		for (int i = 0; i < offscreen.length; i++) {
			offscreen[i] = Utils.createOffscreenTexture(screen, true);
			ctx.getResourceManager().PutTexture("offscreen"+i, offscreen[i]);
		};

		OffscreenTextureRenderer offscreenRenderer = new OffscreenTextureRenderer(triangle, offscreen[0]);

		PassthruTextureRenderer mainscreenRenderer = new PassthruTextureRenderer("offscreen0");

		addItems(offscreenRenderer, mainscreenRenderer);
		super.init(ctx);
	}
}
