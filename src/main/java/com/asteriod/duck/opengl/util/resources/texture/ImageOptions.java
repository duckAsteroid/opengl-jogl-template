package com.asteriod.duck.opengl.util.resources.texture;

public record ImageOptions(boolean flipY, boolean singleLine, Type type) {
	public static final ImageOptions DEFAULT = new ImageOptions(true, false, Type.RGBA);

	public ImageOptions withNoFlip() {
		if (flipY) {
			return new ImageOptions(false, singleLine, type);
		}
		return this;
	}

	public ImageOptions withSingleLine() {
		if (!singleLine) {
			return new ImageOptions(flipY, true, type);
		}
		return this;
	}

	public ImageOptions withType(Type type) {
		if (this.type != type) {
			return new ImageOptions(flipY, singleLine, type);
		}
		return this;
	}
}
