package com.asteriod.duck.opengl;

import com.jogamp.common.net.Uri;
import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import static com.jogamp.opengl.GL.GL_DONT_CARE;
import static com.jogamp.opengl.GL2ES2.*;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;

public class Main implements GLEventListener {
    public static final int STEP = 10;
    public static final int LARGE_STEP = 100;

    private static GLWindow window;

    private ShaderProgram shaderProgram = null;
    private AtomicBoolean shaderDispose = new AtomicBoolean(false);

    private long elapsed;
    private long lastUpdate;
    private Animator animator;

    private boolean paused = true;

    private final float freq = 1; // Hz
    private FloatBuffer bkgBuffer;
    private short[] indices;

    private int[] vbo;
    private int[] ibo;

    public static void main(String[] args) {
        new Main().setup();
    }

    private void setup() {
        GLProfile glProfile = GLProfile.getGL4ES3();
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

        window = GLWindow.create(glCapabilities);

        window.setSize(600, 600);
        window.setResizable(true);

        updateTitle();

        //window.setFullscreen(true);
        window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        window.setVisible(true);

        window.addGLEventListener(this);

        elapsed = 0;

        animator = new Animator(window);
        animator.setUpdateFPSFrames(1, null);
        animator.setRunAsFastAsPossible(true);
        animator.start();

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(WindowEvent e) {
                animator.stop();
                System.exit(0);
            }
        });

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
                    exit();
                }
                else if (KeyEvent.VK_SPACE == e.getKeyCode()) {
                    togglePause();
                }
                else if (KeyEvent.VK_LEFT == e.getKeyCode()) {
                    stepBack((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
                }
                else if (KeyEvent.VK_RIGHT == e.getKeyCode()) {
                    stepForward((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0);
                }
                else if (KeyEvent.VK_F5 == e.getKeyCode()) {
                    shaderDispose.set(true);
                }
                else if (KeyEvent.VK_F11 == e.getKeyCode()) {
                    window.setFullscreen(!window.isFullscreen());
                    //System.out.println("Size: "+windowSizeString());
                }
            }
        });
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
        window.destroy();
    }

    private void updateTitle() {
        window.setTitle("Shader playground ["+windowSizeString()+"]");
    }

    public String windowSizeString() {
        return window.getWidth()+"x"+window.getHeight();
    }

    public void init(GLAutoDrawable drawable) {
        GL4 gl = drawable.getGL().getGL4();
        togglePause(); // start the clock

        initDebug(gl);

        bkgBuffer = Buffers.newDirectFloatBuffer(4);
        bkgBuffer.put(0, new float[] {0.0f, 0.0f, 0.0f, 0.0f});

        initShaderProgram(gl);

        // Define the vertices of the rectangle
        float[] vertices = {
                -1.0f, -1.0f, // bottom left
                1.0f, -1.0f, // bottom right
                1.0f, 1.0f, // top right
                -1.0f, 1.0f // top left
        };

        // Create a VBO and bind it
        vbo = new int[1];
        gl.glGenBuffers(1, vbo, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);

        // Store the vertex data in the VBO
        FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(vertices);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexBuffer.limit() * 4, vertexBuffer, GL.GL_STATIC_DRAW);


        // Create an IBO and bind it
        ibo = new int[1];
        gl.glGenBuffers(1, ibo, 0);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);

        // Store the index data in the IBO
        indices = new short[] {0, 1, 2, 0, 2, 3};
        ShortBuffer indexBuffer = Buffers.newDirectShortBuffer(indices);
        gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indexBuffer.limit() * 2, indexBuffer, GL.GL_STATIC_DRAW);

    }

    private static Uri[] fromPath(Path p) {
	    try {
          Uri uri = Uri.valueOf(p.toUri());
          return new Uri[] { uri };
      } catch (URISyntaxException e) {
		    e.printStackTrace();
	    }
	    return new Uri[0];
    }

    public void initShaderProgram(GL4 gl) {
        if (shaderProgram != null) {
            shaderProgram.destroy(gl);
            System.out.println("Shader disposed");
        }
        // load the GLSL Shaders
        ShaderCode
                vertShader = ShaderCode.create(gl, GL_VERTEX_SHADER, 1, fromPath(Paths.get("src/main/glsl/main.vert")), true);
        ShaderCode fragShader =
                ShaderCode.create(gl, GL_FRAGMENT_SHADER, 1, fromPath(Paths.get("src/main/glsl/main.frag")), true);

        ShaderProgram shaderProgram = new ShaderProgram();

        shaderProgram.add(vertShader);
        shaderProgram.add(fragShader);

        shaderProgram.init(gl);

        this.shaderProgram = shaderProgram;

        if (!shaderProgram.link(gl, System.err)) {
            ByteBuffer buffer = Buffers.newDirectByteBuffer(2 * 1024);
            gl.glGetShaderInfoLog(shaderProgram.program(), 1, IntBuffer.wrap(new int[]{buffer.limit()}), buffer);
            buffer.flip();
            System.err.println(StandardCharsets.UTF_8.decode(buffer));
        }

        System.out.println("Shaders loaded");
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GL4 gl = glAutoDrawable.getGL().getGL4();
        shaderProgram.destroy(gl);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClearBufferfv(GL_COLOR, 0, bkgBuffer);
        if (shaderDispose.get()) {
            initShaderProgram(gl);
            shaderDispose.set(false);
        }
        if (shaderProgram != null && shaderProgram.program() > 0) {
            gl.glUseProgram(shaderProgram.program());

            if (!paused) {
                long now = System.currentTimeMillis();
                elapsed += now - lastUpdate;
                lastUpdate = now;
            }

            int uniformLocation = gl.glGetUniformLocation(shaderProgram.program(), "millis");
            gl.glUniform1f(uniformLocation, timer());

            // Enable the vertex attribute array and specify the vertex attribute pointer
            int positionAttribute = gl.glGetAttribLocation(shaderProgram.program(), "position");
            gl.glEnableVertexAttribArray(positionAttribute);
            gl.glVertexAttribPointer(positionAttribute, 2, GL.GL_FLOAT, false, 0, 0);

        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vbo[0]);
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, ibo[0]);

        gl.glDrawElements(GL.GL_TRIANGLES, indices.length, GL.GL_UNSIGNED_SHORT, 0);

        gl.glUseProgram(0);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL4 gl = drawable.getGL().getGL4();
        gl.glViewport(x, y, width, height);
        updateTitle();
        System.out.println("Size: "+windowSizeString());
    }

    private void initDebug(GL4 gl) {

        window.getContext().addGLDebugListener(new GLDebugListener() {
            @Override
            public void messageSent(GLDebugMessage event) {
                System.out.println(event);
                throw new RuntimeException(event.getDbgMsg());
            }
        });

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DONT_CARE,
                0,
                null,
                false);

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_HIGH,
                0,
                null,
                true);

        gl.glDebugMessageControl(
                GL_DONT_CARE,
                GL_DONT_CARE,
                GL_DEBUG_SEVERITY_MEDIUM,
                0,
                null,
                true);
    }
}
