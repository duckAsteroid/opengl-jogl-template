package com.asteroid.duck.opengl.util.resources.texture;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Interface for loading texture data from a file.
 *
 * This interface defines a method for loading image data from a file specified by a {@link Path} object.
 * The image data is returned as an {@link ImageData} object, which contains the pixel data and metadata.
 *
 * The {@link ImageOptions} parameter allows for customization of the image loading process, such as specifying
 * the desired image format or enabling/disabling mipmapping.
 *
 * @author YourName
 * @since 1.0
 */
public interface TextureDataLoader {
    /**
     * Loads image data from the specified file path using the given image options.
     *
     * @param path The path to the image file.
     * @param options The image loading options.
     * @return The loaded image data.
     * @throws IOException If an error occurs while reading the image file.
     */
    ImageData load(Path path, ImageOptions options) throws IOException;
}
