package com.asteriod.duck.opengl;


import com.asteriod.duck.opengl.experiments.Experiment;
import com.asteriod.duck.opengl.experiments.ExperimentChooser;
import com.asteriod.duck.opengl.util.*;
import com.asteriod.duck.opengl.util.resources.texture.*;
import com.asteriod.duck.opengl.util.timer.Timer;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30C.*;


public class Main extends GLWindow implements RenderContext {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    private static String INSTRUCTIONS;

    public static String[] ARGS = {};

    static {
        INSTRUCTIONS = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("/instructions.txt"))))
            .lines().collect(Collectors.joining("\n"));
    }

    private final Timer timer = Timer.glfwGetTimeInstance();

    private RenderedItem renderedItem;

    public Main(RenderedItem renderer, String title, int width, int height) {
        super(title, width, height, "icon16.png");
        this.renderedItem = renderer;
    }

    public Main(String title, int width, int height) {
        super(title, width, height, "icon16.png");
    }

    public RenderedItem getRenderedItem() {
        return renderedItem;
    }

    public void setRenderedItem(RenderedItem renderedItem) {
        this.renderedItem = renderedItem;
    }

    @Override
    public Timer getTimer() {
        return timer;
    }


    public static void main(String[] args) throws Exception {
        Main.ARGS = args;
        Main main = new Main( "(cShader Playground", 1024, 800);
        main.setClearScreen(false);

        ExperimentChooser chooser = new ExperimentChooser();
        Experiment experiment = chooser.get();
        main.setRenderedItem(experiment);

        printInstructions();
        main.displayLoop();
    }

    public void registerKeys() {
        registerKeyAction(GLFW_KEY_SPACE, timer::togglePaused);
        registerKeyAction(GLFW_KEY_LEFT, () -> stepBack(false));
        registerKeyAction (GLFW_KEY_RIGHT, () -> stepForward(false));
        registerKeyAction(GLFW_KEY_I, Main::printInstructions);
    }

    public static void printInstructions() {
        System.out.println(INSTRUCTIONS);
    }

    private void stepBack(boolean largeStep) {
        timer.step(largeStep ? -LARGE_STEP : -STEP);
    }

    private void stepForward(boolean largeStep) {
        timer.step(largeStep ? LARGE_STEP : STEP);
    }


    public void init() throws IOException {
        timer.reset(); // start the clock
        renderedItem.init(this);
    }

    private void initRenderTexture() {
        Texture tex = new Texture();
        tex.setInternalFormat( GL_RGBA);
        tex.setImageFormat( GL_RGBA);
        Rectangle window = getWindow();
        tex.Generate(window.width, window.height, 0);
        //getResourceManager().PutTexture(TEXTURE, tex);
    }

    public void render() throws IOException {
        timer.update();
        renderedItem.doRender(this);
    }

    public void dispose() {
        super.dispose();
        renderedItem.dispose();
    }
}
