package com.asteroid.duck.opengl.util.text;

import com.asteroid.duck.opengl.util.resources.font.Glyph;

import java.awt.*;

/**
 * A character in a rendered string
 * @param position the X,Y location of the top right of the character
 * @param character the character itself
 * @param glyph the image glyph data for that character
 */
public record Character(char character, Glyph glyph, Point position) {
}
