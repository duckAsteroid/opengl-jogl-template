package com.asteroid.duck.opengl.experiments;

import com.asteroid.duck.opengl.util.RenderedItem;

/**
 * Interface for a thing we can run as an experiment.
 * Used via {@link ExperimentChooser}
 */
public interface Experiment extends RenderedItem {
	default String getTitle() {
		return getClass().getSimpleName();
	}

	String getDescription();
}
