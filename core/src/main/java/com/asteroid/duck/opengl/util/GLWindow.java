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

import com.asteroid.duck.opengl.util.timer.Clock;
import com.asteroid.duck.opengl.util.timer.ClockImpl;
import com.asteroid.duck.opengl.util.timer.TimeSource;
import com.asteroid.duck.opengl.util.timer.Timer;
import org.jcodec.api.awt.AWTSequenceEncoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
	private int viewportWidth;
	private int viewportHeight;
	private int initialWidth;
	private int initialHeight;
	private int windowScale = 0;
	private final List<ResizeListener> resizeListeners = new CopyOnWriteArrayList<>();
	private Vector4f backgroundColor = new Vector4f(0.0f);
	private boolean clearScreen = true;
    private boolean windowClosing = false;


	private final ClockImpl clock = new ClockImpl(TimeSource.glfwGetTimeInstance());
	private Double desiredUpdatePeriod = null;

	/** Non-null when a screenshot has been requested; cleared after the capture executes. */
	private volatile Path pendingCapture = null;

	private record RecordingRequest(Path path, Duration duration) {}
	/** Non-null when a recording has been requested from another thread; consumed on the GL thread. */
	private volatile RecordingRequest pendingRecording = null;
	/** The currently active recording session; only accessed on the GL thread. */
	private RecordingSession activeRecording = null;

	/**
	 * Create and display a GLFW window with an OpenGL 3.3 Core Profile context.
	 *
	 * <p>Convenience overload that uses the OS default window placement.</p>
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
		this(resourceManager, title, width, height, icon, WindowPlacement.defaultPlacement());
	}

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
	 * @param placement       where to place the window on first display; use
	 *                        {@link WindowPlacement#centeredOn(Monitor)} to centre on a specific
	 *                        monitor, {@link WindowPlacement#at(Monitor, int, int)} for an explicit
	 *                        offset, or {@link WindowPlacement#defaultPlacement()} to let the OS decide
	 */
	public GLWindow(ResourceManager resourceManager, String title, int width, int height, String icon, WindowPlacement placement) {
        this.resourceManager = resourceManager;
		this.windowTitle = title;
		this.initialWidth = width;
		this.initialHeight = height;
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

        // Apply requested monitor placement before showing the window
        placement.apply(windowHandle, width, height);

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
            viewportWidth = pWidth.get(0);
            viewportHeight = pHeight.get(0);
            glViewport(0, 0, viewportWidth, viewportHeight);
        }

        String gpuName = glGetString(GL_RENDERER);
        LOG.info("GPU Renderer: {}", gpuName);
    }

	@Override
	public Random getRandom() {
		return ThreadLocalRandom.current();
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

	@Override
	public Clock getClock() {
		return clock;
	}

	@Override
	public Double getDesiredUpdatePeriod() {
		return desiredUpdatePeriod;
	}

	@Override
	public void setDesiredUpdatePeriod(Double period) {
		this.desiredUpdatePeriod = period;
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
		viewportWidth = width;
		viewportHeight = height;
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
		clock.reset();
		init();
		registerKeys();
		printInstructions();

		// loop
		while (!windowClosing)
		{
			glfwPollEvents();

			double lastUpdatePeriod = clock.update();

			if(clearScreen) {
				clearScreen();
			}

			// render
			// ---------------------
			render();

			// frame rate cap
			if (desiredUpdatePeriod != null && desiredUpdatePeriod > lastUpdatePeriod) {
				sleep(desiredUpdatePeriod - lastUpdatePeriod);
			}

			// capture before swap so the image matches exactly what hits the screen
			Path capture = pendingCapture;
			if (capture != null) {
				pendingCapture = null;
				captureFramebuffer(capture);
			}

			// start a pending recording session if one was requested
			RecordingRequest req = pendingRecording;
			if (req != null && activeRecording == null) {
				pendingRecording = null;
				beginRecordingSession(req);
			}

			// feed the current frame into the active recording, or finalise if time is up
			if (activeRecording != null) {
				if (activeRecording.isExpired() || activeRecording.stopping) {
					endRecordingSession(activeRecording);
					activeRecording = null;
				} else {
					captureFrameForRecording(activeRecording);
				}
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
		int maxKeyStrWidth = getKeyRegistry().stream().mapToInt(ka -> ka.getCombination().asSimpleString().length()).max().orElse(0);
		String fmt = "\t%-" + maxKeyStrWidth + "s - %s";
		LOG.info("Keys:");
		for(KeyAction ka : getKeyRegistry()) {
			LOG.info(fmt, ka.getCombination().asSimpleString(), ka.getDescription());
		}
	}

	@Override
	public void captureNextFrame(Path destination) {
		this.pendingCapture = destination;
	}

	@Override
	public void startRecording(Path destination, Duration duration) {
		this.pendingRecording = new RecordingRequest(destination,
				duration.compareTo(Duration.ofMinutes(1)) > 0 ? Duration.ofMinutes(1) : duration);
	}

	@Override
	public void stopRecording() {
		RecordingSession session = activeRecording;
		if (session != null) session.stopping = true;
	}

	/**
	 * Reads the current framebuffer into a {@link BufferedImage} via {@code glReadPixels}.
	 * Flips rows vertically (GL origin is bottom-left). Must be called on the GL thread.
	 */
	private BufferedImage readFramebuffer() {
		try (MemoryStack stack = stackPush()) {
			IntBuffer pw = stack.mallocInt(1), ph = stack.mallocInt(1);
			glfwGetFramebufferSize(windowHandle, pw, ph);
			int w = pw.get(0), h = ph.get(0);
			int stride = w * 3;
			ByteBuffer pixels = memAlloc(stride * h);
			try {
				glReadPixels(0, 0, w, h, GL_RGB, GL_UNSIGNED_BYTE, pixels);
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
				BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
				img.setRGB(0, 0, w, h, rgb, 0, w);
				return img;
			} finally {
				memFree(pixels);
			}
		}
	}

	/**
	 * Captures the current framebuffer as a PNG. Pixel data is read on the GL thread;
	 * file I/O is handed off to a virtual thread to avoid stalling the render loop.
	 */
	private void captureFramebuffer(Path path) {
		BufferedImage img = readFramebuffer();
		Thread.ofVirtual().start(() -> {
			try {
				if (path.getParent() != null) Files.createDirectories(path.getParent());
				ImageIO.write(img, "PNG", path.toFile());
				LOG.info("Screenshot saved: {}", path.toAbsolutePath());
			} catch (IOException e) {
				LOG.error("Failed to write screenshot to {}", path, e);
			}
		});
	}

	/** Starts a new {@link RecordingSession}: opens the encoder and launches the encode thread. */
	private void beginRecordingSession(RecordingRequest req) {
		try {
			if (req.path().getParent() != null) Files.createDirectories(req.path().getParent());
			AWTSequenceEncoder encoder = AWTSequenceEncoder.createSequenceEncoder(req.path().toFile(), 30);
			RecordingSession session = new RecordingSession(req.path(), encoder, getClock().track(req.duration()));
			session.encodeThread = Thread.ofVirtual().start(() -> runEncodeLoop(session));
			activeRecording = session;
			LOG.info("Recording started: {} (duration {}s)", req.path(), req.duration().toSeconds());
		} catch (IOException e) {
			LOG.error("Failed to start recording to {}", req.path(), e);
		}
	}

	/** Signals the session's encode thread to drain the queue and finalise the MP4. */
	private void endRecordingSession(RecordingSession session) {
		session.stopping = true;
		LOG.debug("Recording stopping: {}", session.path);
	}

	/** Reads the current frame and offers it to the session's encode queue (drops if full). */
	private void captureFrameForRecording(RecordingSession session) {
		BufferedImage frame = readFramebuffer();
		if (!session.queue.offer(frame) && !session.frameDropWarned) {
			LOG.warn("Recording frame dropped — JCodec encode slower than render rate");
			session.frameDropWarned = true;
		}
	}

	/** Encode-thread body: drains the queue through JCodec until stopped, then finalises. */
	private void runEncodeLoop(RecordingSession session) {
		try {
			while (!session.stopping || !session.queue.isEmpty()) {
				BufferedImage frame = session.queue.poll(50, TimeUnit.MILLISECONDS);
				if (frame != null) session.encoder.encodeImage(frame);
			}
			session.encoder.finish();
			LOG.info("Recording saved: {}", session.path.toAbsolutePath());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOG.error("Recording encode thread interrupted for {}", session.path, e);
		} catch (IOException e) {
			LOG.error("Failed to encode recording to {}", session.path, e);
		}
	}

	private void sleep(double seconds) {
		long ms = (long) (seconds * 1000.0);
		if (ms > 10) {
			try {
				Thread.sleep(ms);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Free all GLFW callbacks, dispose the {@link ResourceManager}, and release the error callback.
	 * If a recording is in progress it is finalised before resources are destroyed; this blocks
	 * until the encode thread completes (up to 30 s).
	 * Called automatically at the end of {@link #displayLoop()}; subclasses that override this
	 * must call {@code super.dispose()}.
	 */
	public void dispose() {
		if (glfwKeyCallback != null) glfwKeyCallback.close();
		if (glfwFramebufferSizeCallback != null) glfwFramebufferSizeCallback.close();
        if (glfwWindowCloseCallback != null) glfwWindowCloseCallback.close();
		RecordingSession recording = activeRecording;
		if (recording != null) {
			activeRecording = null;
			endRecordingSession(recording);
			if (recording.encodeThread != null) {
				try {
					recording.encodeThread.join(30_000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
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
	 * Returns the {@link Monitor} that currently contains the largest area of this window.
	 * Falls back to the primary monitor if no overlap is found (e.g. the window is off-screen).
	 */
	@Override
	public Monitor getCurrentMonitor() {
		return Monitor.fromHandle(glfwGetCurrentMonitor(windowHandle));
	}

	/**
	 * Move the window to the centre of the given monitor.
	 *
	 * @param monitor the target monitor
	 */
	protected void moveToMonitor(Monitor monitor) {
		WindowPlacement.centeredOn(monitor).apply(windowHandle, window.width, window.height);
		this.window = readWindow();
	}

	/**
	 * Move the window to a pixel offset relative to the given monitor's top-left corner.
	 *
	 * @param monitor the target monitor
	 * @param x       horizontal offset in screen coordinates from the monitor's left edge
	 * @param y       vertical offset in screen coordinates from the monitor's top edge
	 */
	protected void moveToMonitor(Monitor monitor, int x, int y) {
		WindowPlacement.at(monitor, x, y).apply(windowHandle, window.width, window.height);
		this.window = readWindow();
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
	 * Toggle the clock between running and paused.
	 * While paused the render loop continues but {@link #getClock()} stops advancing.
	 */
	protected void toggleClock() {
		clock.togglePaused();
	}

	/**
	 * Advance or rewind the clock by a fixed number of seconds.
	 * Intended for use while paused to scrub through time.
	 *
	 * @param seconds seconds to add; negative values rewind
	 */
	protected void stepClock(double seconds) {
		clock.step(seconds);
	}

	/**
	 * Request a clean shutdown by signalling GLFW to close the window.
	 * The loop exits after the current frame finishes; {@link #dispose()} is still called.
	 */
	protected void exit() {
		LOG.info("Exit");
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
		float right = viewportWidth;

		float top = 0;
		float bottom = viewportHeight;

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

	private static final class RecordingSession {
		final Path path;
		final AWTSequenceEncoder encoder;
		final ArrayBlockingQueue<BufferedImage> queue = new ArrayBlockingQueue<>(30);
		Thread encodeThread;
		final Timer durationTracker;
		volatile boolean stopping;
		boolean frameDropWarned;

		RecordingSession(Path path, AWTSequenceEncoder encoder, Timer durationTracker) {
			this.path = path;
			this.encoder = encoder;
			this.durationTracker = durationTracker;
		}

		boolean isExpired() {
			return durationTracker.hasElapsed();
		}
	}
}
