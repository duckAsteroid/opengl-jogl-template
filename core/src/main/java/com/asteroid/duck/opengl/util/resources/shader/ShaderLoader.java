package com.asteroid.duck.opengl.util.resources.shader;

import com.asteroid.duck.opengl.util.resources.io.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShaderLoader  {
	private static final Logger LOG = LoggerFactory.getLogger(ShaderLoader.class);
	private final static boolean performIncludesProcessing = !Boolean.getBoolean("shader.ignore.includes");
	private final Loader loader;

	public ShaderLoader(Loader root) {
		this.loader = root;
	}

	public ShaderProgram LoadSimpleShaderProgram(String simple) throws IOException {
		return LoadShaderProgram(simple+"/vertex.glsl", simple+"/frag.glsl", null);
	}

	/**
	 * Loads and compiles a shader from files by path.
	 * @param vertexPath the vertex shader path
	 * @param fragmentPath the fragment shader path
	 * @param geometryPath (optional) the geometry shader path (may be null)
	 * @return the compiled and linked shader program - ready to use
	 * @throws IOException If the files can't be accessed
	 */
	public ShaderProgram LoadShaderProgram(String vertexPath, String fragmentPath, String geometryPath) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loading vert={}, frag={}, geom={}", vertexPath, fragmentPath, geometryPath);
		}
		return ShaderProgram.compile(loadFrom(vertexPath).orElseThrow(), loadFrom(fragmentPath).orElseThrow(), loadFrom(geometryPath).orElse(null));
	}

	private Optional<ShaderSource> loadFrom(String path) throws IOException {
		if (path != null && !path.isBlank()) {
			try(var is = new BufferedReader(new InputStreamReader(loader.open(path),StandardCharsets.UTF_8))) {
				List<String> lines = new ArrayList<>(is.lines().toList());
				if (lines.isEmpty()) return Optional.empty();

				StringBuilder result = new StringBuilder();

				if (performIncludesProcessing) {
					performIncludesProcessing(lines, result);
				}

				// check if we loaded anything at all!
				if (result.toString().isBlank()) return Optional.empty();
				var location = loader.describe(path);
				result.append("\n// Source: ").append(location);
				return Optional.of(new ShaderSource(result.toString(), location));
			}
		}

		return Optional.empty();
	}

	/**
	 * Performs include processing for shader source code. à la C/C+=
	 *
	 * @param lines A list of lines from the shader source code.
	 * @param result A StringBuilder to store the processed shader source code.
	 * @throws IOException If an included file is not found.
	 */
	private void performIncludesProcessing(List<String> lines, StringBuilder result) throws IOException {
		// do include processing
		for (String line : lines) {
			if (line.startsWith("#include ")) {
				// work out what is included
				final String included = line.substring(10).trim();
				// does it exist?
				if (!included.isBlank()) {
					if (loader.exists(included)) {
						// recursively load and include...
						result.append("// BEGIN included from ").append(included).append('\n');
						result.append(loadFrom(included)).append('\n');
						result.append("// END   included from ").append(included).append('\n');
					} else {
						throw new IOException("Included file not found: " + included);
					}
				}
			} else {
				result.append(line).append('\n');
			}
		}
	}




}
