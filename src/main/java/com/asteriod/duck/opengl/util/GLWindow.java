package com.asteriod.duck.opengl.util;

import com.asteriod.duck.opengl.util.resources.texture.ImageData;
import com.asteriod.duck.opengl.util.resources.ResourceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class GLWindow {
	private static final Logger LOG = LoggerFactory.getLogger(GLWindow.class);

	private long windowHandle;
	private String windowTitle;

	private Rectangle windowed = null;

	public GLWindow(String title, int width, int height, String icon) {
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

		if (icon != null) {
			try (GLFWImage.Buffer icons = GLFWImage.malloc(1)) {
				ImageData imgData = ResourceManager.instance().LoadTextureData(icon, true);
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
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	public abstract void keyCallback(long window, int key, int scancode, int action, int mode);

	public void frameBufferSizeCallback(long window, int width, int height) {
		glViewport(0, 0, width, height);
		updateTitle();
	}

	public void displayLoop() throws IOException {
		// initialize
		// ---------------
		init();

		while (!glfwWindowShouldClose(windowHandle))
		{
			glfwPollEvents();
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

	public abstract void init() throws IOException;
	public abstract void render() throws IOException;
	public abstract void dispose();


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
}
