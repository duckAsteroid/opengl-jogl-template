package com.asteroid.duck.opengl.util.resources.shader;

/**
 * Used to give a shader source and to indicate the location of the resource
 * @param source the actual source code in GLSL
 * @param location a hint about the location the source came from
 */
public record ShaderSource(String source, String location) {
    public static ShaderSource fromClass(String source, Class<?> location) {
        return new ShaderSource(source, "String variable in " + location.getName());
    }

    public boolean isSourceBlank() {
        return source == null || source.isBlank();
    }
}
