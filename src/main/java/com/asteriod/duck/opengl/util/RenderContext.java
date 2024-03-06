package com.asteriod.duck.opengl.util;

import com.asteriod.duck.opengl.util.resources.ResourceManager;
import com.asteriod.duck.opengl.util.timer.Timer;

import java.awt.*;

public interface RenderContext {
	void registerKeyAction(int key, Runnable runnable);
	void registerKeyAction(int key, int mod, Runnable runnable);
	Rectangle getWindow();
	Timer getTimer();
	ResourceManager getResourceManager();
}
