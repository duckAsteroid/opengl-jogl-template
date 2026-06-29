package com.asteroid.duck.opengl;


import com.asteroid.duck.opengl.experiments.Experiment;
import com.asteroid.duck.opengl.experiments.ExperimentChooser;
import com.asteroid.duck.opengl.util.GLWindow;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.manager.ResourceManagerImpl;
import com.asteroid.duck.opengl.util.resources.io.ClasspathLoader;
import com.asteroid.duck.opengl.util.timer.ClockImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

import static org.lwjgl.glfw.GLFW.*;

/**
 * An OpenGL window that runs a experiment
 */
public class Main extends GLWindow implements RenderContext {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private RenderedItem renderedItem;

    public Main(RenderedItem item, String title, int width, int height) {
        super(new ResourceManagerImpl(new ClasspathLoader(item.getClass(),"/")),title, width, height, "icon16.png");
        this.renderedItem = item;
    }


    public RenderedItem getRenderedItem() {
        return renderedItem;
    }

    public void setRenderedItem(RenderedItem renderedItem) {
        this.renderedItem = renderedItem;
    }

    public static void main(String[] args) throws Exception {
        String javaVersion = System.getProperty("java.version");
        System.out.println("Running with Java version: " + javaVersion);
        Main main = null;
        try {

            ExperimentChooser chooser = new ExperimentChooser(args);
            Experiment experiment = chooser.get();
            LOG.info("Running experiment: {}", experiment.getClass().getName());
            
            main = new Main(experiment, "(cShader Playground", 1024, 800);
            main.setClearScreen(false);

            //Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            main.displayLoop();
        }
        catch(RuntimeException e) {
            LOG.error("Error during main loop", e);
        }
        finally {
            LOG.info("Exiting...");
        }
        System.exit(0);
    }

    public void registerKeys() {
        KeyRegistry kr = getKeyRegistry();
        kr.registerKeyAction(GLFW_KEY_ESCAPE, this::exit, "Exit");
        kr.registerKeyAction(GLFW_KEY_SPACE, this::toggleClock, "Pause/unpause the clock");
        kr.registerKeyAction(GLFW_KEY_LEFT, () -> stepClock(-10.0), "When paused, rewind time 10 s");
        kr.registerKeyAction(KeyCombination.namedWithMods("LEFT", "SHIFT"), () -> stepClock(-100.0), "When paused, rewind time 100 s");
        kr.registerKeyAction(GLFW_KEY_RIGHT, () -> stepClock(10.0), "When paused, advance time 10 s");
        kr.registerKeyAction(KeyCombination.namedWithMods("RIGHT", "SHIFT"), () -> stepClock(100.0), "When paused, advance time 100 s");
        kr.registerKeyAction(KeyCombination.named("PRINT_SCREEN"), this::captureNextFrame, "Save screenshot");
        kr.registerKeyAction(KeyCombination.namedWithMods("PRINT_SCREEN", "SHIFT"), () -> startRecording(Duration.ofSeconds(5)), "Record 5s video");
        kr.registerKeyAction(GLFW_KEY_I, this::printInstructions, "Print instructions");
        kr.registerKeyAction(GLFW_KEY_F11, this::toggleFullscreen, "Toggle fullscreen mode");
        kr.registerKeyAction(GLFW_KEY_HOME, this::resetWindowSize, "Reset window to original size");
        kr.registerKeyAction(GLFW_KEY_EQUAL, this::scaleWindowUp, "Scale window up by 2x");
        kr.registerKeyAction(GLFW_KEY_MINUS, this::scaleWindowDown, "Scale window down by 2x");
        kr.registerKeyAction(GLFW_KEY_F1, this::printDebug, "Print debug data");
    }

    private void printDebug() {
        ClockImpl clock = (ClockImpl) getClock();
        System.out.println("Debug data:");
        System.out.printf("\tdesired-freq=%s%n", getDesiredUpdatePeriod() == null ? "N/A" : 1.0 / getDesiredUpdatePeriod());
        System.out.printf("\taverage-freq=%s%n", 1.0 / clock.averageUpdatePeriod());
        System.out.printf("\tinvocation-count=%s%n", clock.updateCount());
    }

    public void init() throws IOException {
        renderedItem.init(this);
    }

    public void render() throws IOException {
        renderedItem.doRender(this);
    }

    public void dispose() {
        super.dispose();
        renderedItem.dispose();
    }
}
