package com.asteroid.duck.opengl.util.timer;

import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Callable;

public class TimeSource implements Callable<Double> {
	private final String name;
	private final Callable<Double> function;

    public TimeSource(String name, Callable<Double> function) {
        this.name = name;
        this.function = function;
    }

	@Override
	public Double call() throws Exception {
		return function.call();
	}

	@Override
	public String toString() {
		return name;
	}

	public static Callable<Double> glfwGetTimeInstance() {
		return new TimeSource("GLFW::glfwGetTime", GLFW::glfwGetTime);
	}

	public static Callable<Double> systemCurrentTimeMillisInstance() {
		return new TimeSource("System.currentTimeMillis()",()->System.currentTimeMillis() / 1000.0);
	}

	public static Callable<Double> systemNanoTimeInstance() {
		return new TimeSource("System.nanoTime", () -> System.nanoTime() / 1000000.0);
	}
}
