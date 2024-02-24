package com.asteriod.duck.opengl;


import com.asteriod.duck.opengl.util.keys.Keys;
import com.asteriod.duck.opengl.util.ShaderProgram;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;


public class Main {
    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    private long windowHandle;
    private String windowTitle;

    private Rectangle windowed = null;

    private static String INSTRUCTIONS;

    static {
        INSTRUCTIONS = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/instructions.txt")))
            .lines().collect(Collectors.joining("\n"));
    }

    private ShaderProgram shaderProgram = null;
    private AtomicBoolean shaderDispose = new AtomicBoolean(false);

    private long elapsed;
    private long lastUpdate;

    private boolean paused = true;

    private int vbo;
    private int ibo;
    private int vao;

    public Main(String title, int width, int height) {
        this.windowTitle = title;
        //System.out.println("INFO: OpenGL Version: "+glGetString(GL_VERSION));
        GLFWErrorCallback.createPrint(System.err).set();

        if(!glfwInit()) throw new RuntimeException("Unable to init GLFW");

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_DEBUG, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        glfwSetKeyCallback(windowHandle, this::keyCallback);
        glfwSetFramebufferSizeCallback(windowHandle, this::frameBufferSizeCallback);

        updateTitle();

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(windowHandle);


        // kick off GL
        GL.createCapabilities();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void main(String[] args) throws Exception {

        Main main = new Main("Shader Playground", 1024, 1024);
        printInstructions();
        main.displayLoop();
    }

    public void frameBufferSizeCallback(long window, int width, int height) {
        glViewport(0, 0, width, height);
        updateTitle();
    }

    public void keyCallback(long window, int key, int scancode, int action, int mode) {
        if (action == GLFW_PRESS) {
            if (GLFW_KEY_ESCAPE == key) {
                exit();
            } else if (GLFW_KEY_SPACE == key) {
                togglePause();
            } else if (GLFW_KEY_LEFT == key) {
                stepBack((mode & GLFW_MOD_SHIFT) != 0);
            } else if (GLFW_KEY_RIGHT == key) {
                stepForward((mode & GLFW_MOD_SHIFT) != 0);
            } else if (GLFW_KEY_F5 == key) {
                shaderDispose.set(true);
            } else if (GLFW_KEY_F11 == key) {
                toggleFullscreen();
            } else {
                printInstructions();
            }
        }
    }

    public void toggleFullscreen() {
        if (windowed == null) {
            windowed = getWindow();
            long monitor = glfwGetCurrentMonitor(windowHandle);
            GLFWVidMode mode = glfwGetVideoMode(monitor);
            glfwSetWindowMonitor(windowHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
        } else {
            glfwSetWindowMonitor(windowHandle, NULL, windowed.x, windowed.y, windowed.width, windowed.height, 0 );
            windowed = null;
        }
    }

    public static long glfwGetCurrentMonitor(long window) {
        int[] wx = {0}, wy = {0}, ww = {0}, wh = {0};
        int[] mx = {0}, my = {0}, mw = {0}, mh = {0};
        int overlap, bestoverlap;
        long bestmonitor;
        PointerBuffer monitors;
        GLFWVidMode mode;

        bestoverlap = 0;
        bestmonitor = glfwGetPrimaryMonitor();// (You could set this back to NULL, but I'd rather be guaranteed to get a valid monitor);

        glfwGetWindowPos(window, wx, wy);
        glfwGetWindowSize(window, ww, wh);
        monitors = glfwGetMonitors();

        while(monitors.hasRemaining()) {
            long monitor = monitors.get();
            mode = glfwGetVideoMode(monitor);
            glfwGetMonitorPos(monitor, mx, my);
            mw[0] = mode.width();
            mh[0] = mode.height();

            overlap =
                    Math.max(0, Math.min(wx[0] + ww[0], mx[0] + mw[0]) - Math.max(wx[0], mx[0])) *
                            Math.max(0, Math.min(wy[0] + wh[0], my[0] + mh[0]) - Math.max(wy[0], my[0]));

            if (bestoverlap < overlap) {
                bestoverlap = overlap;
                bestmonitor = monitor;
            }
        }

        return bestmonitor;
    }

    public static void printInstructions() {
        System.out.println(INSTRUCTIONS);
    }

    private void stepBack(boolean largeStep) {
        step(largeStep ? -LARGE_STEP : -STEP);
    }

    private void stepForward(boolean largeStep) {
        step(largeStep ? LARGE_STEP : STEP);
    }

    private void step(int amount) {
        if (paused) {
            elapsed += amount;
            System.out.println("Step to "+ timer() + "ms");
        }
        else {
            System.err.println("Program is not paused");
        }
    }

    private void togglePause() {
        paused = !paused;
        if (!paused) {
            System.out.println("Restarted");
            lastUpdate = System.currentTimeMillis();
        }
        else {
            System.out.println("Paused @ "+ timer() + "ms");
        }
    }

    public long timer() {
        return elapsed;
    }

    private void exit() {
        System.out.println("Exit");
        glfwSetWindowShouldClose(windowHandle, true);
    }

    private void updateTitle() {
        glfwSetWindowTitle(windowHandle,windowTitle + " ["+windowSizeString()+"]");
    }

    public String windowSizeString() {
        Rectangle window = getWindow();
        return window.getWidth()+"x"+window.getHeight();
    }

    public Rectangle getWindow() {
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            IntBuffer pX = stack.mallocInt(1);
            IntBuffer pY = stack.mallocInt(1);
            glfwGetWindowPos(windowHandle, pX, pY);

            return new Rectangle(pX.get(0), pY.get(0), pWidth.get(0), pHeight.get(0));
        }
    }

    public void init() throws IOException {

        togglePause(); // start the clock

        initShaderProgram();

        initBuffers();
    }

    private void initBuffers() {

        // Define the vertices of the rectangle
        float[] vertices = {
                -1.0f, -1.0f, // bottom left
                1.0f, -1.0f, // bottom right
                1.0f, 1.0f, // top right
                -1.0f, 1.0f // top left
        };

        short[] indices = new short[]{0, 1, 2, 0, 2, 3};

        try(MemoryStack stack = MemoryStack.stackPush()) {
            vao = glGenVertexArrays();
            glBindVertexArray(vao);

            // Create a VBO and bind it
            vbo = glGenBuffers();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);

            // Store the vertex data in the VBO
            FloatBuffer vertexBuffer = stack.floats(vertices);
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

            // Create an IBO and bind it
            ibo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

            // Store the index data in the IBO - create two triangles
            ShortBuffer indexBuffer = stack.shorts(indices);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        }
    }


    public void initShaderProgram() throws IOException {
        if (shaderProgram != null) {
            shaderProgram.destroy();
            System.out.println("Shader disposed");
        }
        // load the GLSL Shaders
        this.shaderProgram = ShaderProgram.compile(Paths.get("src/main/glsl/main.vert"), Paths.get("src/main/glsl/main.frag"), null);

        System.out.println("Shaders loaded");
    }

    public void dispose() {

        if (shaderProgram!=null) {
            shaderProgram.destroy();
        }
    }

    public void render() throws IOException {
        if (shaderDispose.get()) {
            initShaderProgram();
            shaderDispose.set(false);
        }
        if (shaderProgram != null && shaderProgram.id() > NULL) {
            shaderProgram.use();

            if (!paused) {
                long now = System.currentTimeMillis();
                elapsed += now - lastUpdate;
                lastUpdate = now;
            }

            shaderProgram.setFloat("millis", timer(), false);

            shaderProgram.setVertexAttribPointer("position", 2, GL_FLOAT, false, 0, 0);


        }
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);

        glUseProgram(0);
    }

    public void displayLoop() throws IOException {
        // initialize
        // ---------------
        init();

        // deltaTime variables
        // -------------------
        double deltaTime = 0.0f;
        double lastFrame = 0.0f;

        while (!glfwWindowShouldClose(windowHandle))
        {
            // calculate delta time
            // --------------------
            double currentFrame = glfwGetTime();
            deltaTime = currentFrame - lastFrame;
            lastFrame = currentFrame;
            glfwPollEvents();

            // manage user input
            // -----------------
            //processInput((float) deltaTime);

            // update game state
            // -----------------
            //update(deltaTime);

            // render
            // ------
            glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);
            render();

            glfwSwapBuffers(windowHandle);
        }

        // delete all resources
        // ---------------------
        dispose();

        glfwFreeCallbacks(windowHandle);
        glfwSetErrorCallback(null).free();
        glfwDestroyWindow(windowHandle);

        glfwTerminate();
    }

    public void reshape(int x, int y, int width, int height) {
        glViewport(x, y, width, height);
        updateTitle();
        System.out.println("Size: "+windowSizeString());
    }

}
