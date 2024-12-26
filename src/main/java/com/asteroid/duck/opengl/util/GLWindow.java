package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.keys.*;
import com.asteroid.duck.opengl.util.resources.ResourceManager;
import com.asteroid.duck.opengl.util.resources.texture.ImageData;
import com.asteroid.duck.opengl.util.resources.texture.ImageOptions;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class GLWindow implements RenderContext {
	private static final Logger LOG = LoggerFactory.getLogger(GLWindow.class);

	private final long windowHandle;
	private final String windowTitle;

	private final ResourceManager resourceManager = new ResourceManager("src/main/");
	private final GLFWKeyCallback glfwKeyCallback;
	private final GLFWFramebufferSizeCallback glfwFramebufferSizeCallback;
	private final KeyRegistry keyRegistry = new KeyRegistry();
	private GLFWErrorCallback errorCallback;
	private Rectangle windowed = null;
	private Rectangle window;
	private Vector4f backgroundColor = new Vector4f(0.0f);
	private boolean clearScreen = true;

	public GLWindow(String title, int width, int height, String icon) {
		this.windowTitle = title;
		//System.out.println("INFO: OpenGL Version: "+glGetString(GL_VERSION));
		//this.errorCallback = GLFWErrorCallback.createPrint(System.err).set();

		if(!glfwInit()) throw new RuntimeException("Unable to init GLFW");

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

		window = readWindow();

		updateTitle();

		if (icon != null) {
			try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
				ImageData imgData = resourceManager.LoadTextureData(icon, ImageOptions.DEFAULT.withNoFlip());
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

	public void registerKeyAction(int key, Runnable runnable) {
		Key knownKey = Keys.instance().keyFor(key).orElseThrow(() -> new IllegalArgumentException("Unknown key code: " + key));
		keyRegistry.registerKeyAction(new KeyCombination(Set.of(knownKey), Collections.emptySet()), runnable, null);
	}

	public void registerKeyAction(int key, int mod, Runnable runnable) {
		Key knownKey = Keys.instance().keyFor(key).orElseThrow(() -> new IllegalArgumentException("Unknown key code: " + key));
		Set<Key> knownMods = Keys.instance().modsFor(mod);
		keyRegistry.registerKeyAction(new KeyCombination(Set.of(knownKey), knownMods), runnable, null);
	}



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

	public void frameBufferSizeCallback(long window, int width, int height) {
		int[] wwidth={0};
		int[] wheight={0};
		glfwGetFramebufferSize(window, wwidth, wheight);

		glViewport(0, 0, width, height);
		this.window = readWindow();
		updateTitle();
	}

	public void displayLoop() throws IOException {
		// initialize
		// ---------------
		init();
		registerKeys();
		printInstructions();

		// loop
		while (!glfwWindowShouldClose(windowHandle))
		{
			glfwPollEvents();

			if(clearScreen) {
				clearScreen();
			}

			// render
			// ---------------------
			render();

			glfwSwapBuffers(windowHandle);
		}

		// delete all resources
		// ---------------------
		dispose();

		glfwFreeCallbacks(windowHandle);
		glfwDestroyWindow(windowHandle);

		glfwSetErrorCallback(null);

		glfwTerminate();
	}

	public void clearScreen() {
		glClearColor(backgroundColor.x, backgroundColor.y, backgroundColor.z, backgroundColor.w);
		glClear(GL_COLOR_BUFFER_BIT);
	}

	public abstract void registerKeys();

	public abstract void init() throws IOException;
	public abstract void render() throws IOException;

	public void printInstructions() {
		System.out.println("Keys:");
		int maxKeyStrWidth = getKeyRegistry().stream().mapToInt(ka -> ka.getCombination().asSimpleString().length()).max().orElse(0);
		for(KeyAction ka : getKeyRegistry()) {
			System.out.printf("\t%-"+maxKeyStrWidth+ "s - %s%n", ka.getCombination().asSimpleString(), ka.getDescription());
		}
	}

	public void dispose() {
		if (glfwKeyCallback != null) glfwKeyCallback.close();
		if (glfwFramebufferSizeCallback != null) glfwFramebufferSizeCallback.close();
		resourceManager.clear();
		if (errorCallback != null) errorCallback.free();
	}


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
	protected void exit() {
		System.out.println("Exit");
		glfwSetWindowShouldClose(windowHandle, true);
	}

	private void updateTitle() {
		glfwSetWindowTitle(windowHandle,windowTitle + " ["+windowSizeString()+"]");
	}

	public String windowSizeString() {
		return window.getWidth()+"x"+window.getHeight();
	}

	public Rectangle getWindow() {
		return window;
	}

	public Vector2f getWindowDimensions() {
		return new Vector2f(window.width, window.height);
	}

	private Rectangle readWindow() {
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
}
