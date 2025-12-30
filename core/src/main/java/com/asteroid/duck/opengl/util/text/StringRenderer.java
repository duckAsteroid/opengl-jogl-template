package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector2f;

import java.io.IOException;

public class StringRenderer implements RenderedItem {
    /**
     * The font texture used to draw the string
     */
    private FontTexture font;
    /**
     * Where on screen the string is rendered
     */
    private Vector2f position;
    /**
     * The text to render
     */
    private String text;
    /**
     * A shader used to render the text
     */
    private ShaderProgram shaderProgram;

    private VertexArrayObject vao;


    public StringRenderer(FontTexture font, String text) {
        this.font = font;
        this.text = text;
        VertexDataStructure structure = new VertexDataStructure(
                new VertexElement(VertexElementType.VEC_2F, "position"),
                new VertexElement(VertexElementType.VEC_2F, "texCoords"),
                new VertexElement(VertexElementType.VEC_3F, "color"));
        this.vao = new VertexArrayObject();
        vao.createVbo(structure, 100);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Vector2f getPosition() {
        return position;
    }

    public void setPosition(Vector2f position) {
        this.position = position;
    }


    @Override
    public void init(RenderContext ctx) throws IOException {
        this.shaderProgram = ctx.getResourceManager().getShaderLoader().LoadSimpleShaderProgram("text");
        vao.init(ctx);
        vao.getVbo().setup(shaderProgram);
    }

    @Override
    public void doRender(RenderContext ctx) {

    }

    @Override
    public void dispose() {
        shaderProgram.dispose();
        vao.dispose();
    }
}
