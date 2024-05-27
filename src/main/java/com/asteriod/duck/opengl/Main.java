package com.asteriod.duck.opengl;


import com.asteriod.duck.opengl.util.CompositeRenderItem;
import com.asteriod.duck.opengl.util.GLWindow;
import com.asteriod.duck.opengl.util.RenderContext;
import com.asteriod.duck.opengl.util.RenderedItem;
import com.asteriod.duck.opengl.util.resources.texture.*;
import com.asteriod.duck.opengl.util.timer.Timer;
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
        Main main = new Main( "(cShader Playground", 2048, 1600);
        main.setClearScreen(false);

        main.setRenderedItem(main.blurTest());

        printInstructions();
        main.displayLoop();
    }

    public RenderedItem paletteTest() throws IOException {
        Texture gray = getResourceManager().GetTexture("gray", "window.jpeg", ImageOptions.DEFAULT.withType(Type.GRAY));
        Texture palette = getResourceManager().GetTexture("palette", "palettes/FIRE2.MAP.png", ImageOptions.DEFAULT.withSingleLine());
        return new PaletteRenderer("gray");
    }

    enum TestType implements BiFunction<Point, Dimension, Byte> {
        STRIPE {
            @Override
            public Byte apply(Point p, Dimension d) {
                return (byte) (p.x % 2);
            }
        },
        BANDS {
            @Override
            public Byte apply(Point p, Dimension d) {
                return (byte) (p.x % 255);
            }
        },
        HORIZONTAL {
            @Override
            public Byte apply(Point p, Dimension d) {
                double fraction = p.x / d.getWidth();
                return (byte) (fraction * 255);
            }
        },
        VERTICAL {
            @Override
            public Byte apply(Point p, Dimension d) {
                return (byte) (p.y % 256);
            }
        },
        RANDOM {
            @Override
            public Byte apply(Point p, Dimension d) {
                return (byte) (Math.random() * 256);
            }
        };
    }

    private ImageData createTestData(Dimension size, Main.TestType type) {
        ByteBuffer imageData = ByteBuffer.allocateDirect(size.width * size.height);
        for (int y = 0; y < size.height; y++) {
            for (int x = 0; x < size.width; x++) {
                Point p = new Point(x,y);
                imageData.put(type.apply(p,size));
            }
        }
        imageData.flip();
        return new ImageData(imageData, size);
    }

    public RenderedItem blurTest() {
        BlurKernel.DiscreteSampleKernel blurKernel = new BlurKernel(21).getDiscreteSampleKernel();

        // a multi texture renderer alternating between two textures
        MultiTextureRenderer source = new MultiTextureRenderer("molly", "window");
        Texture molly = getResourceManager().GetTexture("molly", "molly.jpg");
        Texture window = getResourceManager().GetTexture("window", "test-card.jpeg");

        // a soundwave
        //Polyline poly = new Polyline();

        // create 2 offscreen textures
        Rectangle screen = getWindow();
        Texture[] offscreen = new Texture[2];
        for (int i = 0; i < offscreen.length ; i++) {
            offscreen[i] = new Texture();
            offscreen[i].Generate(screen.width, screen.height, 0);
            getResourceManager().PutTexture("offscreen"+i, offscreen[i]);
        }

        // wrap the multi tex to render to the offscreen texture 0
        OffscreenTextureRenderer offscreenTextureRenderer = new OffscreenTextureRenderer(source, offscreen[0]);
        // this passthrough uses the blur shader in X
        PassthruTextureRenderer blurX = new PassthruTextureRenderer("offscreen0", "blur-x", shader -> {
            shader.setFloatArray("offset", blurKernel.floatOffsets());
            shader.setFloatArray("weight", blurKernel.floatWeights());
        });
        // we wrap the blurX to render to the offscreen texture 1
        OffscreenTextureRenderer offscreenTextureRenderer2 = new OffscreenTextureRenderer(blurX , offscreen[1]);

        // a passthrough renderer (onto screen) of the "offscreen" texture 1 - which blurs Y on the way through
        PassthruTextureRenderer blurY = new PassthruTextureRenderer("offscreen1", "blur-y", shader -> {
            shader.setFloatArray("offset", blurKernel.floatOffsets());
            shader.setFloatArray("weight", blurKernel.floatWeights());
        });


        // A composite renderer to initialise and render the two paths: offscreen and onscreen
        return new CompositeRenderItem(offscreenTextureRenderer, offscreenTextureRenderer2, blurY);
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
