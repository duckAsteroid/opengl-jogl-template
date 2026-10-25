package com.asteroid.duck.opengl.util.wave;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AmplitudeFunctionTest {

    @Test
    void factoriesWithEqualParametersAreEqual() {
        // Renderers skip their VBO rebuild when setAmplitudeFunction receives an equal function.
        assertEquals(AmplitudeFunction.constant(1.5f), AmplitudeFunction.constant(1.5f));
        assertEquals(AmplitudeFunction.ellipse(0.8f), AmplitudeFunction.ellipse(0.8f));
        assertNotEquals(AmplitudeFunction.ellipse(0.8f), AmplitudeFunction.ellipse(0.9f));
        assertNotEquals(AmplitudeFunction.constant(0.8f), AmplitudeFunction.ellipse(0.8f));
    }

    @Test
    void valuesAreUnchanged() {
        assertEquals(2f, AmplitudeFunction.constant(2f).amplitudeAt(7, 0.3f));
        AmplitudeFunction ellipse = AmplitudeFunction.ellipse(2f);
        assertEquals(0f, ellipse.amplitudeAt(0, -1f), 1e-6f);
        assertEquals(2f, ellipse.amplitudeAt(0, 0f), 1e-6f);
        assertEquals(0f, ellipse.amplitudeAt(0, 1f), 1e-6f);
    }
}
