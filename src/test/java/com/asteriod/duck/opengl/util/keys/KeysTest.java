package com.asteriod.duck.opengl.util.keys;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class KeysTest {
	Keys subject = Keys.instance();

	@Test
	public void keyFor() {
		Key a = subject.keyFor('A');
		assertNotNull(a);
		assertEquals("A", a.name());
		assertEquals(GLFW.GLFW_KEY_A, a.code());
	}

}
