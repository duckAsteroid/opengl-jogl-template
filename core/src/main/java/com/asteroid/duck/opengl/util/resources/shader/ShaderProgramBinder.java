package com.asteroid.duck.opengl.util.resources.shader;

import com.asteroid.duck.opengl.util.resources.bound.Binder;
import com.asteroid.duck.opengl.util.resources.bound.BindingException;
import com.google.auto.service.AutoService;

import static org.lwjgl.opengl.GL20.glUseProgram;

@AutoService(Binder.class)
public class ShaderProgramBinder implements Binder<ShaderProgram> {
    @Override
    public Class<ShaderProgram> resourceType() {
        return ShaderProgram.class;
    }

    @Override
    public void bind(ShaderProgram program) throws BindingException {
        glUseProgram(program.id());
    }

    @Override
    public ShaderProgram unbind(ShaderProgram resource) throws BindingException {
        glUseProgram(0);
        return null;
    }
}
