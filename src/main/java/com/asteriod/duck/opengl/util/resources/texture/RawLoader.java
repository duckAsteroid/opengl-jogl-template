package com.asteriod.duck.opengl.util.resources.texture;

import org.lwjgl.BufferUtils;

import java.awt.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A class that implements the TextureDataLoader interface and loads texture data from a raw file.
 * It always creates images of the same "size" (whatever it's initialised with)
 */
public class RawLoader implements TextureDataLoader {
    /**
     * The size of the texture data.
     */
    private final Dimension size;

    /**
     * Constructs a new RawLoader with the given size.
     *
     * @param size The size of the texture data.
     */
    public RawLoader(Dimension size) {
        this.size = size;
    }

    /**
     * Loads texture data from the specified file path and options.
     *
     * @param path    The file path of the raw texture data.
     * @param options The image options for loading the texture data. These are ignored in this implementation
     * @return The loaded image data.
     * @throws IOException If an error occurs while reading the file.
     */
    @Override
    public ImageData load(Path path, ImageOptions options) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
        buffer.put(bytes);
        buffer.flip();
        return new ImageData(buffer, size);
    }
}
