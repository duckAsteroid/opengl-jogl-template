package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.text.StringRenderer;

public class TextExperiment extends StringRenderer implements Experiment {
	public TextExperiment() {
		super(font, text);
	}

	@Override
	public String getDescription() {
		return "";
	}
}
