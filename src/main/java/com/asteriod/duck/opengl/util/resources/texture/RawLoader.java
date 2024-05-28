package com.asteriod.duck.opengl.util.resources.texture;

import org.lwjgl.BufferUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class RawLoader implements TextureDataLoader {
	private final Dimension size;

	public RawLoader(Dimension size) {
		this.size = size;
	}

	@Override
	public ImageData load(Path path, ImageOptions options) throws IOException {
		byte[] bytes = Files.readAllBytes(path);
		ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
		buffer.put(bytes);
		buffer.flip();
		return new ImageData(buffer, size);
	}
}
