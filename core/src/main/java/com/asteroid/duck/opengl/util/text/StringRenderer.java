package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.RenderedItem;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.renderaction.RenderActionQueue;
import com.asteroid.duck.opengl.util.resources.buffer.BufferDrawMode;
import com.asteroid.duck.opengl.util.resources.buffer.UpdateHint;
import com.asteroid.duck.opengl.util.resources.buffer.VertexArrayObject;
import com.asteroid.duck.opengl.util.resources.buffer.ebo.ElementBufferObject;
import com.asteroid.duck.opengl.util.resources.buffer.vbo.*;
import com.asteroid.duck.opengl.util.resources.font.FontTexture;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.ShaderSource;
import com.asteroid.duck.opengl.util.resources.textureunit.TextureUnit;
import org.intellij.lang.annotations.Language;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Renders a text string using a {@link FontTexture}.
 * <p>
 * VBO vertices are in string-relative (origin) space — the baseline datum sits at (0, 0). Screen
 * position is applied via a {@code model} matrix uniform, so calling {@link #setPosition} only
 * updates a uniform and never touches the vertex buffer.
 * <p>
 * The VBO and EBO are allocated to exactly match the current text length. When the text length
 * changes the buffers are reallocated; when only the content changes (same length) the existing
 * buffers are updated in place. No rendering occurs until {@link #setText} has been called at
 * least once.
 */
public class StringRenderer implements RenderedItem {
    private static final Logger LOG = LoggerFactory.getLogger(StringRenderer.class);
    private static final List<Vertice> fourCorners = Vertice.standardFourVertices().toList();
    private static final int[] indices = Vertice.standardSixVertices().mapToInt(fourCorners::indexOf).toArray();

    // language="GLSL"
    private static final String VERTEX_GLSL = """
            #version 460
            in vec2 screenPosition;
            in vec2 texturePosition;
            out vec2 texCoords;
            uniform mat4 projection;
            uniform mat4 model;
            void main() {
                gl_Position = projection * model * vec4(screenPosition, 1.0, 1.0);
                texCoords = texturePosition;
            }
            """;


    // language="GLSL"
    private static final String FRAGMENT_GLSL = """
            #version 460
            precision mediump float;
            uniform sampler2D tex;
            uniform vec4 textColor;
            in vec2 texCoords;
            out vec4 fragColor;
            void main() {
                vec4 color = texture(tex, texCoords);
                float mask = color.r;
                fragColor = vec4(textColor.rgb * mask, textColor.a * color.a);
            }
            """;

    private static final String TEXT_UPDATE   = "textUpdate";
    private static final String TEXT_COLOR    = "textColor";
    private static final String TEXT_POSITION = "textPosition";

    private final RenderActionQueue renderActions = new RenderActionQueue(TEXT_UPDATE, TEXT_COLOR, TEXT_POSITION);

    private final FontTexture fontTexture;

    private Point position = new Point(0, 0);
    private String text;

    private ShaderProgram shaderProgram;
    private TextureUnit textureUnit;

    private final Matrix4f model = new Matrix4f();

    private final VertexElement screenPosition  = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
    private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

    private final VertexArrayObject vao = new VertexArrayObject();
    private ElementBufferObject ebo;
    private VertexBufferObject vbo;

    /** Number of characters the current VBO/EBO were allocated for; 0 means not yet allocated. */
    private int allocatedLength = 0;


    public StringRenderer(FontTexture fontTexture) {
        this.fontTexture = Objects.requireNonNull(fontTexture, "Font texture cannot be null");
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        initTexture(ctx);
        initShader(ctx);
        textureUnit.useInShader(shaderProgram, "tex");
        initUniforms(ctx);
    }

    private void initTexture(RenderContext ctx) {
        textureUnit = ctx.getResourceManager().nextTextureUnit();
        textureUnit.activate();
        textureUnit.bind(fontTexture.getTexture());
    }

    private void initShader(RenderContext ctx) {
        shaderProgram = ShaderProgram.compile(
                ShaderSource.fromClass(VERTEX_GLSL, StringRenderer.class),
                ShaderSource.fromClass(FRAGMENT_GLSL, StringRenderer.class),
                null);
        shaderProgram.use(ctx);
    }

    private void initUniforms(RenderContext ctx) {
        shaderProgram.uniforms().get("projection", Matrix4f.class).set(ctx.ortho());
        model.identity().translate(position.x, position.y, 0);
        shaderProgram.uniforms().get("model", Matrix4f.class).set(model);
        shaderProgram.uniforms().get("textColor", Vector4f.class).set(StandardColors.WHITE.color);
    }

    public FontTexture getFontTexture() {
        return fontTexture;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
        renderActions.enqueue(TEXT_UPDATE, ctx -> rebuildBuffers(ctx));
    }

    /**
     * Returns the screen position of the text baseline datum in AWT pixel coordinates.
     * The origin (0, 0) is the top-left of the window; x increases rightward and y increases
     * downward. The returned point is the baseline anchor: ascenders render above it (lower y)
     * and descenders below (higher y).
     */
    public Point getPosition() {
        return position;
    }

    /**
     * Sets the screen position of the text baseline datum.
     * Coordinates are in AWT pixel space: origin (0, 0) at the top-left of the window, x
     * increasing rightward, y increasing downward. The point anchors the font baseline — glyphs
     * with ascenders will render above this y value and descenders below it.
     * <p>
     * This only updates the {@code model} matrix uniform; the vertex buffer is not rebuilt.
     *
     * @param position baseline anchor in window pixel coordinates
     */
    public void setPosition(Point position) {
        this.position = position;
        Point pos = new Point(position);
        renderActions.enqueue(TEXT_POSITION, ctx -> {
            model.identity().translate(pos.x, pos.y, 0);
            shaderProgram.uniforms().get("model", Matrix4f.class).set(model);
        });
    }

    public void setTextColor(Vector4f color) {
        renderActions.enqueue(TEXT_COLOR, ctx ->
            shaderProgram.uniforms().get(TEXT_COLOR, Vector4f.class).set(color));
    }

    /**
     * Updates buffers for the current text. Reallocates the VAO/VBO/EBO when the text length has
     * changed; otherwise updates the existing buffers in place.
     */
    private void rebuildBuffers(RenderContext ctx) {
        if (text.isEmpty()) {
            allocatedLength = 0;
            return;
        }
        if (text.length() != allocatedLength) {
            reallocate(ctx);
        }
        fontTexture.computeVertexData(text, vbo, screenPosition, texturePosition);
        ebo.clear();
        for (int i = 0; i < text.length(); i++) {
            for (int j = 0; j < indices.length; j++) {
                ebo.put((short) (i * 4 + indices[j]));
            }
        }
        vao.bind(ctx);
        vao.setDrawMode(BufferDrawMode.TRIANGLES);
        vbo.update(UpdateHint.DYNAMIC);
        ebo.update();
    }

    /**
     * Disposes the existing VAO/VBO/EBO (if any) and creates new ones sized to the current text.
     * Must only be called when {@code text} is non-empty.
     */
    private void reallocate(RenderContext ctx) {
        if (allocatedLength > 0) {
            vao.dispose();
        }
        int len = text.length();
        vao.createEbo(indices.length * len);
        vao.createVbo(new VertexDataStructure(screenPosition, texturePosition), fourCorners.size() * len);
        vao.init(ctx);
        vbo = vao.getVbo();
        ebo = vao.getEbo();
        vbo.setup(shaderProgram);
        allocatedLength = len;
        LOG.debug("Reallocated text buffers for {} characters", len);
    }

    @Override
    public void doRender(RenderContext ctx) {
        shaderProgram.use(ctx);
        renderActions.processAll(ctx);
        if (allocatedLength > 0) {
            vao.doRender(ctx);
        }
    }

    @Override
    public void dispose() {
        shaderProgram.dispose();
        vao.dispose();
    }
}
