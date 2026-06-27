package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.events.ResizeListener;
import com.asteroid.duck.opengl.util.keys.*;
import com.asteroid.duck.opengl.util.resources.manager.ResourceManager;
import com.asteroid.duck.opengl.util.resources.texture.io.TextureData;
import com.asteroid.duck.opengl.util.resources.texture.io.ImageLoadingOptions;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.imageio.ImageIO;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/// An abstract base class for creating OpenGL applications. Sets up a window with key handlers
/// and responds to resize events.
///
/// This class sets up and runs the main render loop.
/// Implements [RenderContext] for child components to interact with this window and the render loop.
public abstract class GLWindow implements RenderContext {
	private static final Logger LOG = LoggerFactory.getLogger(GLWindow.class);

	private final long windowHandle;
	private final String windowTitle;

	private final ResourceManager resourceManager;
	private final GLFWKeyCallback glfwKeyCallback;
	private final GLFWFramebufferSizeCallback glfwFramebufferSizeCallback;
    private final GLFWWindowCloseCallback glfwWindowCloseCallback;

	private final KeyRegistry keyRegistry = new KeyRegistry();
	private GLFWErrorCallback errorCallback;
	private Rectangle windowed = null;
	private Rectangle window;
	private int initialWidth;
	private int initialHeight;
	private int windowScale = 0;
	private final List<ResizeListener> resizeListeners = new CopyOnWriteArrayList<>();
	private Vector4f backgroundColor = new Vector4f(0.0f);
	private boolean clearScreen = true;
    private boolean windowClosing = false;
	private final Random random = new Random();

	/** Non-null when a screenshot has been requested; cleared after the capture executes. */
	private volatile Path pendingCapture = null;

	/**
	 * Create and display a GLFW window with an OpenGL 3.3 Core Profile context.
	 *
	 * <p>The constructor performs all GLFW and GL initialisation synchronously on the calling
	 * thread. Key and framebuffer-size callbacks are installed before the window is shown. If
	 * running on Linux, X11 is forced to avoid libdecor issues on Wayland compositors.</p>
	 *
	 * @param resourceManager the resource manager that owns all GL handles for this window;
	 *                        it is disposed when the window closes
	 * @param title           the window title; displayed in the title bar augmented with the
	 *                        current pixel resolution
	 * @param width           the initial window width in screen coordinates (not framebuffer pixels)
	 * @param height          the initial window height in screen coordinates
	 * @param icon            classpath path to a PNG icon image, or {@code null} to use the
	 *                        system default window icon
	 */
	public GLWindow(ResourceManager resourceManager, String title, int width, int height, String icon) {
        this.resourceManager = resourceManager;
		this.windowTitle = title;
		this.initialWidth = width;
		this.initialHeight = height;
        //System.out.println("INFO: OpenGL Version: "+glGetString(GL_VERSION));
        //this.errorCallback = GLFWErrorCallback.createPrint(System.err).set();
		if (Platform.get() == Platform.LINUX) {
			// Force X11 to avoid libdecor issues on Wayland
			GLFW.glfwInitHint(GLFW.GLFW_PLATFORM, GLFW.GLFW_PLATFORM_X11);
		}
        if (!glfwInit()) throw new RuntimeException("Unable to init GLFW");



        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_CONTEXT_DEBUG, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        //glfwWindowHint(GLFW_SCALE_TO_MONITOR, GLFW_TRUE);


        windowHandle = glfwCreateWindow(width, height, title, NULL, NULL);
        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        this.glfwKeyCallback = glfwSetKeyCallback(windowHandle, this::keyCallback);
        this.glfwFramebufferSizeCallback = glfwSetFramebufferSizeCallback(windowHandle, this::frameBufferSizeCallback);
        this.glfwWindowCloseCallback = glfwSetWindowCloseCallback(windowHandle, this::windowCloseCallback);

        window = readWindow();

        updateTitle();

        if (icon != null) {
            try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
                TextureData imgData = resourceManager.loadTextureData(icon, ImageLoadingOptions.DEFAULT.withNoFlip());
                icons.position(0)
                        .width(imgData.size().width)
                        .height(imgData.size().height)
                        .pixels(imgData.buffer());
                glfwSetWindowIcon(windowHandle, icons);
            } catch (IOException e) {
                LOG.error("Unable to load window icon", e);
            }
        }

        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(windowHandle);

        // kick off GL
        GL.createCapabilities();
        glEnable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Set the initial viewport. This is critical for high-DPI displays.
        // We query the framebuffer size, which may be larger than the window size
        // we requested, and set the viewport to match.
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1), pHeight = stack.mallocInt(1);
            glfwGetFramebufferSize(windowHandle, pWidth, pHeight);
            glViewport(0, 0, pWidth.get(0), pHeight.get(0));
        }

        String gpuName = glGetString(GL_RENDERER);
        System.out.println("GPU Renderer: " + gpuName);
    }

	@Override
	public Random getRandom() {
		return random;
	}

	@Override
	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	@Override
	public KeyRegistry getKeyRegistry() {
		return keyRegistry;
	}

	public Vector4f getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Vector4f color) {
		this.backgroundColor = color;
	}

	public boolean isClearScreen() {
		return clearScreen;
	}

	public void setClearScreen(boolean clearScreen) {
		this.clearScreen = clearScreen;
	}

	/**
	 * GLFW key callback: translates a raw key event into a {@link KeyCombination} and dispatches
	 * it to the {@link KeyRegistry}. Only {@code GLFW_PRESS} events are forwarded; held and
	 * released events are ignored.
	 *
	 * @param window     the GLFW window handle (unused; context is current)
	 * @param key        GLFW key code of the pressed key
	 * @param scancode   platform-specific scan code (unused)
	 * @param actionCode GLFW action code ({@code GLFW_PRESS}, {@code GLFW_RELEASE}, etc.)
	 * @param mode       GLFW modifier bitmask at the time of the press
	 */
	public final void keyCallback(long window, int key, int scancode, int actionCode, int mode) {
		if (actionCode == GLFW_PRESS) {
			Optional<Key> knownKey = Keys.instance().keyFor(key);
			if (knownKey.isPresent()) {
				Set<Key> mods = Keys.instance().modsFor(mode);
				KeyCombination combo = new KeyCombination(Set.of(knownKey.get()), mods);
				keyRegistry.handleCallback(combo);
			}
		}
	}

	/**
	 * GLFW framebuffer-size callback: updates the GL viewport and cached window rectangle,
	 * then notifies all registered {@link ResizeListener}s.
	 * Called automatically by GLFW on the render thread when the window is resized.
	 *
	 * @param window the GLFW window handle (unused; context is already current)
	 * @param width  new framebuffer width in pixels
	 * @param height new framebuffer height in pixels
	 */
	public void frameBufferSizeCallback(long window, int width, int height) {
		glViewport(0, 0, width, height);
		this.window = readWindow();
		updateTitle();
		for (ResizeListener listener : resizeListeners) {
			listener.onResize(width, height);
		}
	}

    /**
     * GLFW window-close callback: sets the closing flag so {@link #displayLoop()} exits cleanly
     * after the current frame completes. The actual GLFW window destruction happens in
     * {@link #displayLoop()} to keep all GL and GLFW calls on the render thread.
     *
     * @param l the GLFW window handle (unused)
     */
    public void windowCloseCallback(long l) {
        windowClosing = true;
    }

	/**
	 * Run the main render loop: calls {@link #init()}, {@link #registerKeys()}, and
	 * {@link #printInstructions()} once, then enters the frame loop until the window closes.
	 *
	 * <p>Each frame: polls events, optionally clears the screen, calls {@link #render()},
	 * processes any pending screenshot capture, and swaps buffers. On exit, calls
	 * {@link #dispose()} and tears down GLFW.</p>
	 *
	 * @throws IOException if {@link #init()} or {@link #render()} throws
	 */
	public void displayLoop() throws IOException {
		// initialize
		// ---------------
		init();
		registerKeys();
		printInstructions();

		// loop
		while (!windowClosing)
		{
			glfwPollEvents();

			if(clearScreen) {
				clearScreen();
			}

			// render
			// ---------------------
			render();

			// capture before swap so the image matches exactly what hits the screen
			Path capture = pendingCapture;
			if (capture != null) {
				pendingCapture = null;
				captureFramebuffer(capture);
			}

			glfwSwapBuffers(windowHandle);
            windowClosing |= glfwWindowShouldClose(windowHandle);
		}

		// delete all resources
		// ---------------------
		dispose();

		glfwFreeCallbacks(windowHandle);
		glfwDestroyWindow(windowHandle);

		glfwSetErrorCallback(null);

		glfwTerminate();
	}

	/**
	 * Clear the colour buffer to {@link #getBackgroundColor()}.
	 * Called each frame when {@link #isClearScreen()} is {@code true}.
	 */
	public void clearScreen() {
		glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	/**
	 * Register keyboard shortcuts with the {@link KeyRegistry}.
	 * Called once during {@link #displayLoop()} before the frame loop begins.
	 */
	public abstract void registerKeys();

	/**
	 * Initialise GL resources for this window.
	 * Called once at the start of {@link #displayLoop()}; allocate shaders, buffers, and textures here.
	 *
	 * @throws IOException if resource loading fails
	 */
	public abstract void init() throws IOException;

	/**
	 * Render one frame. Called every iteration of the display loop after optional screen clearing.
	 *
	 * @throws IOException if rendering fails due to an I/O-backed resource
	 */
	public abstract void render() throws IOException;

	/**
	 * Print the registered key bindings to standard output.
	 * Called once after {@link #registerKeys()} so the user sees the controls on startup.
	 */
	public void printInstructions() {
		System.out.println("Keys:");
		int maxKeyStrWidth = getKeyRegistry().stream().mapToInt(ka -> ka.getCombination().asSimpleString().length()).max().orElse(0);
		for(KeyAction ka : getKeyRegistry()) {
			System.out.printf("\t%-"+maxKeyStrWidth+ "s - %s%n", ka.getCombination().asSimpleString(), ka.getDescription());
		}
	}

	@Override
	public void captureNextFrame(Path destination) {
		this.pendingCapture = destination;
	}

	/**
	 * Reads the current framebuffer pixels via {@code glReadPixels}, flips the image vertically
	 * (GL origin is bottom-left), then writes a PNG on a virtual thread.
	 *
	 * <p>Must be called on the GL thread after {@link #render()} and before
	 * {@code glfwSwapBuffers} so that the framebuffer contents are complete and correct.</p>
	 */
	private void captureFramebuffer(Path path) {
		try (MemoryStack stack = stackPush()) {
			IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
			glfwGetFramebufferSize(windowHandle, pw, ph);
			int w = pw.get(0), h = ph.get(0);
			int stride = w * 3;
			ByteBuffer pixels = memAlloc(stride * h);
			try {
				glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, pixels);
				// Copy into an int[] on the GL thread (fast, no GL calls), then hand off to a
				// virtual thread for the slow file I/O so the render loop is not stalled.
				int[] rgb = new int[w * h];
				for (int y = 0; y < h; y++) {
					// GL stores rows bottom-to-top; invert so row 0 is the top of the image.
					int src = (h - 1 - y) * stride;
					for (int x = 0; x < w; x++) {
						int i = src + x * 3;
						rgb[y * w + x] = ((pixels.get(i) & 0xFF) << 16)
								| ((pixels.get(i + 1) & 0xFF) << 8)
								|  (pixels.get(i + 2) & 0xFF);
					}
				}
				Thread.ofVirtual().start(() -> {
					try {
						if (path.getParent() != null) {
							Files.createDirectories(path.getParent());
						}
						BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
						img.setRGB(0, 0, w, h, rgb, 0, w);
						ImageIO.write(img, "PNG", path.toFile());
						LOG.info("Screenshot saved: {}", path.toAbsolutePath());
					} catch (IOException e) {
						LOG.error("Failed to write screenshot to {}", path, e);
					}
				});
			} finally {
				memFree(pixels);
			}
		}
	}

	/**
	 * Free all GLFW callbacks, dispose the {@link ResourceManager}, and release the error callback.
	 * Called automatically at the end of {@link #displayLoop()}; subclasses that override this
	 * must call {@code super.dispose()}.
	 */
	public void dispose() {
		if (glfwKeyCallback != null) glfwKeyCallback.close();
		if (glfwFramebufferSizeCallback != null) glfwFramebufferSizeCallback.close();
        if (glfwWindowCloseCallback != null) glfwWindowCloseCallback.close();
		resourceManager.dispose();
		if (errorCallback != null) errorCallback.free();
	}


	/** Reset the window to its initial width and height and clear the scale step counter. */
	public void resetWindowSize() {
		windowScale = 0;
		glfwSetWindowSize(windowHandle, initialWidth, initialHeight);
	}

	/** Double the window size by incrementing the scale step; each step multiplies dimensions by 2. */
	public void scaleWindowUp() {
		windowScale++;
		applyWindowScale();
	}

	/** Halve the window size by decrementing the scale step. */
	public void scaleWindowDown() {
		windowScale--;
		applyWindowScale();
	}

	private void applyWindowScale() {
		int w = windowScale >= 0 ? initialWidth  << windowScale : initialWidth  >> (-windowScale);
		int h = windowScale >= 0 ? initialHeight << windowScale : initialHeight >> (-windowScale);
		glfwSetWindowSize(windowHandle, Math.max(w, 1), Math.max(h, 1));
	}

	/**
	 * Toggle between windowed and fullscreen mode on the monitor that currently contains the
	 * largest area of this window.
	 *
	 * <p>When entering fullscreen the current windowed rectangle is saved so it can be restored
	 * exactly when toggling back. Uses the native monitor's video mode to set resolution and
	 * refresh rate.</p>
	 */
	public void toggleFullscreen() {
		if (windowed == null) {
			windowed = readWindow();
			long monitor = glfwGetCurrentMonitor(windowHandle);
			GLFWVidMode mode = glfwGetVideoMode(monitor);
			glfwSetWindowMonitor(windowHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
		} else {
			glfwSetWindowMonitor(windowHandle, NULL, windowed.x, windowed.y, windowed.width, windowed.height, 0 );
			windowed = null;
		}
	}

	/**
	 * Determine which monitor has the greatest overlap with the given window.
	 *
	 * <p>Iterates all connected monitors and computes the intersection area with the window
	 * rectangle; returns the monitor with the largest overlap. Falls back to the primary monitor
	 * if no overlap is found (e.g. window is off-screen).</p>
	 *
	 * @param window the GLFW window handle
	 * @return the GLFW monitor handle that best contains the window
	 */
	protected static long glfwGetCurrentMonitor(long window) {
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
	/**
	 * Request a clean shutdown by signalling GLFW to close the window.
	 * The loop exits after the current frame finishes; {@link #dispose()} is still called.
	 */
	protected void exit() {
		System.out.println("Exit");
		glfwSetWindowShouldClose(windowHandle, true);
	}

	private void updateTitle() {
		glfwSetWindowTitle(windowHandle,windowTitle + " ["+windowSizeString()+"]");
	}

	/**
	 * Returns a compact string describing the current window pixel dimensions (e.g. {@code "1920x1080"}).
	 * Used to augment the window title bar.
	 *
	 * @return the window size as {@code "<width>x<height>"}
	 */
	public String windowSizeString() {
		return window.getWidth()+"x"+window.getHeight();
	}

	public Rectangle getWindow() {
		return window;
	}

	@Override
	public void addResizeListener(ResizeListener listener) {
		resizeListeners.add(listener);
	}

	@Override
	public void removeResizeListener(ResizeListener listener) {
		resizeListeners.remove(listener);
	}

	/**
	 * Returns the current window dimensions as a 2D float vector.
	 *
	 * @return {@code (width, height)} in pixels; same values as {@link #getWindow()}.width/height
	 */
	public Vector2f getWindowDimensions() {
		return new Vector2f(window.width, window.height);
	}


	public Matrix4f ortho() {
		float left = 0;
		float right = window.width;

		float top = 0;
		float bottom = window.height;

		float near = -1.0f;
		float far = 1.0f;

		return new Matrix4f().ortho(left, right, bottom, top, near, far);
	}

	private Rectangle readWindow() {
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(windowHandle, pWidth, pHeight);

			IntBuffer pX = stack.mallocInt(1);
			IntBuffer pY = stack.mallocInt(1);
			if (Platform.get() != Platform.LINUX) {
				glfwGetWindowPos(windowHandle, pX, pY);
			}

			return new Rectangle(pX.get(0), pY.get(0), pWidth.get(0), pHeight.get(0));
		}
	}
}
