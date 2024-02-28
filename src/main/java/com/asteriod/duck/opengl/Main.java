package com.asteriod.duck.opengl;


import com.asteriod.duck.opengl.util.GLWindow;
import com.asteriod.duck.opengl.util.resources.ImageData;
import com.asteriod.duck.opengl.util.resources.ResourceManager;
import com.asteriod.duck.opengl.util.Texture;
import com.asteriod.duck.opengl.util.ShaderProgram;
import com.asteriod.duck.opengl.util.timer.Timer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


public class Main extends GLWindow {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    private static String INSTRUCTIONS;

    static {
        INSTRUCTIONS = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/instructions.txt")))
            .lines().collect(Collectors.joining("\n"));
    }

    private ShaderProgram shaderProgram = null;
    private AtomicBoolean shaderDispose = new AtomicBoolean(false);

    private Timer timer = Timer.glfwGetTimeInstance();

    private int vbo;
    private int ibo;
    private int vao;

    public Main(String title, int width, int height) {
        super(title, width, height, "icon16.png");
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main("Shader Playground", 1024, 1024);
        printInstructions();
        main.displayLoop();
    }


    public void keyCallback(long window, int key, int scancode, int action, int mode) {
        if (action == GLFW_PRESS) {
            if (GLFW_KEY_ESCAPE == key) {
                exit();
            } else if (GLFW_KEY_SPACE == key) {
                timer.togglePaused();
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
        Texture molly = ResourceManager.instance().GetTexture("molly", "molly.jpg",false);
        molly.Bind();
        shaderProgram.setInteger("molly", molly.id(), false);

        System.out.println("Shaders loaded");
    }

    public void dispose() {
        if (shaderProgram!=null) {
            shaderProgram.destroy();
        }
    }

    public void render() throws IOException {
        timer.update();

        if (shaderDispose.get()) {
            initShaderProgram();
            shaderDispose.set(false);
        }

        if (shaderProgram != null && shaderProgram.id() > NULL) {
            shaderProgram.use();

            shaderProgram.setFloat("seconds", (float) timer.elapsed(), false);

            shaderProgram.setVertexAttribPointer("position", 2, GL_FLOAT, false, 0, 0);


        }
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);

        glUseProgram(0);
    }



}
