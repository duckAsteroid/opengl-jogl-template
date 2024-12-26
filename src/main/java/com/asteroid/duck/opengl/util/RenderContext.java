package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.ResourceManager;
import com.asteroid.duck.opengl.util.timer.Timer;
import org.joml.Vector2f;

import java.awt.*;

public interface RenderContext {
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
	Rectangle getWindow();
	Timer getTimer();
	ResourceManager getResourceManager();

	void setClearScreen(boolean clear);
	boolean isClearScreen();

	/**
	 * The target update period (if defined) in seconds.
	 * <code>null</code> means the programme runs as fast as possible
	 * @return update period in seconds
	 */
	Double getDesiredUpdatePeriod();
	void setDesiredUpdatePeriod(Double period);

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


	Vector2f getWindowDimensions();
}
