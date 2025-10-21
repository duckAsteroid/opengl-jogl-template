package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElementType;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VdbVisualizer;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import org.joml.Vector2f;

import java.io.IOException;

public class SimpleTriangle implements Experiment {
    private static final VertexElement POSITION = new VertexElement(VertexElementType.VEC_2F, "position");

    private static final String vertexShaderSource = //language=GLSL
            """
            #version 460
            in vec2 position;
            
            void main()
            {
                gl_Position = vec4( position.x, position.y, 0.0, 1.0);
            }
            """;

    private static final VertexElement COLOR = new VertexElement(VertexElementType.VEC_4F, "color");

    private static final String fragmentShaderSource = //language=GLSL
            """
            #version 460
            out vec4 FragColor;
            uniform vec4 fColor = vec4(1.0, 0.5, 0.2, 1.0);

            void main()
            {
                FragColor = fColor;
            }
            """;
    private ShaderProgram program;
    private VertexDataBuffer vbo;

    @Override
    public String getDescription() {
        return "Renders a triangle using the worlds most basic render pipeline";
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        VertexDataStructure structure = new VertexDataStructure(POSITION);//, COLOR);
        this.vbo = new VertexDataBuffer(structure, 3);
        this.vbo.setUpdateHint(VertexDataBuffer.UpdateHint.STATIC);
        this.vbo.init(ctx);

        // 1. Compile the shader program first
        this.program = ShaderProgram.compile(vertexShaderSource, fragmentShaderSource, null);
        // 2. Setup the VBO with the program to configure attributes
        vbo.setup(program);

        // 3. Now, populate the buffer with data
        Vector2f[] verticeCoords = {
                new Vector2f(-.5f, -.5f), // bottom left [0]
                new Vector2f(.5f, -.5f), // bottom right [1]
                new Vector2f(0.0f, .5f) // top middle [2]
        };
        for (int i = 0; i < verticeCoords.length; i++) {
            vbo.set(i, verticeCoords[i]);//, colors[i].color);
        }
        vbo.update(VertexDataBuffer.UpdateHint.STATIC);

        // This is a great debugging tool, now it will show the data you just set
        VdbVisualizer viz = new VdbVisualizer(vbo);
        System.out.println(viz);
    }

    @Override
    public void doRender(RenderContext ctx) {
        program.use();
        vbo.use();
        vbo.render(0, 3);
    }

    @Override
    public void dispose() {
        program.destroy();
        vbo.destroy();
    }
}
