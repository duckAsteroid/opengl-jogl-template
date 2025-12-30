package com.asteroid.duck.opengl.util.timer;

import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Callable;

public class TimeSource {
	public static Callable<Double> glfwGetTimeInstance() {
		return GLFW::glfwGetTime;
	}

	public static Callable<Double> systemCurrentTimeMillisInstance() {
		return ()->System.currentTimeMillis() / 1000.0;
	}

	public static Callable<Double> systemNanoTimeInstance() {
		return () -> System.nanoTime() / 1000000.0;
	}
}
