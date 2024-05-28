package com.asteriod.duck.opengl.util.resources.texture;

import java.io.IOException;
import java.nio.file.Path;

public interface TextureDataLoader {
	ImageData load(Path path, ImageOptions options) throws IOException;
}
