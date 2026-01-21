package com.asteroid.duck.opengl.util.resources.buffer;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;

import com.asteroid.duck.opengl.util.resources.bound.BindingException;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.opengl.GL30.*;

/**
 * This class represents the top level Vertex Array Object (VAO) that
 * can own child: {@link VertexBufferObject VBO}
 * and {@link com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject EBO}
 */
public class VertexArrayObject  implements RenderedItem {
    private static final Logger log = LoggerFactory.getLogger(VertexArrayObject.class);

    private Integer vao = null;
    /**
     * A vertex buffer object (if bound)
     */
    private VertexBufferObject vbo;
    /**
     * An element buffer object (if bound)
     */
    private ElementBufferObject ebo;
    /**
     * The draw mode used to render this buffer.
     * Default is {@link BufferDrawMode#TRIANGLES}
     */
    private BufferDrawMode drawMode = BufferDrawMode.TRIANGLES;

    public boolean hasVbo() {
        return vbo != null;
    }

    public boolean hasEbo() {
        return ebo != null;
    }

    public VertexBufferObject getVbo() {
        return vbo;
    }

    public int id() throws BindingException {
        if (vao == null) throw new BindingException("Not initialised");
        return vao;
    }

    public VertexBufferObject createVbo(VertexDataStructure structure, int capacity) {
        if (this.vbo != null) {
            throw new IllegalStateException("VBO already exists");
        }
        this.vbo = new VertexBufferObject(this, structure, capacity);
        return vbo;
    }

    public ElementBufferObject getEbo() {
        return ebo;
    }

    public ElementBufferObject createEbo(int capacity) {
        if (this.ebo != null) {
            throw  new IllegalStateException("EBO already exists");
        }
        this.ebo = new ElementBufferObject(this, capacity);
        return ebo;
    }

    public BufferDrawMode getDrawMode() {
        return drawMode;
    }

    public void setDrawMode(BufferDrawMode drawMode) {
        this.drawMode = drawMode;
    }

    public void init(RenderContext ctx) {
        vao = glGenVertexArrays();
        bind(ctx);
        if (vbo != null) {
            vbo.init(ctx);
        }
        if (ebo != null) {
            ebo.init(ctx);
        }
    }

    public void bind(RenderContext ctx) {
        var binding = ctx.getResourceManager().exclusivityGroup(VertexArrayObject.class);
        binding.bind(this);
    }

    protected void unbind(RenderContext ctx) throws BindingException {
        var binding = ctx.getResourceManager().exclusivityGroup(VertexArrayObject.class);
        binding.unbind(this);
    }

    @Override
    public void doRender(RenderContext ctx) {
        bind(ctx);
        if (ebo != null) {
            glDrawElements(drawMode.glCode, ebo.capacity(), ebo.getType(), 0);
        }
        else if (vbo != null) {
            glDrawArrays(drawMode.glCode, 0, vbo.size());
        }
        else {
            log.warn("Neither EBO or VBO initialised");
        }
    }

    @Override
    public void dispose() {
        if (vbo != null) {
            vbo.dispose();
            vbo = null;
        }
        if (ebo != null) {
            ebo.dispose();
            ebo = null;
        }
        if (vao != null) {
            glDeleteVertexArrays(vao);
        }
    }
}
