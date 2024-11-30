package com.asteroid.duck.opengl;


import com.asteroid.duck.opengl.experiments.Experiment;
import com.asteroid.duck.opengl.experiments.ExperimentChooser;
import com.asteroid.duck.opengl.util.GLWindow;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.timer.TimeSource;
import com.asteroid.duck.opengl.util.timer.Timer;
import com.asteroid.duck.opengl.util.timer.TimerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;


public class Main extends GLWindow implements RenderContext {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    public static String[] ARGS = {};

    private final TimerImpl timer = new TimerImpl(TimeSource.glfwGetTimeInstance());

    private RenderedItem renderedItem;

    private Double desiredUpdatePeriod = null;

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

    public Double getDesiredUpdatePeriod() {
        return desiredUpdatePeriod;
    }

    public void setDesiredUpdatePeriod(Double period) {
        this.desiredUpdatePeriod = period;
    }

    public static void main(String[] args) throws Exception {
        Main.ARGS = args;
        Main main = new Main( "(cShader Playground", 1024, 800);
        main.setClearScreen(false);

        ExperimentChooser chooser = new ExperimentChooser();
        Experiment experiment = chooser.get();
        main.setRenderedItem(experiment);

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
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
        kr.registerKeyAction(GLFW_KEY_F1,this::printDebug, "Print debug data");
    }

    private void printDebug() {
        System.out.println("Debug data:");
	      System.out.printf("\tdesired-freq=%s%n", desiredUpdatePeriod == null ? "N/A" : 1.0 / desiredUpdatePeriod);
        System.out.printf("\taverage-freq=%s%n", 1.0 / timer.averageUpdatePeriod());
        System.out.printf("\tinvocation-count=%s%n", timer.updateCount());
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
        double lastUpdatePeriod = timer.update();
        renderedItem.doRender(this);
        if (desiredUpdatePeriod != null) {
            // try to sleep to match frequency
            if (desiredUpdatePeriod > lastUpdatePeriod) {
                sleep(desiredUpdatePeriod - lastUpdatePeriod);
            }
        }
    }

    private void sleep(double seconds) {
        long sleepInMillis = (long)(seconds * 1000.0);
        if (sleepInMillis > 10) {
            try {
                Thread.sleep(sleepInMillis);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void dispose() {
        super.dispose();
        renderedItem.dispose();
    }
}
