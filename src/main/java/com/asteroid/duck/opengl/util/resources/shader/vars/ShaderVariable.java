package com.asteroid.duck.opengl.util.resources.shader.vars;

import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ShaderVariable<T> implements Consumer<ShaderProgram> {
	protected final String name;
	protected final Supplier<T> provider;

	public ShaderVariable(String name, Supplier<T> provider) {
		this.name = name;
		this.provider = provider;
	}


	public static ShaderVariable<Boolean> booleanVariable(String name, Supplier<Boolean> provider) {
    return new ShaderVariable<>(name, provider) {
	    public void accept(ShaderProgram program) {
		    program.setBoolean(name, provider.get());
	    }
    };
  }

	public static ShaderVariable<Float> floatVariable(String name, Supplier<Float> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(ShaderProgram program) {
				program.setFloat(name, provider.get());
			}
		};
	}

	public static ShaderVariable<Integer> intVariable(String name, Supplier<Integer> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(ShaderProgram program) {
				program.setFloat(name, provider.get());
			}
		};
	}

	public static ShaderVariable<Vector2f> vec2fVariable(String name, Supplier<Vector2f> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(ShaderProgram program) {
				program.setVector2f(name, provider.get());
			}
		};
	}

	public static ShaderVariable<Vector4f> vec4fVariable(String name, Supplier<Vector4f> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(ShaderProgram program) {
				program.setVector4f(name, provider.get());
			}
		};
	}
}
