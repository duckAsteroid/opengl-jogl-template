package com.asteriod.duck.opengl.util.resources.impl;

import java.nio.file.Path;

public abstract class AbstractResourceLoader<T extends Resource> {
	private final Path root;

	public AbstractResourceLoader(Path root) {
		this.root = root;
	}

	protected Path getPath(String path) {
		return root.resolve(path);
	}

}
