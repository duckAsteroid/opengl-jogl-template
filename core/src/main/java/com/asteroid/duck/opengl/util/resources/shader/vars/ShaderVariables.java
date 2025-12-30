package com.asteroid.duck.opengl.util.resources.shader.vars;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;

import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.function.BiConsumer;

public class ShaderVariables extends AbstractList<ShaderVariable<?>> {
    private final ArrayList<ShaderVariable<?>> variables = new ArrayList<>();

    @Override
    public int size() {
        return variables.size();
    }

    @Override
    public ShaderVariable<?> get(int index) {
        return variables.get(index);
    }

    @Override
    public ShaderVariable<?> set(int index, ShaderVariable element) {
        return super.set(index, element);
    }

    @Override
    public boolean add(ShaderVariable<?> shaderVariable) {
        return variables.add(shaderVariable);
    }

    @Override
    public ShaderVariable<?> remove(int index) {
        return variables.remove(index);
    }

    public void updateForRender(RenderContext ctx, ShaderProgram program) {
        for(var var : variables) {
            var.accept(ctx, program);
        }
    }
}
