package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.ResourceManager;
import com.asteroid.duck.opengl.util.timer.Timer;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.awt.*;

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
	 * @deprecated Use {@link #getKeyRegistry}
	 */
	@Deprecated
	void registerKeyAction(int key, Runnable runnable);
	/**
	 * @deprecated Use {@link #getKeyRegistry}
	 */
	@Deprecated
	void registerKeyAction(int key, int mod, Runnable runnable);

	/**
	 * The current location and size of the window
	 * @return window
	 */
	Rectangle getWindow();

	/**
	 * The timer in operation - used for elapsed time etc.
	 * @return the timer instance
	 */
	Timer getTimer();

	/**
     * A resource manager for handling resources like
     * {@link com.asteroid.duck.opengl.util.resources.texture.Texture}s
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

}
