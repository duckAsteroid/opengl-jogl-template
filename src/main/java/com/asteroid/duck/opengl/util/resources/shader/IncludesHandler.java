package com.asteroid.duck.opengl.util.resources.shader;

import java.nio.file.Path;

public interface IncludesHandler {
	Path find(String inclusion);
}
