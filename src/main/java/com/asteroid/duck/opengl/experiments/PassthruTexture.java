package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.geom.Vertice;
import com.asteroid.duck.opengl.util.RenderContext;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElementType;
import com.asteroid.duck.opengl.util.resources.shader.ShaderProgram;
import com.asteroid.duck.opengl.util.resources.texture.Texture;
import com.asteroid.duck.opengl.util.resources.texture.TextureUnit;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class PassthruTexture implements Experiment {
	private ShaderProgram shaderProgram;
	private TextureUnit textureUnit;
	private Texture texture;
	private VertexDataBuffer buffer;
	private int divisor = 1;
	private int simultaneous = 5;
	private List<List<Map<VertexElement, Vector2f>>> squares = new ArrayList<>();
	private final VertexElement screenPosition = new VertexElement(VertexElementType.VEC_2F, "screenPosition");
	private final VertexElement texturePosition = new VertexElement(VertexElementType.VEC_2F, "texturePosition");

	@Override
	public void init(RenderContext ctx) throws IOException {
		initShader(ctx);
		initTexture(ctx);
		initBuffer(ctx);
		ctx.setClearScreen(true);
		ctx.setDesiredUpdateFrequency(25.0);
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('U'), this::incSimultaneous,"Increase simultaneous squares");
		ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple('J'), this::decSimultaneous,"Decrease simultaneous squares");
	}

	private void decSimultaneous() {
		simultaneous--;
		if (simultaneous < 1) {
			simultaneous = 1;
		}
		System.out.println("Simultaneous: " + simultaneous);
	}

	private void incSimultaneous() {
		simultaneous++;
		if (simultaneous > squares.size()) {
			simultaneous = squares.size();
		}
		System.out.println("Simultaneous: " + simultaneous);
	}


	private void initShader(RenderContext ctx) {
		this.shaderProgram = ctx.getResourceManager().getSimpleShader("passthru2");
		System.out.println(shaderProgram.toString());
	}

	private void initTexture(RenderContext ctx) {
		this.texture = ctx.getResourceManager().getTexture("molly", "molly.jpg");
		shaderProgram.use();
		this.textureUnit = ctx.getResourceManager().nextTextureUnit();
		textureUnit.bind(texture);
		textureUnit.useInShader(shaderProgram, "tex");
	}

	private void initBuffer(RenderContext ctx) {
		//in vec2 screenPosition;
		//in vec2 texturePosition;
		VertexDataStructure structure = new VertexDataStructure(
						screenPosition,
						texturePosition);
		this.buffer = new VertexDataBuffer(structure, 6 * divisor * divisor);
		this.buffer.init();

		this.shaderProgram.use();
		buffer.setup(shaderProgram);
		initSquares();
		updateBufferData();
	}

	/**
	 * Create a set of N x N squares to cover the screen and image where N = divisor
	 * We will then pick N random of these to update the buffer on each paint
	 */
	private void initSquares() {
		float screenSegmentSize = 2.0f / divisor;
		float textureSegmentSize = 1.0f / divisor;
		for(int x = 0; x < divisor; x++) {
			float xScreenStart = -1.0f + (screenSegmentSize * x);
			float xScreenEnd = xScreenStart + screenSegmentSize;
			float xTextureStart = 0f + (textureSegmentSize * x);
			float xTextureEnd = xTextureStart + textureSegmentSize;
			for(int y = 0; y < divisor; y++) {
				float yScreenStart = -1.0f + (screenSegmentSize * y);
				float yScreenEnd = yScreenStart + screenSegmentSize;
				final Vector4f screenSquare = new Vector4f(xScreenStart, yScreenStart, xScreenEnd, yScreenEnd);
				List<Vector2f> screenTriangles = Vertice.standardSixVertices().map(corner -> corner.from(screenSquare)).toList();

				float yTextureStart = 0f + (textureSegmentSize * y);
				float yTextureEnd = yTextureStart + textureSegmentSize;
				final Vector4f textureSquare = new Vector4f(xTextureStart, yTextureStart, xTextureEnd, yTextureEnd);
				List<Vector2f> textureTriangles = Vertice.standardSixVertices().map(corner -> corner.from(textureSquare)).toList();

				List<Map<VertexElement, Vector2f>> squareData = IntStream.range(0, screenTriangles.size())
								.mapToObj(i -> Map.of(screenPosition, screenTriangles.get(i), texturePosition, textureTriangles.get(i)))
								.toList();
				squares.add(squareData);
			}
		}
	}

	@Override
	public void doRender(RenderContext ctx) {
		updateBufferData();
		shaderProgram.use();
		buffer.use();
		buffer.render(0, 6 * simultaneous);
	}

	final Random rnd = new Random();

	private void updateBufferData() {
		if (divisor == 1) {
			this.buffer.set(0, new Vector2f( -1f, 1f), new Vector2f(0f,1f)); // TL
			this.buffer.set(1, new Vector2f( 1f, 1f), new Vector2f(1f,1f)); // TR
			this.buffer.set(2, new Vector2f( 1f, -1f), new Vector2f(1f,0f)); // BR

			this.buffer.set(3, new Vector2f( 1f, -1f), new Vector2f(1f,0f)); // BR
			this.buffer.set(4, new Vector2f( -1f, -1f), new Vector2f(0f,0f)); // BL
			this.buffer.set(5, new Vector2f( -1f, 1f), new Vector2f(0f,1f)); // TL
		}
		else {
			for (int s = 0; s < simultaneous; s++) {
				List<Map<VertexElement, Vector2f>> square = squares.get(rnd.nextInt(squares.size()));
				for (int i = 0; i < square.size(); i++) {
					Map<VertexElement, Vector2f> vertexData = square.get(i);
					this.buffer.set((s * 6) + i, vertexData);
				}
			}
		}
		this.buffer.update(VertexDataBuffer.UpdateHint.DYNAMIC);
	}

	@Override
	public void dispose() {
		textureUnit.destroy();
		shaderProgram.destroy();
		buffer.destroy();
	}

	@Override
	public String getDescription() {
		return "A passthrough texture renderer that uses our new Buffer";
	}
}
