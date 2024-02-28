package com.asteriod.duck.opengl.util.resources.shader;

import com.asteriod.duck.opengl.util.resources.impl.AbstractResourceLoader;

import java.io.IOException;
import java.nio.file.Path;

public class ShaderLoader extends AbstractResourceLoader<ShaderProgram> {

	public ShaderLoader(Path root) {
		super(root);
	}


	public ShaderProgram LoadShaderProgram(String vertex, String fragment, String geometry) throws IOException {
		Path geomPath = (geometry == null || geometry.isBlank()) ? null : getPath(geometry);
		return LoadShaderProgram(getPath(vertex), getPath(fragment), geomPath);
	}

	/**
	 * Loads and compiles a shader from files by path.
	 * @param vertex the vertex shader path
	 * @param fragment the fragment shader path
	 * @param geometry (optional) the geometry shader path (may be null)
	 * @return the compiled and linked shader program - ready to use
	 * @throws IOException If the files can't be accessed
	 */
	public ShaderProgram LoadShaderProgram(Path vertex, Path fragment, Path geometry) throws IOException {
		final ShaderProgram shaderProgram = ShaderProgram.compile(vertex, fragment, geometry);
		System.out.println("Loaded shader to "+shaderProgram.id());
		return shaderProgram;
	}

}
