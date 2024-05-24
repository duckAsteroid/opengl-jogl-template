package com.asteriod.duck.opengl.util.translate;

import com.asteriod.duck.opengl.util.resources.texture.Texture;
import com.asteriod.duck.opengl.util.resources.texture.TextureLoader;

import java.awt.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL30.GL_RG16UI;

public class TranslateMap {
	private final Texture map;

	public TranslateMap(Dimension dims, String mapResource) throws IOException {
		InputStream rawStream = TranslateMap.class.getResourceAsStream(mapResource);
		if (rawStream == null) throw new IOException("Resource not found: " + mapResource);
		try(DataInputStream stream = new DataInputStream(rawStream)) {
			ByteBuffer bb =  ByteBuffer.wrap(stream.readAllBytes());
			//map = TextureLoader.createTexture(GL_RG16UI, GL_RG16UI, dims.width, dims.height, bb);
			map =null;
		}
	}
}
