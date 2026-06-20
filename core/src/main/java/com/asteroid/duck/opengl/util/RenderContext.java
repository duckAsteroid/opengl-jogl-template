package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.events.ResizeListener;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.manager.ResourceManager;
import com.asteroid.duck.opengl.util.timer.Timer;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;
import java.nio.file.Path;
import java.util.Random;

/**
 * The interface that OpenGL rendered components can use to interact with the context (typically the
 * window) they are rendered in.
 */
public interface RenderContext {
	/**
	 * A key registry to set up key handlers
	 * @return the key registry
	 */
	KeyRegistry getKeyRegistry();

	/**
	 * The current location and size of the window viewport in pixels.
	 * This is used for rendering and also resize events.
	 * @return window
	 */
	Rectangle getWindow();

	// listener for resize events (see above window dimensions)
	void addResizeListener(ResizeListener listener);
	void removeResizeListener(ResizeListener listener);

	/**
	 * The timer in operation - used for elapsed time etc.
	 * @return the timer instance
	 */
	Timer getTimer();

	/**
     * A resource manager for handling resources like
     * {@link Texture}s, {@link com.asteroid.duck.opengl.util.resources.shader.ShaderProgram}s etc
     *
     * @return the resource manager instance
     */
	ResourceManager getResourceManager();

	/**
	 * Should the screen be cleared on each render loop cycle
	 * @param clear true to clear
	 */
	void setClearScreen(boolean clear);

	/**
	 * Get the current state of screen clearing in the render loop
	 * @return true if screen cleared during render cycle
	 */
	boolean isClearScreen();

	/**
	 * The background colour the screen is cleared to in the render loop
	 * (if {@link #isClearScreen()} is true)
	 * @return the current background colour
	 */
	Vector4f getBackgroundColor();
	void setBackgroundColor(Vector4f vector4f);

	/**
	 * The target update period (if defined) in seconds.
	 * <code>null</code> means the programme runs as fast as possible
	 * @return current desired update period in seconds
	 */
	Double getDesiredUpdatePeriod();
	void setDesiredUpdatePeriod(Double period);

	/**
	 * A frequency (hz) based version of the update period
	 * @return the update period (if any) in hertz
	 */
	default Double getDesiredUpdateFrequency() {
		return getDesiredUpdatePeriod() == null ? null : 1.0 / getDesiredUpdatePeriod();
	}

	default void setDesiredUpdateFrequency(Double frequencyInHertz) {
		if (frequencyInHertz != null) {
			setDesiredUpdatePeriod(1.0 / frequencyInHertz);
		}
		else {
			setDesiredUpdatePeriod(null);
		}
	}

	/**
	 * The orthographic projection matrix for the current screen size in pixels.
	 * Using 0,0 as the top left
	 * @return a matrix suitable for use as an orthographic projection
	 */
	Matrix4f ortho();

	/**
	 * A random number generator instance for use by rendered components
	 * @return the random instance
	 */
    Random getRandom();

	/**
	 * Request capture of the next rendered frame as a PNG file.
	 *
	 * <p>The capture is deferred: it occurs after the next {@link RenderedItem#doRender} pass
	 * completes but before the framebuffer is swapped to screen, so the image reflects exactly
	 * what will be displayed. The PNG is written on a background thread to avoid stalling the
	 * render loop.</p>
	 *
	 * <p>Safe to call from any thread (including key callbacks). Only the most recently
	 * requested path is captured if multiple calls arrive before a frame completes.</p>
	 *
	 * @param destination path for the PNG output file
	 */
	default void captureNextFrame(Path destination) { }

	/**
	 * Convenience overload: captures the next frame to a timestamped PNG in the working directory.
	 *
	 * @see #captureNextFrame(Path)
	 */
	default void captureNextFrame() {
		captureNextFrame(Path.of("screenshot-" + System.currentTimeMillis() + ".png"));
	}
}
