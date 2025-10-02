package com.asteroid.duck.opengl.util.resources.shader.vars;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A shader variable that uses a {@link Supplier} to provide values.
 * This provides a nice way for renderitems to control variables in the shader -
 * by invoking functions they pass to the variable.
 * @param <T> the type of variable
 */
public abstract class ShaderVariable<T> implements BiConsumer<RenderContext, ShaderProgram> {
	protected final String name;
	protected final Function<RenderContext, T> provider;

	protected ShaderVariable(String name, Function<RenderContext, T> provider) {
		this.name = name;
		this.provider = provider;
	}

	public static <T> Function<RenderContext, T> wrap(Supplier<T> supplier) {
		return (ctx) -> supplier.get();
	}

	public static ShaderVariable<Boolean> booleanVariable(String name, Supplier<Boolean> supplier) {
		return booleanVariable(name, wrap(supplier));
	}

	public static ShaderVariable<Boolean> booleanVariable(String name, Function<RenderContext, Boolean> provider) {
    return new ShaderVariable<>(name, provider) {
	    public void accept(RenderContext ctx, ShaderProgram program) {
		    program.uniforms().get(name, Boolean.class).set(provider.apply(ctx));
	    }
    };
  }

	public static ShaderVariable<Float> floatVariable(String name, Supplier<Float> supplier) {
		return floatVariable(name, wrap(supplier));
	}

	public static ShaderVariable<Float> floatVariable(String name, Function<RenderContext, Float> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(RenderContext ctx, ShaderProgram program) {
				program.uniforms().get(name, Float.class).set(provider.apply(ctx));
			}
		};
	}

	public static ShaderVariable<Integer> intVariable(String name, Function<RenderContext, Integer> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(RenderContext ctx, ShaderProgram program) {
				program.uniforms().get(name, Integer.class).set(provider.apply(ctx));
			}
		};
	}

	public static ShaderVariable<Vector2f> vec2fVariable(String name, Supplier<Vector2f> supplier) {
		return vec2fVariable(name, wrap(supplier));
	}

	public static ShaderVariable<Vector2f> vec2fVariable(String name, Function<RenderContext, Vector2f> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(RenderContext ctx, ShaderProgram program) {
				program.uniforms().get(name, Vector2f.class).set(provider.apply(ctx));
			}
		};
	}

	public static ShaderVariable<Vector4f> vec4fVariable(String name, Function<RenderContext, Vector4f> provider) {
		return new ShaderVariable<>(name, provider) {
			public void accept(RenderContext ctx, ShaderProgram program) {
				program.uniforms().get(name, Vector4f.class).set(provider.apply(ctx));
			}
		};
	}
}
