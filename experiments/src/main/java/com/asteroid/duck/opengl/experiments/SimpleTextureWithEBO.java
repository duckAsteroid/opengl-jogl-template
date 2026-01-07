package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VertexBufferVisualiser;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.io.IOException;

/**
 * This is essentially a duplicate of {@link SimpleTexture} - except it uses an element
 */
public class SimpleTextureWithEBO implements Experiment {
    @Override
    public String getDescription() {
        return "Renders a texture directly. Does not use PassthruTextureRenderer. Does use an element buffer object";
    }

    private VertexArrayObject vao;

    private static final String textureName = "molly";
    private static final String texturePath = "molly.jpg";
    private Texture texture;
    private TextureUnit textureUnit;

    private ShaderProgram shader;
    //language=GLSL
    private static final String VERTEX_SHADER = """
            #version 330
            // the screen position of the vertex
            in vec2 screenPosition;
            // the texture coordinates of the vertex
            in vec2 texturePosition;
            
            
            // output of texture coordinates to fragment shader
            out vec2 texCoords;
            
            void main() {
                // each vertex has 2 coords X&Y - we need XYZ+Depth for gl_Position
                gl_Position = vec4(screenPosition, 0.0, 1.0);
                // pass texture coordinates to vertex shader
                texCoords = texturePosition;
            }
            """;

    //language=GLSL
    private static final String FRAG_SHADER = """
            #version 460
            
            precision mediump float;
            
            // the texture coordinates passed in from the vertex shader
            in vec2 texCoords;
            // the output color of the pixel
            out vec4 fragColor;
            // the texture to sample from
            uniform sampler2D tex;
            
            void main() {
                // copy the texel straight into the pixel
                fragColor = texture(tex, texCoords);
            }
            """;
    @Override
    public void init(RenderContext ctx) throws IOException {
        this.shader = ShaderProgram.compile(
                ShaderSource.fromClass(VERTEX_SHADER, SimpleTextureWithEBO.class),
                ShaderSource.fromClass(FRAG_SHADER, SimpleTextureWithEBO.class)
                , null);
        shader.use();
        this.texture = initTexture(ctx);
        this.textureUnit = initTextureUnit(ctx);
        this.vao = initBuffers(ctx);
        this.vao.getVbo().setup(shader);
    }

    private static final Vector4f SCREEN = new Vector4f(-1,-1,1,1);
    private static final Vector4f TEXTURE = new Vector4f(0,0,1,1);

    private VertexArrayObject initBuffers(RenderContext ctx) {
        vao = new VertexArrayObject();
        vao.init(ctx);

        // setup a VBO
        VertexDataStructure vertexDataStructure = new VertexDataStructure(
                new VertexElement(VertexElementType.VEC_2F, "screenPosition"),
                new VertexElement(VertexElementType.VEC_2F, "texturePosition"));

        var fourVertices = Vertice.standardFourVertices().toList();
        VertexBufferObject vbo = vao.createVbo(vertexDataStructure, fourVertices.size());
        vbo.init();

        for (int i = 0; i < fourVertices.size(); i++) {
            Vertice v = fourVertices.get(i);
            Vector2f screen = v.from(SCREEN);
            Vector2f texture = v.from(TEXTURE);
            vbo.set(i, screen, texture);
        }
        vbo.update(UpdateHint.STATIC);

        // *****************************************************************************
        // THIS IS THE KEY DIFFERENCE! It uses a vertex index buffer to avoid duplicating vertex data!
        // *****************************************************************************

        var sixVertices = Vertice.standardSixVertices().toList();
        ElementBufferObject ebo = vao.createEbo(sixVertices.size());
        ebo.init();
        for (int i = 0; i < sixVertices.size(); i++) {
            Vertice v = sixVertices.get(i);
            int index = fourVertices.indexOf(v);
            ebo.put((short)index);
        }
        ebo.update();

        VertexBufferVisualiser visualiser = new VertexBufferVisualiser(vao);
        System.out.println(visualiser);
        return vao;
    }

    private TextureUnit initTextureUnit(RenderContext ctx) {
        TextureUnit textureUnit = ctx.getResourceManager().nextTextureUnit();
        textureUnit.bind(texture);
        textureUnit.useInShader(shader, "tex");
        return textureUnit;
    }

    private Texture initTexture(RenderContext ctx) {
        return ctx.getResourceManager().getTexture(textureName, texturePath);
    }


    @Override
    public void doRender(RenderContext ctx) {
        vao.bind();
        vao.doRender(ctx);
    }

    @Override
    public void dispose() {
        vao.dispose();
        shader.dispose();
    }
}
