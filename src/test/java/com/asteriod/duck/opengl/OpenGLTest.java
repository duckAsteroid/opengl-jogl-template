package com.asteriod.duck.opengl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glGetString;

/**
 * Initialise OpenGL
 */
public class OpenGLTest {
	@BeforeAll
	public static void initGL() {
		if(!glfwInit()) throw new RuntimeException("Unable to init GLFW");

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		glfwWindowHint(GLFW_CONTEXT_DEBUG, GLFW_TRUE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		System.out.println("Initialised OpenGL Version: "+glGetString(GL_VERSION));
	}

	@AfterAll
	public static void destroyGL() {
		glfwTerminate();
	}
}
