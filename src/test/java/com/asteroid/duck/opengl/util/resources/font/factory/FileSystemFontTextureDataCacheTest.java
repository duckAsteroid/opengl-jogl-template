package com.asteroid.duck.opengl.util.resources.font.factory;

import com.asteroid.duck.opengl.util.resources.font.io.FileSystemFontTextureDataCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

class FileSystemFontTextureDataCacheTest {
	@TempDir
	Path tempDir;

	Font[] testFonts = new Font[]{
					new Font("Arial", Font.PLAIN, 124),
					new Font("Courier", Font.PLAIN, 64),
					new Font("Courier", Font.ITALIC, 64),
					new Font("Courier", Font.BOLD, 64),
					new Font("Courier", Font.BOLD & Font.ITALIC, 64),
					new Font("Arial", Font.BOLD, 124),
					new Font("Arial", Font.BOLD, 55),
	};

	@Test
	void store() throws IOException {
		FileSystemFontTextureDataCache cache = new FileSystemFontTextureDataCache(tempDir);
		for(Font font : testFonts) {
			FontTextureDataFactory factory = new FontTextureDataFactory(font, true);
			FontTextureData testData = factory.createFontTextureData();
			cache.store(new FileSystemFontTextureDataCache.FontLocator(font, true), testData);
		}
		System.out.println(tempDir.toAbsolutePath());
		System.out.println("Waiting for debug");
	}
}