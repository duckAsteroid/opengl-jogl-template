package com.asteroid.duck.opengl.util.resources.shader;

import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariableType;
import com.asteroid.duck.opengl.util.resources.shader.vars.Variable;
import com.asteroid.duck.opengl.util.resources.shader.vars.VariableType;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30C.glUniform1ui;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * A wrapper/utility class for working with shader programs.
 * Provides easy ways to load & compile shader source.
 * Adds support for <pre>#include filename</pre> pre-processor inclusions
 * Some basic utilities to cache uniform indexes.
 */
public class ShaderProgram implements Resource {
	private static final Logger LOG = LoggerFactory.getLogger(ShaderProgram.class);
	private final int id;

	private final HashMap<String, Integer> uniformLocationCache = new HashMap<>();

	private ShaderProgram(int id) {
		this.id = id;
	}

	public int id() {
		return id;
	}

	public static ShaderProgram compile(Path vertexPath, Path fragmentPath, Path geometryPath, IncludesHandler includesHandler) throws IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loading vert={}, frag={}, geom={}", vertexPath, fragmentPath, geometryPath);
		}
		return compile(loadFrom(vertexPath, includesHandler), loadFrom(fragmentPath, includesHandler), loadFrom(geometryPath, includesHandler));
	}

	private static String loadFrom(Path p, IncludesHandler includes) throws IOException {
		if (p != null) {
			List<String> lines = new ArrayList<>(Files.readAllLines(p, StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder();
			if (includes != null) {
				performIncludesProcessing(includes, lines, result);
			}
			result.append("\n// Source: ").append(p.toAbsolutePath());
			return result.toString();
		}
		else return null;
	}

	/**
	 * Performs include processing for shader source code. à la C/C+=
	 *
	 * @param includes An instance of {@link IncludesHandler} to locate included files.
	 * @param lines A list of lines from the shader source code.
	 * @param result A StringBuilder to store the processed shader source code.
	 * @throws IOException If an included file is not found.
	 */
	private static void performIncludesProcessing(IncludesHandler includes, List<String> lines, StringBuilder result) throws IOException {
		// do include processing
		for (String line : lines) {
			if (line.startsWith("#include ")) {
				// work out what is included
				final String included = line.substring(10).trim();
				Path pathIncluded = includes.find(included);
				// does it exist?
				if (Files.exists(pathIncluded)) {
					// recursively load and include...
					result.append("// BEGIN included from ").append(included).append('\n');
					result.append(loadFrom(pathIncluded, includes)).append('\n');
					result.append("// END   included from ").append(included).append('\n');
				}
				else {
					throw new IOException("Included file not found: " + included);
				}
			} else {
				result.append(line).append('\n');
			}
		}
	}

	public static ShaderProgram compile(String vertexSource, String fragmentSource, String geometrySource) {
		int sVertex, sFragment, gShader = -1;
		final boolean hasGeometry = geometrySource != null && !geometrySource.isBlank();
		// vertex Shader
		sVertex = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(sVertex, vertexSource);
		glCompileShader(sVertex);
		checkCompileErrors(sVertex,  CompilationChecker.SHADER);
		// fragment Shader
		sFragment = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(sFragment, fragmentSource);
		glCompileShader(sFragment);
		checkCompileErrors(sFragment,  CompilationChecker.SHADER);
		// if geometry shader source code is given, also compile geometry shader
		if (hasGeometry)
		{
			gShader = glCreateShader(GL_GEOMETRY_SHADER);
			glShaderSource(gShader, geometrySource);
			glCompileShader(gShader);
			checkCompileErrors(gShader, CompilationChecker.SHADER);
		}
		// shader program
		final int id = glCreateProgram();
		glAttachShader(id, sVertex);
		glAttachShader(id, sFragment);
		if (hasGeometry)
			glAttachShader(id, gShader);
		glLinkProgram(id);
		checkCompileErrors(id,  CompilationChecker.PROGRAM);
		// delete the shaders as they're linked into our program now and no longer necessary
		glDeleteShader(sVertex);
		glDeleteShader(sFragment);
		if (hasGeometry) {
			glDeleteShader(gShader);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loaded shader {}", id);
		}
		return new ShaderProgram(id);
	}


	public enum CompilationChecker implements Function<Integer, Optional<String>> {
		SHADER {
			@Override
			public Optional<String> apply(Integer shader) {
				int[] success = new int[1];
				glGetShaderiv(shader, GL_COMPILE_STATUS, success);
				if (success[0] == GL_FALSE) {
					return Optional.of(glGetShaderInfoLog(shader));

				}
				return Optional.empty();
			}
		}
		,
		PROGRAM{
			@Override
			public Optional<String> apply(Integer program) {
				int[] success = new int[1];
				glGetProgramiv(program, GL_LINK_STATUS, success);
				if (success[0] == GL_FALSE) {
					return Optional.of(glGetProgramInfoLog(program));

				}
				return Optional.empty();
			}
		};
	}

	private static void checkCompileErrors(int object, CompilationChecker type) {
		Optional<String> error = type.apply(object);
		if (error.isPresent()) {
			throw new RuntimeException(error.get());
		}
	}

	public ShaderProgram use() {
		glUseProgram(id);
		return this;
	}

	public void unuse() {
		glUseProgram(0);
	}

	private final Map<VariableType, Map<String, Variable>> variableCache = new HashMap<>();

	public Map<String, Variable> get(VariableType type) {
		if (!variableCache.containsKey(type)) {
			variableCache.put(type, read(type));
		}
		return variableCache.get(type);
	}

	public Map<String, Variable> read(VariableType type) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			final int count = type.count(id);
			final int maxNameLength = type.maxNameLength(id);
			ByteBuffer nameHolder = stack.malloc(maxNameLength);
			IntBuffer sizeHolder = stack.mallocInt(1);
			IntBuffer typeHolder = stack.mallocInt(1);
			IntBuffer lengthHolder = stack.mallocInt(1);
			HashMap<String, Variable> result = new HashMap<>(count);
			for (int i = 0; i < count; i++) {
				lengthHolder.clear();
				sizeHolder.clear();
				typeHolder.clear();
				nameHolder.clear();
				type.readVariable(id, i, lengthHolder, sizeHolder, typeHolder, nameHolder);
				nameHolder.limit(lengthHolder.get(0));
				final String name = StandardCharsets.US_ASCII.decode(nameHolder).toString();
				ShaderVariableType variableType = ShaderVariableType.from(typeHolder.get(0));
				Variable var = new Variable(type, name, sizeHolder.get(0), variableType, i);
				result.put(name, var);
			}
			return result;
		}
	}

	protected int uniformLocation(String uniformName) {
		if (!uniformLocationCache.containsKey(uniformName)) {
			int uniformLocation = glGetUniformLocation(id, uniformName);
			uniformLocationCache.put(uniformName, uniformLocation);
		}
		return uniformLocationCache.get(uniformName);
	}

	public void setBoolean(String name, boolean value) {
		glUniform1i(uniformLocation(name), value ? 1 : 0);
	}

	public void setFloat(String name, float value)
	{
		glUniform1f(uniformLocation(name), value);
	}

	public void setFloatArray(String name, float[] value)
	{
		glUniform1fv(uniformLocation(name), value);
	}

	public void setInteger(String name, int value)
	{
		glUniform1i(uniformLocation(name), value);
	}

	public void setUnsignedInteger(String name, int value)
	{
		glUniform1ui(uniformLocation(name), value);
	}

	public void setVector2f(String name, float x, float y)
	{
		glUniform2f(uniformLocation(name), x, y);
	}
	public void setVector2f(String name, Vector2f value)
	{
		glUniform2f(uniformLocation(name), value.x, value.y);
	}
	public void setVector3f(String name, float x, float y, float z)
	{
		glUniform3f(uniformLocation(name), x, y, z);
	}
	public void setVector3f(String name, Vector3f value)
	{
		glUniform3f(uniformLocation(name), value.x, value.y, value.z);
	}
	public void setVector4f(String name, float x, float y, float z, float w)
	{
		glUniform4f(uniformLocation(name), x, y, z, w);
	}
	public void setVector4f(String name, Vector4f value)
	{
		GL20C.glUniform4fv(uniformLocation(name), new float[]{value.x, value.y, value.z, value.w});
	}

	public void setMatrix4(String name, Matrix4f matrix)
	{
		try (MemoryStack stack = MemoryStack.stackPush()) {
			FloatBuffer fb = matrix.get(stack.mallocFloat(16));
			glUniformMatrix4fv(uniformLocation(name), false, fb);
		}
	}

	public void setVertexAttribPointer(String name, int size, int type, boolean normalized, int stride, long pointer) {
		int positionAttribute = glGetAttribLocation(id, name);
		glEnableVertexAttribArray(positionAttribute);
		glVertexAttribPointer(positionAttribute, size, type, normalized, stride, pointer);
	}

	public void destroy() {
		uniformLocationCache.clear();
		glDeleteProgram(id);
	}

	@Override
	public String toString() {
		Map<String, Variable> vars = get(VariableType.UNIFORM);
		String uniforms = vars.values().stream().sorted().map(Objects::toString).collect(Collectors.joining(", ", "uniforms={", "}; "));
		vars = get(VariableType.ATTRIBUTE);
		String attributes = vars.values().stream().sorted().map(Objects::toString).collect(Collectors.joining(", ","attributes={", "}; "));
		return "ShaderProgram(id="+id+"): " + uniforms + attributes;
	}
}
