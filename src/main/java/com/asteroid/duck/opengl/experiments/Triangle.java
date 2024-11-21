package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.OffscreenTextureRenderer;
import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.CompositeRenderItem;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.Triangles;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureFactory;
import org.joml.Vector3f;
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
						new Vector3f(5, 2, 1));
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		RenderedItem triangle = basicTriangle();
		Rectangle screen = ctx.getWindow();

		Texture[] offscreen = new Texture[1];
		for (int i = 0; i < offscreen.length; i++) {
			offscreen[i] = TextureFactory.createTexture(screen, false);
			ctx.getResourceManager().PutTexture("offscreen"+i, offscreen[i]);
		};

		OffscreenTextureRenderer offscreenRenderer = new OffscreenTextureRenderer(triangle, offscreen[0]);

		PassthruTextureRenderer mainscreenRenderer = new PassthruTextureRenderer("offscreen0");

		addItems(offscreenRenderer, mainscreenRenderer);
		super.init(ctx);
	}
}
