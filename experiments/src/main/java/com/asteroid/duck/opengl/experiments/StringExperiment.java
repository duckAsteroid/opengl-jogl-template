package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.PassthruTextureRenderer;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.font.FontTextureFactory;
import com.asteroid.duck.opengl.util.text.StringRenderer;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class StringExperiment implements Experiment {
    private static final float ROTATION_STEP = (float) Math.toRadians(5);
    private static final float SCALE_FACTOR  = 1.1f;

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
    private PassthruTextureRenderer textureRenderer;

    private float tx = 0, ty = 0;
    private float rotation = 0;
    private float scale = 1.0f;

    @Override
    public String getDescription() {
        return "An experiment using string helper";
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        ctx.getResourceManager().getTexture("molly.jpg", "molly.jpg");
        registerKeys(ctx.getKeyRegistry());
        var ftf = new FontTextureFactory(new Font("Times New Roman", Font.BOLD,24), true);
        stringRenderer = new StringRenderer(ftf.createFontTexture());
        stringRenderer.init(ctx);
        stringRenderer.setText(STRINGS[selectedStringIndex]);

        // FIXME This next line is what nukes the texture from the font?
        // Probably because the texture unit is default and this binds a texture during it's init?
        // it then unbinds (binds 0) which nukes the texture in the texture unit
        // before we get a chance to use it in the string renderer?

        textureRenderer = new PassthruTextureRenderer("molly.jpg");
        textureRenderer.init(ctx);
    }

    private void registerKeys(KeyRegistry ctx) {
        ctx.registerKeyAction(GLFW.GLFW_KEY_UP,    GLFW.GLFW_MOD_SHIFT, () -> move(Direction.UP),    "Move text up");
        ctx.registerKeyAction(GLFW.GLFW_KEY_DOWN,  GLFW.GLFW_MOD_SHIFT, () -> move(Direction.DOWN),  "Move text down");
        ctx.registerKeyAction(GLFW.GLFW_KEY_LEFT,  GLFW.GLFW_MOD_SHIFT, () -> move(Direction.LEFT),  "Move text left");
        ctx.registerKeyAction(GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_MOD_SHIFT, () -> move(Direction.RIGHT), "Move text right");

        ctx.registerKeyAction(GLFW.GLFW_KEY_E, () -> { rotation -= ROTATION_STEP; applyTransform(); }, "Rotate text counter-clockwise");
        ctx.registerKeyAction(GLFW.GLFW_KEY_R, () -> { rotation += ROTATION_STEP; applyTransform(); }, "Rotate text clockwise");
        ctx.registerKeyAction(GLFW.GLFW_KEY_Z, () -> { scale *= SCALE_FACTOR;     applyTransform(); }, "Scale text up");
        ctx.registerKeyAction(GLFW.GLFW_KEY_X, () -> { scale /= SCALE_FACTOR;     applyTransform(); }, "Scale text down");
        ctx.registerKeyAction(GLFW.GLFW_KEY_0, () -> { tx = 0; ty = 0; rotation = 0; scale = 1.0f; applyTransform(); }, "Reset text transform");

        ctx.registerKeyAction(GLFW.GLFW_KEY_S, this::nextString, "Change string");
        ctx.registerKeyAction(GLFW.GLFW_KEY_C, this::nextColor, "Change color");
    }

    private void applyTransform() {
        stringRenderer.setTransform(
            new Matrix4f().translate(tx, ty, 0).rotateZ(rotation).scale(scale, scale, 1)
        );
    }
    private final Random rnd = new Random();

    private void nextColor() {
        var index = rnd.nextInt(StandardColors.values().length);
        var color = StandardColors.values()[index];
        System.out.println("Changing color to " + color.name());
        stringRenderer.setTextColor(color.color);
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
            case RIGHT -> tx += amount;
            case DOWN  -> ty += amount;
            case LEFT  -> tx -= amount;
            case UP    -> ty -= amount;
        }
        applyTransform();
    }

    @Override
    public void doRender(RenderContext ctx) {
        textureRenderer.doRender(ctx);
        stringRenderer.doRender(ctx);
    }

    @Override
    public void dispose() {
        textureRenderer.dispose();
        stringRenderer.dispose();
    }
}
