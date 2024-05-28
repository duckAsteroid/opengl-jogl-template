package com.asteriod.duck.opengl.util.resources.texture;

import com.asteriod.duck.opengl.util.resources.impl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.lwjgl.opengl.GL11.*;


public class Texture implements Resource {
	private static final Logger LOG = LoggerFactory.getLogger(Texture.class);

	public interface OpenGLCoded extends Supplier<Integer> {
		int openGlCode();
		int[] openGlParams();
		default Integer get() {
			return openGlCode();
		}
		default IntStream  openGlParamsStream() {
			return IntStream.of(openGlParams());
		}
	}

	public enum Type implements OpenGLCoded {
		ONE_DIMENSION(GL_TEXTURE_1D), TWO_DIMENSION(GL_TEXTURE_2D);
		private final int openGlCode;

		Type(int openGlCode) {
			this.openGlCode = openGlCode;
		}

		public int openGlCode() {
			return openGlCode;
		}

		public int[] openGlParams() {
			return new int[]{};
		}
	}

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

	Type type;

	private final int ID;
	public int Width;
	public int Height;
	private int Internal_Format;
	private int Image_Format;
	private int dataType;
	private Wrap wrap;
	private final Filter filter;

	public Stream<OpenGLCoded> openGlCodedStream() {
		return Stream.of(wrap, filter);
	}

	public Texture()
	{
		this.Width = 0;
		this.Height = 0;
		this.Internal_Format = GL_RGB;
		this.Image_Format = GL_RGB;
		this.wrap = Wrap.REPEAT;
		this.filter = Filter.LINEAR;
		this.type = Type.TWO_DIMENSION;
		this.dataType = GL_UNSIGNED_BYTE;
		ID = glGenTextures();
		LOG.info("Created texture ID: {}", ID);
	}

	// generate an empty texture (e.g. for rendering)
	public void Generate( int width,  int height, long pixels) {
		this.Width = width;
		this.Height = height;
		if (type != Type.TWO_DIMENSION) throw new IllegalArgumentException("Texture type must be 2D");
		// create texture
		glBindTexture(type.openGlCode, this.ID);
		glTexImage2D(type.openGlCode, 0, this.Internal_Format, width, height, 0, this.Image_Format, this.dataType, pixels);
		// set Texture wrap and filter modes
		wrap.openGlParamsStream().forEach(param -> glTexParameteri(type.openGlCode, param, wrap.openGlCode));
		filter.openGlParamsStream().forEach(param -> glTexParameteri(type.openGlCode, param, filter.openGlCode));

		// unbind texture
		glBindTexture(type.openGlCode, 0);
	}

	public void Generate( int width, int height, ByteBuffer data)
	{
		this.Width = width;
		this.Height = height;
		if (type != Type.TWO_DIMENSION) throw new IllegalArgumentException("Texture type must be 2D");
		// create Texture
		Bind();
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glTexImage2D(type.openGlCode, 0, this.Internal_Format, width, height, 0, this.Image_Format, this.dataType, data);
		// set Texture wrap and filter modes
		wrap.openGlParamsStream().forEach(param -> glTexParameteri(type.openGlCode, param, wrap.openGlCode));
		filter.openGlParamsStream().forEach(param -> glTexParameteri(type.openGlCode, param, filter.openGlCode));
		// unbind texture
		UnBind();
	}

	public void Generate1D( int length, ByteBuffer data)
	{
		this.Width = length;
		this.Height = 1;
		this.type = Type.ONE_DIMENSION;
		this.wrap = Wrap.REPEAT;
		// create Texture
		Bind();
		glTexImage1D(this.type.openGlCode, 0, this.Internal_Format, Width, 0, this.Image_Format, GL_UNSIGNED_BYTE, data);
		// set Texture wrap and filter modes
		wrap.openGlParamsStream().forEach(param -> glTexParameteri(type.openGlCode, param, wrap.openGlCode));
		filter.openGlParamsStream().forEach(param -> glTexParameteri(type.openGlCode, param, filter.openGlCode));
		// unbind texture
		UnBind();
	}

	public int id() {
		return ID;
	}

	public void Bind()
	{
		glBindTexture(type.openGlCode, ID);
	}

	public void UnBind()
	{
		glBindTexture(type.openGlCode, 0);
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

	public int getWidth() {
		return Width;
	}
}
