package com.asteroid.duck.opengl.util.resources.io;

import java.io.IOException;
import java.io.InputStream;
/**
 * Interface for loading resources as input streams.
 */
public interface ResourceLoader {
    InputStream open(String relativePath) throws IOException;
    ResourceLoader atPath(String relativePath);
    boolean exists(String relativePath) throws IOException;
}
