package com.asteroid.duck.opengl.util.resources.font;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FontTextureFactoryTest {

	@Test
	void createFontTexture() {
		Font monospacedFont = new Font(Font.SERIF, Font.PLAIN, 24);
		FontTextureFactory subject = new FontTextureFactory(monospacedFont, true);
		subject.setImageDumpPath(Path.of("font-images"));
		FontTexture fontTexture = subject.createFontTexture();
		assertNotNull(fontTexture);
	}
}
