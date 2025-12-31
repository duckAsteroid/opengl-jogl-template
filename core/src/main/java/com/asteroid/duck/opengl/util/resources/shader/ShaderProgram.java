package com.asteroid.duck.opengl.util.resources.shader;

import com.asteroid.duck.opengl.util.resources.Resource;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariableType;
import com.asteroid.duck.opengl.util.resources.shader.vars.Variable;
import com.asteroid.duck.opengl.util.resources.shader.vars.VariableType;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * A wrapper/utility class for working with shader programs.
 * Provides easy ways to load & compile shader source.
 *
 * <p>To load shaders from resource files - see {@link ShaderLoader}</p>
 */
public class ShaderProgram implements Resource {
	private static final Logger LOG = LoggerFactory.getLogger(ShaderProgram.class);
	final int id;

	private final ShaderSource vertex;
	private final ShaderSource fragment;
	private final ShaderSource geometry;

	private final Map<VariableType, Map<String, Variable>> variableCache = new HashMap<>();

	private final Uniforms uniforms;

	private ShaderProgram(int id, ShaderSource vertex, ShaderSource fragment, ShaderSource geometry) {
		this.id = id;
		this.vertex = vertex;
		this.fragment = fragment;
		this.geometry = geometry;
		this.uniforms = new Uniforms(this);
	}

	public int id() {
		return id;
	}



	public static ShaderProgram compile(ShaderSource vertexSource, ShaderSource fragmentSource, ShaderSource geometrySource) {
		Objects.requireNonNull(vertexSource, "Source for vertex shader must not be null");
		if (vertexSource.isSourceBlank()) throw new IllegalArgumentException("Source for vertex shader must not be blank");
		Objects.requireNonNull(fragmentSource, "Source for fragment shader must not be null");
		if (fragmentSource.isSourceBlank()) throw new IllegalArgumentException("Source for fragment shader must not be blank");

		int sVertex, sFragment, gShader = -1;
		final boolean hasGeometry = geometrySource != null && !geometrySource.isSourceBlank();
		// vertex Shader
		sVertex = glCreateShader(GL_VERTEX_SHADER);
		glShaderSource(sVertex, vertexSource.source());
		glCompileShader(sVertex);
		checkCompileErrors(sVertex,  CompilationChecker.SHADER);
		// fragment Shader
		sFragment = glCreateShader(GL_FRAGMENT_SHADER);
		glShaderSource(sFragment, fragmentSource.source());
		glCompileShader(sFragment);
		checkCompileErrors(sFragment,  CompilationChecker.SHADER);
		// if geometry shader source code is given, also compile geometry shader
		if (hasGeometry)
		{
			gShader = glCreateShader(GL_GEOMETRY_SHADER);
			glShaderSource(gShader, geometrySource.source());
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
		return new ShaderProgram(id, vertexSource, fragmentSource, geometrySource);
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

	/**
	 * Access to the uniform variables of this shader program
	 * @return an object to get/set uniforms
	 */
	public Uniforms uniforms() {
		return uniforms;
	}

	public Map<String, Variable> get(VariableType type) {
		if (!variableCache.containsKey(type)) {
			variableCache.put(type, read(type));
		}
		return variableCache.get(type);
	}

	/**
	 * Builds a VertexDataStructure representing the attribute variables of this shader program.
	 * @return a VertexDataStructure for this shaders attribute variables
	 */
	public VertexDataStructure getAttributeStructure() {
		Map<String, Variable> attrs = get(VariableType.ATTRIBUTE);
		List<VertexElement> elements = attrs.values().stream()
				.sorted(Comparator.comparingInt(Variable::location))
				.map(v -> new VertexElement(VertexElementType.from(v.dataType()), v.name()))
				.collect(Collectors.toList());
		return new VertexDataStructure(elements);
	}

	public Map<String, Variable> read(VariableType type) {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			// how many variables of this type?
			final int count = type.count(id);
			// how long is the longest variable name (this helps allocate buffer to read names into)
			final int maxNameLength = type.maxNameLength(id);
			// holders for reading variable data
			ByteBuffer nameHolder = stack.malloc(maxNameLength);
			IntBuffer sizeHolder = stack.mallocInt(1);
			IntBuffer typeHolder = stack.mallocInt(1);
			IntBuffer lengthHolder = stack.mallocInt(1);
			// a map to hold the results
			HashMap<String, Variable> result = new HashMap<>(count);
			// read each variable
			for (int i = 0; i < count; i++) {
				// clear holders
				lengthHolder.clear();
				sizeHolder.clear();
				typeHolder.clear();
				nameHolder.clear();
				// read variable data
				type.readVariable(id, i, lengthHolder, sizeHolder, typeHolder, nameHolder);
				nameHolder.limit(lengthHolder.get(0));
				// convert the name to a string
				final String name = StandardCharsets.US_ASCII.decode(nameHolder).toString();
				// what type is it?
				ShaderVariableType variableType = ShaderVariableType.from(typeHolder.get(0));
				// create the variable and add to results
				Variable var = new Variable(type, name, sizeHolder.get(0), variableType, i);
				// put the variable in the results map
				result.put(name, var);
			}
			return result;
		}
	}

	public int getAttributeLocation(String attributeName) {
		int location = glGetAttribLocation(id, attributeName);
		if (location < 0) {
			throw new IllegalArgumentException(String.format("Attribute '%s' not found in shader program %s", attributeName, id));
		}
		return location;
	}

	public void dispose() {
		uniforms.clear();
		glDeleteProgram(id);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("ShaderProgram");
		sb.append("(id=").append(id).append("):\n");
		sb.append('\t').append("vertex=").append(vertex.location()).append("\n");
		sb.append('\t').append("fragment=").append(fragment.location()).append("\n");
		if (geometry != null && !geometry.isSourceBlank()) {
			sb.append('\t').append("geometry=").append(geometry.location()).append("\n");
		}
		sb.append("----------------------------\n");
		Map<String, Variable> vars = get(VariableType.UNIFORM);
		String uniforms = vars.values().stream().sorted().map(Objects::toString).collect(Collectors.joining(", ", "uniforms={", "}; "));
		sb.append('\t').append(uniforms).append("\n");
		vars = get(VariableType.ATTRIBUTE);
		String attributes = vars.values().stream().sorted().map(Objects::toString).collect(Collectors.joining(", ","attributes={", "}; "));
		sb.append('\t').append(attributes).append("\n");
		return sb.toString();
	}
}
