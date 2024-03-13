package com.asteriod.duck.opengl;


import com.asteriod.duck.opengl.util.GLWindow;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.resources.FrameBuffer;
import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.timer.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;


public class Main extends GLWindow implements RenderContext {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;
    private static final String TEXTURE = "molly";

    private static String INSTRUCTIONS;

    static {
        INSTRUCTIONS = new BufferedReader(new InputStreamReader(Objects.requireNonNull(Main.class.getResourceAsStream("/instructions.txt"))))
            .lines().collect(Collectors.joining("\n"));
    }

    private final Timer timer = Timer.glfwGetTimeInstance();

    private final List<RenderedItem> renderedItems = new ArrayList<>();
    //private final Polyline polyline;
    private final SimpleTextureRenderer textureRenderer;
    private FrameBuffer frameBuffer;

    public Main(String title, int width, int height) {
        super(title, width, height, "icon16.png");
        //this.polyline = new Polyline();
        //renderedItems.add(polyline);
        this.textureRenderer = new SimpleTextureRenderer(TEXTURE);
        renderedItems.add(textureRenderer);
    }

    @Override
    public Timer getTimer() {
        return timer;
    }


    public static void main(String[] args) throws Exception {
        Main main = new Main("Shader Playground", 1024, 1024);
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
        // init texture

        getResourceManager().GetTexture(TEXTURE, "molly.jpg", false);

        //this.frameBuffer = new FrameBuffer(tex);
        // init rendered items
        renderedItems.forEach(item -> {
	        try {
		        item.init(this);
	        } catch (IOException e) {
		        LOG.error("Error initialising "+item, e);
	        }
        });
    }

    private void initRenderTexture() {
        Texture tex = new Texture();
        tex.setInternalFormat( GL_RGBA);
        tex.setImageFormat( GL_RGBA);
        Rectangle window = getWindow();
        tex.Generate(window.width, window.height, 0);
        getResourceManager().PutTexture(TEXTURE, tex);
    }


    public void dispose() {
        super.dispose();
        renderedItems.forEach(RenderedItem::dispose);
        //frameBuffer.dispose();
    }

    public void render() throws IOException {
        timer.update();


        textureRenderer.doRender(this);
    }
}
