package com.asteriod.duck.opengl.experiments;

import com.asteriod.duck.opengl.util.RenderedItem;

public interface Experiment extends RenderedItem {
	default String getTitle() {
		return getClass().getSimpleName();
	}

	String getDescription();
}
