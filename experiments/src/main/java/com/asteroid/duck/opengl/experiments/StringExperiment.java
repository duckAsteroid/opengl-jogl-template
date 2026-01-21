package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.text.StringRenderer;

import java.awt.*;
import java.io.IOException;

public class StringExperiment implements Experiment {
    private StringRenderer stringRenderer;
    @Override
    public String getDescription() {
        return "An experiment using string helper";
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        var ftf = new FontTextureFactory(new Font("Times New Roman", Font.PLAIN,100), true);
        stringRenderer = new StringRenderer(ftf.createFontTexture(), 13);
        stringRenderer.init(ctx);
        stringRenderer.setText("Hello, World!");
    }

    @Override
    public void doRender(RenderContext ctx) {
        stringRenderer.doRender(ctx);
    }

    @Override
    public void dispose() {
        stringRenderer.dispose();
    }
}
