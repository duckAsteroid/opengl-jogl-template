package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.UpdateHint;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is a simple renderer for rendering strings using a FontTexture.
 * Users must supply the text, position (up to a maximum number of characters) to render.
 * And then supply a FontTexture to use for rendering.
 */
public class StringRenderer implements RenderedItem {
    private static final Logger LOG = LoggerFactory.getLogger(StringRenderer.class);
    private static final List<Vertice> fourCorners = Vertice.standardFourVertices().toList();
    private static final int[] indices = Vertice.standardSixVertices().mapToInt(fourCorners::indexOf).toArray();
    /**
     * The font texture used to draw the string
     */
    private final FontTexture fontTexture;
    /**
     * The maximum number of characters this renderer can handle
     */
    private final int maxChars;
    /**
     * Whether to enforce a hard limit on maxChars. If true, an exception is thrown if exceeded.
     * If false, text is truncated to maxChars.
     */
    private final boolean hardLimit = true;
    /**
     * Where on screen the string is rendered
     */
    private Point position = new Point(0, 100);
    /**
     * The text to render
     */
    private String text;
    /**
     * A shader used to render the text
     */
    private ShaderProgram shaderProgram;
    private TextureUnit textureUnit;

    private final VertexElement screenPosition = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
    private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

    private final VertexArrayObject vao = new VertexArrayObject();
    private ElementBufferObject ebo;
    private VertexBufferObject vbo;
    private final AtomicBoolean requiresUpdate = new AtomicBoolean(true);

    public StringRenderer(FontTexture fontTexture, int maxChars) {
        this.fontTexture = Objects.requireNonNull(fontTexture, "Font texture cannot be null");
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        this.maxChars = maxChars;
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        initTexture(ctx);
        initShader(ctx);

        textureUnit.useInShader(shaderProgram, "tex");

        initBuffers(ctx);
        initVariables(ctx);
    }

    private void initShader(RenderContext ctx) {
        // create the shader
        shaderProgram = ctx.getResourceManager().getSimpleShader("passthru2");
        shaderProgram.use(ctx);
    }

    private void initTexture(RenderContext ctx) {
        this.textureUnit = ctx.getResourceManager().nextTextureUnit();
        textureUnit.activate();
        var tex = fontTexture.getTexture();
        textureUnit.bind(tex);
    }

    private void initBuffers(RenderContext ctx) {
        // init buffers
        vao.init(ctx);
        // element buffer
        ebo = vao.createEbo(indices.length * maxChars);
        ebo.init(ctx);
        ebo.clear();

        // vertex buffer
        var vds = new VertexDataStructure(screenPosition, texturePosition);
        vbo = vao.createVbo(vds, fourCorners.size() * maxChars);
        vbo.init(ctx);
        vbo.setup(shaderProgram);
    }
    
    private void initVariables(RenderContext ctx) {
        // put the ortho matrix into the shader
        Matrix4f ortho = ctx.ortho();
        shaderProgram.uniforms().get("projection", Matrix4f.class).set(ortho);
        // set the text color for the shader
        shaderProgram.uniforms().get("textColor", Vector4f.class).set(StandardColors.LIGHTBLUE.color);
    }


    public FontTexture getFontTexture() {
        return fontTexture;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        if (text.length() > maxChars) {
            if (hardLimit) {
                throw new IllegalArgumentException("Text length exceeds maxChars");
            }
            else {
                text = text.substring(0, maxChars);
            }
        }
        this.text = text;
        requiresUpdate.set(true);
    }

    public Point getPosition() {
        return position;
    }

    public void setPosition(Point position) {
        this.position = position;
        requiresUpdate.set(true);
    }

    public void setTextColor(Vector4f color) {
        shaderProgram.uniforms().get("textColor", Vector4f.class).set(color);
    }

    /**
     * Calculate the vertex data for the current text and position values.
     * @param ctx    The render context
     * @param cursor The starting position to render the text, the cursor will be advanced as each glyph is processed
     */
    private void calculateVertexData(RenderContext ctx, Point cursor) {
        ebo.clear();
        for(int i = 0; i < text.length(); i++) {
            var c = text.charAt(i);
            // get the glyph for this character
            var glyph = fontTexture.getGlyph(c);

            // the bounds of the glyph in texture coordinates
            Vector4f texture = glyph.normalBounds();
            // the screen bounds (where to draw the glyph)
            final var screen = glyph.rawBounds(cursor);
            // add the debug line for the glyph datum
            final Vector2f datum = glyph.datum(cursor);

            // populate the vertex data buffer with the screen and texture positions of each vertice
            for(int j = 0; j < fourCorners.size(); j++) {
                Vertice v = fourCorners.get(j);
                // create an index buffer to point at the vertices of the triangles
                // put the screen position in the vertex data element
                vbo.setElement((i * 4) + j, screenPosition, v.from(screen));
                // put the texture position in the vertex data element
                vbo.setElement((i * 4) + j, texturePosition, v.from(texture));
            }
            // add the indices for this glyph to the index buffer
            for(int j = 0; j < indices.length; j++) {
                ebo.put((short)((i * 4) + indices[j]));
            }
            // advance the cursor for the next glyph
            cursor.x += glyph.advance();
        }

        vao.bind(ctx);
        vao.setDrawMode(BufferDrawMode.TRIANGLES);

        vbo.update(UpdateHint.DYNAMIC);
        ebo.update();

    }

    @Override
    public void doRender(RenderContext ctx) {
        shaderProgram.use(ctx);
        vao.bind(ctx);
        // calculate the vertex data for the current text and position
        if (requiresUpdate.get()) {
            calculateVertexData(ctx, new Point(position));
            requiresUpdate.set(false);
        }
        // draw the text
        vao.doRender(ctx);
    }

    @Override
    public void dispose() {
        shaderProgram.dispose();
        vao.dispose();
    }
}
