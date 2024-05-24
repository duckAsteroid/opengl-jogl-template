package com.asteriod.duck.opengl.util.resources.texture;

import com.asteriod.duck.opengl.util.resources.impl.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;


public class Texture implements Resource {
	private static final Logger LOG = LoggerFactory.getLogger(Texture.class);
	int type;

	private int ID;
	public int Width;
	public int Height;
	private int Internal_Format;
	private int Image_Format;

	private final int Wrap_S;
	private final int Wrap_T;
	private final int Filter_Min;
	private final int Filter_Max;

	public Texture()
	{
		this.Width = 0;
		this.Height = 0;
		this.Internal_Format = GL_RGB;
		this.Image_Format = GL_RGB;
		this.Wrap_S = GL_REPEAT;
		this.Wrap_T = GL_REPEAT;
		this.Filter_Min = GL_LINEAR;
		this.Filter_Max = GL_LINEAR;
		this.type = GL_TEXTURE_2D;
		ID = glGenTextures();
		LOG.info("Created texture ID: " + ID);
	}

	// generate an empty texture (e.g. for rendering)
	public void Generate( int width,  int height, long pixels) {
		this.Width = width;
		this.Height = height;
		// create texture
		glBindTexture(type, this.ID);
		glTexImage2D(type, 0, this.Internal_Format, width, height, 0, this.Image_Format, GL_UNSIGNED_BYTE, pixels);
		// set Texture wrap and filter modes
		glTexParameteri(type, GL_TEXTURE_WRAP_S, this.Wrap_S);
		glTexParameteri(type, GL_TEXTURE_WRAP_T, this.Wrap_T);
		glTexParameteri(type, GL_TEXTURE_MIN_FILTER, this.Filter_Min);
		glTexParameteri(type, GL_TEXTURE_MAG_FILTER, this.Filter_Max);
		// unbind texture
		glBindTexture(type, 0);
	}

	public void Generate( int width, int height, ByteBuffer data)
	{
		this.Width = width;
		this.Height = height;


		// create Texture
		glBindTexture(GL_TEXTURE_2D, this.ID);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
		glTexImage2D(GL_TEXTURE_2D, 0, this.Internal_Format, width, height, 0, this.Image_Format, GL_UNSIGNED_BYTE, data);
		// set Texture wrap and filter modes
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, this.Wrap_S);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, this.Wrap_T);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, this.Filter_Min);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, this.Filter_Max);
		// unbind texture
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public void Generate1D( int length, ByteBuffer data)
	{
		this.Width = length;
		this.Height = 1;
		this.type = GL_TEXTURE_1D;

		// create Texture
		glBindTexture(GL_TEXTURE_1D, this.ID);
		glTexImage1D(GL_TEXTURE_1D, 0, this.Internal_Format, Width, 0, this.Image_Format, GL_UNSIGNED_BYTE, data);
		// set Texture wrap and filter modes
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		// unbind texture
		glBindTexture(GL_TEXTURE_1D, 0);
	}

	public int id() {
		return ID;
	}

	public void Bind()
	{
		glBindTexture(type, ID);
	}

	public void UnBind()
	{
		glBindTexture(type, 0);
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

	public int getWidth() {
		return Width;
	}
}
