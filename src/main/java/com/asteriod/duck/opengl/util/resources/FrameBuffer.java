package com.asteriod.duck.opengl.util.resources;

import com.asteriod.duck.opengl.util.resources.texture.Texture;

import static org.lwjgl.opengl.GL30.*;

public class FrameBuffer {
	private final int fbo;
	private final Texture target;

	public FrameBuffer(Texture target) {
		this.target = target;
		fbo = glGenFramebuffers();
		bind();
		target.Bind();
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target.id(), 0);
		glDrawBuffers(GL_COLOR_ATTACHMENT0);
		final int fboStatus = glCheckFramebufferStatus(GL_FRAMEBUFFER);
		if (fboStatus != GL_FRAMEBUFFER_COMPLETE) {
			int error = glGetError();
			if (fboStatus == GL_FRAMEBUFFER_UNSUPPORTED) throw new IllegalArgumentException("Unsupported framebuffer type "+error);
			throw new IllegalArgumentException("Error creating framebuffer "+error);
		}
		System.out.println("Frame buffer ready "+fbo);
		unbind();
	}

	public void bind() {
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);
		glViewport(0,0, target.Width, target.Height);
	}

	public void unbind() {
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
	}

	public void dispose() {
		glDeleteFramebuffers(fbo);
	}


}
