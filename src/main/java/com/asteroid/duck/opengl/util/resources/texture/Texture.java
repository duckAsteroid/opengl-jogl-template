package com.asteroid.duck.opengl.util.resources.texture;

import com.asteroid.duck.opengl.util.resources.Resource;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;


public class Texture implements Resource {
	private static final Logger LOG = LoggerFactory.getLogger(Texture.class);


	    /**
     * An interface that represents an OpenGL coded object.
     * This interface provides methods to retrieve the OpenGL code and parameters associated with the object.
     */
    public interface OpenGLCoded extends Supplier<Integer> {
        /**
         * Returns the OpenGL code associated with this object.
         *
         * @return the OpenGL code
         */
        int openGlCode();

        /**
         * Returns an array of OpenGL parameters associated with this object.
         *
         * @return an array of OpenGL parameters
         */
        int[] openGlParams();

        @Override
        default Integer get() {
            return openGlCode();
        }

        /**
         * Returns a stream of OpenGL parameters used to apply this code
         *
         * @return a stream of OpenGL parameters
         */
        default IntStream openGlParamsStream() {
            return IntStream.of(openGlParams());
        }
    }

	/** The basic dimensions of a texture: 1- or 2-dimensional */
	public enum Dimensions implements OpenGLCoded {
		/** A 1D Texture */
		ONE_DIMENSION(GL_TEXTURE_1D),
		/** A 2D Texture */
		TWO_DIMENSION(GL_TEXTURE_2D);
		private final int openGlCode;

		Dimensions(int openGlCode) {
			this.openGlCode = openGlCode;
		}

		public int openGlCode() {
			return openGlCode;
		}

		public int[] openGlParams() {
			return new int[]{};
		}
	}

	/** How the texture wraps - repeat or stop at edge */
	public enum Wrap implements OpenGLCoded {
		CLAMP_TO_EDGE(GL_CLAMP), REPEAT(GL_REPEAT);
		private final int openGlCode;

		Wrap(int openGlCode) {
			this.openGlCode = openGlCode;
		}

		public int openGlCode() {
			return openGlCode;
		}

		public int[] openGlParams() {
			return new int[]{GL_TEXTURE_WRAP_S, GL_TEXTURE_WRAP_T};
		}
	}

	/**
	 * How the texture is filtered when magnified or minified.
	 * LINEAR interpolation or NEAREST neighbour
	 */
	public enum Filter implements OpenGLCoded {
		LINEAR(GL_LINEAR), NEAREST(GL_NEAREST);

		private final int openGlCode;

		Filter(int openGlCode) {
			this.openGlCode = openGlCode;
		}

		public int openGlCode() {
			return openGlCode;
		}

		public int[] openGlParams() {
			return new int[]{GL_TEXTURE_MIN_FILTER, GL_TEXTURE_MAG_FILTER};
		}
	}

	// e.g. 2D or 1D
	Dimensions dimensions;

	private final int ID;
	public int Width;
	public int Height;
	private int Internal_Format;
	private int Image_Format;
	private int dataType;
	private Wrap wrap;
	private Filter filter;

	public Stream<OpenGLCoded> openGlCodedStream() {
		return Stream.of(wrap, filter);
	}
	/**
	 * Constructs a new Texture object.
	 *
	 * Initializes the texture with default values:
	 * - Width: 0
	 * - Height: 0
	 * - Internal Format: GL_RGB
	 * - Image Format: GL_RGB
	 * - Wrap: repeating
	 * - Filter: Linear interpolation
	 * - Type: 2D
	 * - Data Type: GL_UNSIGNED_BYTE
	 *
	 * Generates a unique texture ID using OpenGL's glGenTextures function.
	 * Logs the creation of the texture with the generated ID.
	 */
	public Texture()
	{
		this.Width = 0;
		this.Height = 0;
		this.Internal_Format = GL_RGB;
		this.Image_Format = GL_RGB;
		this.wrap = Wrap.REPEAT;
		this.filter = Filter.LINEAR;
		this.dimensions = Dimensions.TWO_DIMENSION;
		this.dataType = GL_UNSIGNED_BYTE;
		ID = glGenTextures();
		LOG.info("Created texture ID: {}", ID);
	}

	/**
	 * The width and height dimensions as a 2d vector
	 */
	public Vector2f dimensions() {
		return new Vector2f(Width, Height);
	}

	/**
	 * A matrix that can be used to normalise pixel coordinates
	 */
	public Matrix3x2f normalisationMatrix() {
		return new Matrix3x2f().scale(dimensions());
	}

	// generate an empty texture (e.g. for rendering)
	public void Generate( int width,  int height, long pixels) {
		this.Width = width;
		this.Height = height;
		if (dimensions != Dimensions.TWO_DIMENSION) throw new IllegalArgumentException("Texture type must be 2D");
		// create texture
		glBindTexture(dimensions.openGlCode, this.ID);
		glTexImage2D(dimensions.openGlCode, 0, this.Internal_Format, width, height, 0, this.Image_Format, this.dataType, pixels);
		// set Texture wrap and filter modes
		wrap.openGlParamsStream().forEach(param -> glTexParameteri(dimensions.openGlCode, param, wrap.openGlCode));
		filter.openGlParamsStream().forEach(param -> glTexParameteri(dimensions.openGlCode, param, filter.openGlCode));

		// unbind texture
		glBindTexture(dimensions.openGlCode, 0);
	}

	public void Generate( int width, int height, ByteBuffer data)
	{
		this.Width = width;
		this.Height = height;
		if (dimensions != Dimensions.TWO_DIMENSION) throw new IllegalArgumentException("Texture type must be 2D");
		// create Texture
		Bind();
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glTexImage2D(dimensions.openGlCode, 0, this.Internal_Format, width, height, 0, this.Image_Format, this.dataType, data);
		// set Texture wrap and filter modes
		wrap.openGlParamsStream().forEach(param -> glTexParameteri(dimensions.openGlCode, param, wrap.openGlCode));
		filter.openGlParamsStream().forEach(param -> glTexParameteri(dimensions.openGlCode, param, filter.openGlCode));
		// unbind texture
		UnBind();
	}

	public void Generate1D( int length, ByteBuffer data)
	{
		this.Width = length;
		this.Height = 1;
		this.dimensions = Dimensions.ONE_DIMENSION;
		this.wrap = Wrap.REPEAT;
		// create Texture
		Bind();
		glTexImage1D(this.dimensions.openGlCode, 0, this.Internal_Format, Width, 0, this.Image_Format, GL_UNSIGNED_BYTE, data);
		// set Texture wrap and filter modes
		wrap.openGlParamsStream().forEach(param -> glTexParameteri(dimensions.openGlCode, param, wrap.openGlCode));
		filter.openGlParamsStream().forEach(param -> glTexParameteri(dimensions.openGlCode, param, filter.openGlCode));
		// unbind texture
		UnBind();
	}

	public int id() {
		return ID;
	}

	public void Bind()
	{
		glBindTexture(dimensions.openGlCode, ID);
	}

	public void UnBind()
	{
		glBindTexture(dimensions.openGlCode, 0);
	}

	public void destroy() {
		glDeleteTextures(ID);
	}

	public void setInternalFormat(int fmt) {
		this.Internal_Format = fmt;
	}

	public void setImageFormat(int fmt) {
		this.Image_Format = fmt;
	}

	public void setDataType(int fmt) {
		this.dataType = fmt;
	}

	public void setWrap(Wrap wrap) {
		this.wrap = wrap;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public int getWidth() {
		return Width;
	}
}
