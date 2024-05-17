package com.asteriod.duck.opengl;


import com.asteriod.duck.opengl.util.GLWindow;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.keys.KeyCombination;
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
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;


public class Main extends GLWindow implements RenderContext {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    private static String INSTRUCTIONS;

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
        Main main = new Main( "(cShader Playground", 1024, 800);
        main.setClearScreen(false);

        // a multi texture renderer alternating between two textures
        MultiTextureRenderer source = new MultiTextureRenderer("molly", "window");
        Texture molly = main.getResourceManager().GetTexture("molly", "molly.jpg", false);
        Texture window = main.getResourceManager().GetTexture("window", "window.jpeg", false);

        // a soundwave
        //Polyline poly = new Polyline();

        // create an offscreen texture
        Rectangle screen = main.getWindow();

        Texture[] offscreen = new Texture[2];
        for (int i = 0; i < offscreen.length ; i++) {
            offscreen[i] = new Texture();
            offscreen[i].Generate(screen.width, screen.height, 0);
            main.getResourceManager().PutTexture("offscreen"+i, offscreen[i]);
        }

        // wrap the multi tex to render to the offscreen texture
        TextureRenderer textureRenderer = new TextureRenderer(source, offscreen[0]);

        // a passthrough renderer (onto screen) of the "offscreen" texture
        PassthruTextureRenderer passthrough = new PassthruTextureRenderer("offscreen0", "blur");



        // A holder to initialise and render the two paths: offscreen and onscreen
        RenderedItem renderItem = new RenderedItem() {

            @Override
            public void init(RenderContext ctx) throws IOException {
                textureRenderer.init(ctx);
                passthrough.init(ctx);
            }

            @Override
            public void doRender(RenderContext ctx) {
                textureRenderer.doRender(ctx);
                passthrough.doRender(ctx);

            }

            @Override
            public void dispose() {

                passthrough.dispose();
                textureRenderer.dispose();
            }
        };

        main.setRenderedItem(renderItem);

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
