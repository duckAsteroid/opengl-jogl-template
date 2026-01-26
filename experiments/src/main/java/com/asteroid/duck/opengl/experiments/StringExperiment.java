package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.text.StringRenderer;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.IOException;

public class StringExperiment implements Experiment {
    private static final String[] STRINGS = new String[]{
            "Hello, World!",
            "The quick brown fox jumps over the lazy dog.",
            "Sphinx of black quartz, judge my vow.",
            "Pack my box with five dozen liquor jugs.",
            "How vexingly quick daft zebras jump!"
    };
    private static int selectedStringIndex = 0;

    private static int maxLengthString() {
        int max = -1;
        for (int i = 0; i < STRINGS.length; i++) {
            var length = STRINGS[i].length();
            if (length > max) {
                max = length;
            }
        }
        return max;
    }

    private StringRenderer stringRenderer;
    @Override
    public String getDescription() {
        return "An experiment using string helper";
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        ctx.setClearScreen(true);
        registerKeys(ctx.getKeyRegistry());
        var ftf = new FontTextureFactory(new Font("Times New Roman", Font.BOLD,24), true);
        stringRenderer = new StringRenderer(ftf.createFontTexture(), maxLengthString());
        stringRenderer.init(ctx);
        stringRenderer.setText(STRINGS[selectedStringIndex]);
    }

    private void registerKeys(KeyRegistry ctx) {
        ctx.registerKeyAction(GLFW.GLFW_KEY_UP, GLFW.GLFW_MOD_SHIFT, () -> move(Direction.UP), "Move text up");
        ctx.registerKeyAction(GLFW.GLFW_KEY_DOWN,  GLFW.GLFW_MOD_SHIFT,() -> move(Direction.DOWN), "Move text down");
        ctx.registerKeyAction(GLFW.GLFW_KEY_LEFT, GLFW.GLFW_MOD_SHIFT, () -> move(Direction.LEFT), "Move text left");
        ctx.registerKeyAction(GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_MOD_SHIFT, () -> move(Direction.RIGHT), "Move text right");

        ctx.registerKeyAction(GLFW.GLFW_KEY_S, this::nextString, "Change string");
    }

    private void nextString() {
        selectedStringIndex = (selectedStringIndex + 1) % STRINGS.length;
        stringRenderer.setText(STRINGS[selectedStringIndex]);
    }

    private enum Direction {
        RIGHT, 
        DOWN,
        LEFT,
        UP
    }
    
    private void move(Direction direction) {
        final var amount = 5;
        switch (direction) {
            case RIGHT -> stringRenderer.setPosition(new Point(stringRenderer.getPosition().x + amount, stringRenderer.getPosition().y));
            case DOWN -> stringRenderer.setPosition(new Point(stringRenderer.getPosition().x, stringRenderer.getPosition().y + amount));
            case LEFT -> stringRenderer.setPosition(new Point(stringRenderer.getPosition().x - amount, stringRenderer.getPosition().y));
            case UP -> stringRenderer.setPosition(new Point(stringRenderer.getPosition().x, stringRenderer.getPosition().y - amount));
        }
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
