package com.asteroid.duck.opengl.util.resources.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Resource loader that loads resources from the classpath relative to a given base class.
 */
public class ClasspathResourceLoader implements ResourceLoader {
    private final Class<?> baseClass;
    private final String basePath;

    public ClasspathResourceLoader(Class<?> baseClass, String basePath) {
        this.baseClass = baseClass;
        this.basePath = basePath.endsWith("/") ? basePath : basePath + "/";
    }

    private InputStream tryOpen(String relativePath) {
        return baseClass.getResourceAsStream(basePath + relativePath);
    }

    @Override
    public InputStream open(String relativePath) throws IOException {
        InputStream stream = tryOpen(relativePath);
        if (stream == null) {
            throw new IOException("Resource not found: " + basePath + relativePath +" from class " + baseClass.getName());
        }
        return stream;
    }

    @Override
    public boolean exists(String relativePath) throws IOException {
        InputStream stream = tryOpen(relativePath);
        if  (stream == null) {
            return false;
        } else {
            stream.close();
            return true;
        }
    }

    @Override
    public ResourceLoader atPath(String relativePath) {
        return new ClasspathResourceLoader(baseClass, basePath + relativePath);
    }
}
