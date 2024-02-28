package com.asteriod.duck.opengl.util.resources.texture;

import com.asteriod.duck.opengl.util.resources.impl.Resource;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;

public class Texture implements Resource {

	private int ID;
	private int Width;
	private int Height;
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
		ID = glGenTextures();
	}

	public void Generate( int width,  int height, ByteBuffer data)
	{
		this.Width = width;
		this.Height = height;
		// create Texture
		glBindTexture(GL_TEXTURE_2D, this.ID);
		glTexImage2D(GL_TEXTURE_2D, 0, this.Internal_Format, width, height, 0, this.Image_Format, GL_UNSIGNED_BYTE, data);
		// set Texture wrap and filter modes
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, this.Wrap_S);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, this.Wrap_T);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, this.Filter_Min);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, this.Filter_Max);
		// unbind texture
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public int id() {
		return ID;
	}

	public void Bind()
	{
		glBindTexture(GL_TEXTURE_2D, ID);
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
}
