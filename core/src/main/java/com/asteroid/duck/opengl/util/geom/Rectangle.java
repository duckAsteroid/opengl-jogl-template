package com.asteroid.duck.opengl.util.geom;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VertexBufferVisualiser;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A vertex data buffer that contains a rectangle made from two triangles.
 */
public class Rectangle {
    private static final Logger LOG = LoggerFactory.getLogger(Rectangle.class);
    /**
     * Screen normal coordinates are from -1,-1 (bottom left) to +1, +1 (top right)
     */
    public static Vector4f SCREEN_NORMAL = new Vector4f(-1,-1,1,1);
    /**
     * Texture normal coordinates are from 0,0 (bottom left) to 1, 1 (top right)
     */
    public static Vector4f TEXTURE_NORMAL = new Vector4f(0,0,1,1);

    private final VertexArrayObject vao = new VertexArrayObject();
    private final ElementBufferObject ebo;
    private final VertexBufferObject vbo;

    public Rectangle(String scrnPosVertexAttrName, String texPosVertexAttrName) {
        require(scrnPosVertexAttrName, "Screen position vertex attribute name");
        var scrnPos = new VertexElement(VertexElementType.VEC_2F, scrnPosVertexAttrName);

        require(texPosVertexAttrName, "Texture position vertex attribute name");
        var texPos = new VertexElement(VertexElementType.VEC_2F, texPosVertexAttrName);
        // setup our VBO with screen and texture positions for 4 corners of a square
        List<VertexElement> elements = List.of(scrnPos, texPos);
        VertexDataStructure structure = new VertexDataStructure(elements);
        this.vbo = vao.createVbo(structure, 4);
        this.vao.init(null);
        // put the four corners into the VBO
        final List<Vertice> fourCorners = Vertice.standardFourVertices().toList();
        for(int i = 0; i < fourCorners.size(); i++) {
            Vertice corner = fourCorners.get(i);
            Vector2f screenPosition = corner.from(SCREEN_NORMAL);
            Vector2f texPosition = corner.from(TEXTURE_NORMAL);
            vbo.set(i, screenPosition, texPosition);
        }
        // commit the data to the VBO
        vbo.update(UpdateHint.STATIC);

        // create an element buffer to "index" the four corners
        this.ebo =vao.createEbo(6);
        ebo.init();
        List<Short> indices = Vertice.standardSixVertices().map(v -> (short) fourCorners.indexOf(v)).toList();
        ebo.update(indices);
        vao.unbind();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Rectangle VAO: {}", new VertexBufferVisualiser(vao));
        }
    }

    private static void require(String value, String message) {
        Objects.requireNonNull(value, message);
        if(value.isBlank()) {
            throw new IllegalArgumentException(message + " cannot be emtpy");
        }
    }

    public VertexArrayObject getVertexArrayObject() {
        return vao;
    }

    public ElementBufferObject getElementBufferObject() {
        return ebo;
    }

    public VertexBufferObject getVertexBufferObject() {
        return vbo;
    }

    public void destroy() {
        vbo.dispose();
        ebo.dispose();
    }

    public void render(RenderContext ctx) {
        vao.doRender(ctx);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Rectangle:\n");
        sb.append('\t').append(new VertexBufferVisualiser(vao)).append('\n');
        return sb.toString();
    }
}
