package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.*;
import com.asteroid.duck.opengl.util.audio.Polyline;
import com.asteroid.duck.opengl.util.blur.BlurTextureRenderer;
import com.asteroid.duck.opengl.util.keys.KeyCombination;
import com.asteroid.duck.opengl.util.keys.KeyRegistry;
import com.asteroid.duck.opengl.util.palette.PaletteRenderer;
import com.asteroid.duck.opengl.util.resources.shader.vars.ShaderVariable;
import com.asteroid.duck.opengl.util.resources.texture.*;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

public class SoundWave extends Polyline implements Experiment {

	@Override
	public String getDescription() {
		return "Renders an audio wave on screen";
	}

}
