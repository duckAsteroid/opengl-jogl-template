package com.asteroid.duck.opengl.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GLFWConfigurerTest {

    @Test
    void beforeInitFactoryRunsOnlyThatHook() {
        List<String> calls = new ArrayList<>();
        GLFWConfigurer c = GLFWConfigurer.beforeInit(() -> calls.add("init"));

        c.beforeInit();
        c.beforeCreateWindow();

        assertEquals(List.of("init"), calls);
    }

    @Test
    void beforeCreateWindowFactoryRunsOnlyThatHook() {
        List<String> calls = new ArrayList<>();
        GLFWConfigurer c = GLFWConfigurer.beforeCreateWindow(() -> calls.add("window"));

        c.beforeInit();
        c.beforeCreateWindow();

        assertEquals(List.of("window"), calls);
    }

    @Test
    void noneDoesNothing() {
        assertDoesNotThrow(() -> {
            GLFWConfigurer.NONE.beforeInit();
            GLFWConfigurer.NONE.beforeCreateWindow();
        });
    }
}
