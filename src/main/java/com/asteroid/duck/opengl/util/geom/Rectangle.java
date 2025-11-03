package com.asteroid.duck.opengl.util.geom;

import com.asteroid.duck.opengl.util.resources.buffer.*;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.LinkedList;
import java.util.List;

/**
 * A texture rectangle
 */
public class Rectangle {
    /**
     * Screen normal coordinates are from -1,-1 (bottom left) to +1, +1 (top right)
     * @return two pairs of coordinates
     */
    public static Vector4f SCREEN_NORMAL = new Vector4f(-1,-1,1,1);

    public static Vector4f TEXTURE_NORMAL = new Vector4f(0,0,1,1);

    private final VertexDataStructure structure;
    private final IndexBuffer ibo;
    private final VertexDataBuffer vbo;

    public Rectangle(String scrnPosVertexAttrName, String texPosVertexAttrName) {
        // setup our VBO with screen and texture positions for 4 corners of a square
        this.structure = new VertexDataStructure(
                new VertexElement(VertexElementType.VEC_2F, scrnPosVertexAttrName),
                new VertexElement(VertexElementType.VEC_2F, texPosVertexAttrName));
        this.vbo = new VertexDataBuffer(structure, 4);
        this.vbo.init();
        List<Vertice> fourCorners = Vertice.standardFourVertices().toList();
        for(int i = 0; i < fourCorners.size(); i++) {
            Vertice corner = fourCorners.get(i);
            Vector2f screenPosition = corner.from(SCREEN_NORMAL);
            Vector2f texPosition = corner.from(TEXTURE_NORMAL);
            vbo.set(i, screenPosition, texPosition);
        }

        this.ibo = new IndexBuffer(6);
        ibo.init();
        List<Short> indices = Vertice.standardSixVertices().map(v -> (short) fourCorners.indexOf(v)).toList();
        ibo.update(indices);
    }

    public IndexBuffer getIndexBuffer() {
        return ibo;
    }

    public VertexDataBuffer getVertexDataBuffer() {
        return vbo;
    }

    public void destroy() {
        vbo.destroy();
        ibo.destroy();
    }
}
