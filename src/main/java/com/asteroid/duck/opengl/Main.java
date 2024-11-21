package com.asteroid.duck.opengl;


import com.asteroid.duck.opengl.experiments.Experiment;
import com.asteroid.duck.opengl.experiments.ExperimentChooser;
import com.asteroid.duck.opengl.util.GLWindow;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.keys.KeyAction;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;


public class Main extends GLWindow implements RenderContext {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    public static String[] ARGS = {};


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

        main.displayLoop();
    }

    public void registerKeys() {
        KeyRegistry kr = getKeyRegistry();
        kr.registerKeyAction (GLFW_KEY_ESCAPE, this::exit, "Exit");
        kr.registerKeyAction(GLFW_KEY_SPACE, timer::togglePaused, "Pause and unpause the program timer (the display loop keeps running)");
        kr.registerKeyAction(GLFW_KEY_LEFT, () -> stepBack(false), "When paused, move the time backwards (<SHIFT> for larger steps)");
        kr.registerKeyAction(GLFW_KEY_RIGHT, () -> stepForward(false), "When paused, move the time forwards (<SHIFT> for larger steps)");
        kr.registerKeyAction(GLFW_KEY_I, this::printInstructions, "Print instructions");
        kr.registerKeyAction(GLFW_KEY_F11, this::toggleFullscreen, "Toggle fullscreen mode");
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

    public void render() throws IOException {
        timer.update();
        renderedItem.doRender(this);
    }

    public void dispose() {
        super.dispose();
        renderedItem.dispose();
    }
}
