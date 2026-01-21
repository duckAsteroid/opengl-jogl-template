package com.asteroid.duck.opengl.util.resources.buffer.ebo;

import com.asteroid.duck.opengl.util.resources.bound.Binder;
import com.asteroid.duck.opengl.util.resources.bound.BindingException;
import com.google.auto.service.AutoService;

import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.glBindBuffer;

@AutoService(Binder.class)
public class ElementBufferObjectBinder implements Binder<ElementBufferObject> {
    @Override
    public Class<ElementBufferObject> resourceType() {
        return ElementBufferObject.class;
    }

    @Override
    public void bind(ElementBufferObject ebo) throws BindingException {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo.id());
    }

    @Override
    public ElementBufferObject unbind(ElementBufferObject resource) throws BindingException {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        return null;
    }
}
