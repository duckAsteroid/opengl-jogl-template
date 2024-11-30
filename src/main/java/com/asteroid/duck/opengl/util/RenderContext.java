package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.resources.ResourceManager;
import com.asteroid.duck.opengl.util.timer.Timer;

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

	Double getDesiredUpdatePeriod();
	void setDesiredUpdatePeriod(Double period);
}
