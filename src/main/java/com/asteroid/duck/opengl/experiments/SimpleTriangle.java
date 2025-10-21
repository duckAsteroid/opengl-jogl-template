package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.color.StandardColors;
import com.asteroid.duck.opengl.util.geom.Triangles;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElementType;
import com.asteroid.duck.opengl.util.resources.buffer.debug.VdbVisualizer;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.shader.Uniform;
import com.asteroid.duck.opengl.util.timer.AccumulatorFunction;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.io.IOException;

public class SimpleTriangle implements Experiment {
    private static final VertexElement POSITION = new VertexElement(VertexElementType.VEC_2F, "position");

    private static final String vertexShaderSource = //language=GLSL
            """
            #version 460
            in vec2 position;
            in vec4 color;
            out vec4 fColor;
            
            void main()
            {
                gl_Position = vec4( position.x, position.y, 0.0, 1.0);
                fColor = color;
            }
            """;

    private static final VertexElement COLOR = new VertexElement(VertexElementType.VEC_4F, "color");

    private static final String fragmentShaderSource = //language=GLSL
            """
            #version 460
            out vec4 FragColor;
            in vec4 fColor;
            uniform float time;

            // A common function to rotate the hue of an RGB color
            vec3 hueRotate(vec3 color, float hue)
            {
                vec3 k = vec3(0.57735, 0.57735, 0.57735); // sqrt(1/3)
                float cosAngle = cos(hue);
                // The hue rotation matrix in axis-angle form
                return color * cosAngle + cross(k, color) * sin(hue) + k * dot(k, color) * (1.0 - cosAngle);
            }

            void main()
            {
                // Rotate the hue of the incoming fragment color by 'time' radians
                vec3 rotatedColor = hueRotate(fColor.rgb, time);
                FragColor = vec4(rotatedColor, fColor.a);
            }
            """;
    private ShaderProgram program;
    private VertexDataBuffer vbo;
    private Uniform<Float> timeUniform;
    private AccumulatorFunction accumulator;

    @Override
    public String getDescription() {
        return "Renders a triangle using the worlds most basic render pipeline";
    }

    @Override
    public void init(RenderContext ctx) throws IOException {
        accumulator = new AccumulatorFunction(ctx.getTimer());
        accumulator.setMaxSpeed(10.0);
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('Q'), () -> changeSpeed(2.0), "Much Faster");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simpleWithMods('Q',"SHIFT"), () -> changeSpeed(1.1), "Bit Faster");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simpleWithMods('A', "SHIFT"), () -> changeSpeed(0.9), "Bit Slower");
        ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('A'), () -> changeSpeed(0.5), "Much Slower");

        VertexDataStructure structure = new VertexDataStructure(POSITION, COLOR);
        this.vbo = new VertexDataBuffer(structure, 3);
        this.vbo.setUpdateHint(VertexDataBuffer.UpdateHint.STATIC);
        this.vbo.init(ctx);

        // 1. Compile the shader program first
        this.program = ShaderProgram.compile(vertexShaderSource, fragmentShaderSource, null);
        // 2. Setup the VBO with the program to configure attributes
        vbo.setup(program);

        this.timeUniform = program.uniforms().get("time", Float.class);

        // 3. Now, populate the buffer with data
        Vector2f[] verticeCoords = {
                new Vector2f(-.5f, -.5f), // bottom left [0]
                new Vector2f(.5f, -.5f), // bottom right [1]
                new Vector2f(0.0f, .5f) // top middle [2]
        };
        Vector4f[] verticeColors = {
                StandardColors.RED.color,
                StandardColors.GREEN.color,
                StandardColors.BLUE.color
        };
        for (int i = 0; i < verticeCoords.length; i++) {
            vbo.set(i, verticeCoords[i], verticeColors[i]);
        }
        vbo.update(VertexDataBuffer.UpdateHint.STATIC);

        // This is a great debugging tool, now it will show the data you just set
        VdbVisualizer viz = new VdbVisualizer(vbo);
        System.out.println(viz);
    }

    private void changeSpeed(double v) {
        double speed = accumulator.getSpeed();
        accumulator.setSpeed(speed * v);
    }

    @Override
    public void doRender(RenderContext ctx) {
        // Use the shader program first
        program.use();
        // Update the 'time' uniform with the elapsed time from the render context
        timeUniform.set((float) accumulator.value());
        // Bind the vertex data
        vbo.use();
        // Draw the triangle
        vbo.render(0, 3);
    }

    @Override
    public void dispose() {
        program.destroy();
        vbo.destroy();
    }
}
