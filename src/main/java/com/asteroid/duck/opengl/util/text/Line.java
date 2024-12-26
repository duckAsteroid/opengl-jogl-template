package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.resources.font.Glyph;

import java.awt.*;
import java.util.List;

public class Line {
	private final int charSpacing;
	private final List<Character> characters;
	private final Point position;
	private final Dimension size;

	public Line(Point position, List<Character> characters, int charSpacing) {
		this.position = position;
		this.charSpacing = charSpacing;
		this.characters = characters;
		int width = characters.stream()
						.map(Character::glyph)
						.mapToInt(Glyph::width)
						.map(i -> i + charSpacing)
						.sum();
		int height = characters.stream()
						.map(Character::glyph)
						.mapToInt(Glyph::height)
						.max()
						.orElseThrow();
		this.size = new Dimension(width, height);
	}
}
