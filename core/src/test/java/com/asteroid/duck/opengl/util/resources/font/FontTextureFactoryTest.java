package com.asteroid.duck.opengl.util.resources.font;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class FontTextureFactoryTest {

	@Disabled("Requires an active OpenGL context — calls glGenTextures() via TextureFactory")
	@Test
	void createFontTexture() {
		Font monospacedFont = new Font(Font.SERIF, Font.PLAIN, 24);
		FontTextureFactory subject = new FontTextureFactory(monospacedFont, true);
		FontTexture fontTexture = subject.createFontTexture();
		assertNotNull(fontTexture);
	}
}
