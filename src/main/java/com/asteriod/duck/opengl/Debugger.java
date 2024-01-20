package com.asteriod.duck.opengl;

import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLDebugListener;
import com.jogamp.opengl.GLDebugMessage;

import static com.jogamp.opengl.GL.GL_DONT_CARE;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;

public class Debugger implements GLDebugListener {

	public void messageSent(GLDebugMessage event) {
		System.out.println(event);
		//throw new RuntimeException(event.getDbgMsg());
	}

	public void Debugger(GLWindow window, GL4 gl) {
		window.getContext().addGLDebugListener(this);

		gl.glDebugMessageControl(
						GL_DONT_CARE,
						GL_DONT_CARE,
						GL_DONT_CARE,
						0,
						null,
						false);

		gl.glDebugMessageControl(
						GL_DONT_CARE,
						GL_DONT_CARE,
						GL_DEBUG_SEVERITY_HIGH,
						0,
						null,
						true);

		gl.glDebugMessageControl(
						GL_DONT_CARE,
						GL_DONT_CARE,
						GL_DEBUG_SEVERITY_MEDIUM,
						0,
						null,
						true);
	}
}
