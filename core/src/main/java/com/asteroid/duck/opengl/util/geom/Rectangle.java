package com.asteroid.duck.opengl.util.geom;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * A vertex data buffer that contains a rectangle made from two triangles.
 */
public class Rectangle {
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
    private final VertexDataStructure structure;
    private final VertexBufferObject vbo;

    public Rectangle(String scrnPosVertexAttrName, String texPosVertexAttrName) {

        // setup our VBO with screen and texture positions for 4 corners of a square
        List<VertexElement> elements = Stream.of(scrnPosVertexAttrName, texPosVertexAttrName)
                .filter(Objects::nonNull)
                .map(name -> new VertexElement(VertexElementType.VEC_2F, name))
                .toList();

        this.structure = new VertexDataStructure(elements);
        final var scrnPos = elements.get(0);
        final var texPos = elements.size() > 1 ? elements.get(1) : null;
        this.vbo = vao.createVbo(structure, 4);
        this.vao.init(null);
        final List<Vertice> fourCorners = Vertice.standardFourVertices().toList();
        for(int i = 0; i < fourCorners.size(); i++) {
            Vertice corner = fourCorners.get(i);
            Vector2f screenPosition = corner.from(SCREEN_NORMAL);
            vbo.setElement(i, scrnPos, screenPosition);
            if (texPos != null) {
                Vector2f texPosition = corner.from(TEXTURE_NORMAL);
                vbo.setElement(i, texPos, texPosition);
            }
        }

        this.ebo =vao.createEbo(6);
        ebo.init();
        List<Short> indices = Vertice.standardSixVertices().map(v -> (short) fourCorners.indexOf(v)).toList();
        ebo.update(indices);
    }

    public ElementBufferObject getIndexBuffer() {
        return ebo;
    }

    public VertexBufferObject getVertexDataBuffer() {
        return vbo;
    }



    public void destroy() {
        vbo.dispose();
        ebo.dispose();
    }

    public void render(RenderContext ctx) {
        vao.doRender(ctx);
    }
}
