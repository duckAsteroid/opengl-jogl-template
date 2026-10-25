package com.asteroid.duck.opengl.util.resources.font.factory;

import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class FontTextureDataFactoryTest {

    private final FontTextureDataFactory factory =
            new FontTextureDataFactory(new Font(Font.SANS_SERIF, Font.PLAIN, 48), true);

    @Test
    void glyphImageIsCroppedToItsBounds() {
        GlyphImage g = factory.createCharacterImage('A');

        assertNotNull(g);
        assertEquals(new Rectangle(0, 0, g.image().getWidth(), g.image().getHeight()), g.bounds());
        // 'A' sits on the baseline, so the datum is at the bottom of its bounds
        assertTrue(g.datum().y >= g.bounds().height - 2, "datum " + g.datum() + " bounds " + g.bounds());
    }

    @Test
    void glyphsDontInheritPixelsFromThePreviousGlyph() {
        GlyphImage wide = factory.createCharacterImage('W');
        GlyphImage narrow = factory.createCharacterImage('.');

        assertTrue(narrow.bounds().width < wide.bounds().width);
        assertTrue(narrow.bounds().height < wide.bounds().height);
    }

    @Test
    void blankGlyphGetsMetricBounds() {
        GlyphImage space = factory.createCharacterImage(' ');

        assertNotNull(space);
        assertEquals(1, space.bounds().height);
        assertTrue(space.bounds().width > 0);
        assertEquals(new Point(0, 0), space.boundsRelativeDatum());
    }

    @Test
    void atlasContainsEveryGlyph() {
        FontTextureData data = factory.createFontTextureData();

        assertEquals(data.glyphImages().keySet(), data.glyphData().keySet());
        assertTrue(data.glyphData().containsKey('A'));
        // the strip is opaque wherever 'A' was copied
        Rectangle a = data.glyphData().get('A').bounds();
        boolean anyOpaque = false;
        for (int y = a.y; y < a.y + a.height; y++) {
            for (int x = a.x; x < a.x + a.width; x++) {
                anyOpaque |= (data.combined().getRGB(x, y) >>> 24) > 0;
            }
        }
        assertTrue(anyOpaque);
    }
}
