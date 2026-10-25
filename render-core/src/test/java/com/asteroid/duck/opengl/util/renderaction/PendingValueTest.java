package com.asteroid.duck.opengl.util.renderaction;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PendingValueTest {

    PendingValue<Matrix4f> subject = new PendingValue<>(new Matrix4f(), (src, dst) -> dst.set(src));
    Matrix4f dest = new Matrix4f();

    @Test
    void nothingToTakeInitially() {
        assertFalse(subject.takeIfChanged(dest));
    }

    @Test
    void offeringTheInitialValueIsANoOp() {
        subject.offer(new Matrix4f());
        assertFalse(subject.takeIfChanged(dest));
    }

    @Test
    void changedValueIsTakenOnceAndCopied() {
        Matrix4f source = new Matrix4f().rotateZ(0.5f);
        subject.offer(source);
        source.identity(); // caller reuses its instance; the offered value must be unaffected

        assertTrue(subject.takeIfChanged(dest));
        assertEquals(new Matrix4f().rotateZ(0.5f), dest);
        assertFalse(subject.takeIfChanged(dest), "already taken");
    }

    @Test
    void reofferingTheSameValueIsANoOp() {
        subject.offer(new Matrix4f().scale(2f));
        subject.takeIfChanged(dest);

        subject.offer(new Matrix4f().scale(2f));
        assertFalse(subject.takeIfChanged(dest));
    }

    @Test
    void lastWriteWins() {
        subject.offer(new Matrix4f().scale(2f));
        subject.offer(new Matrix4f().scale(3f));

        assertTrue(subject.takeIfChanged(dest));
        assertEquals(new Matrix4f().scale(3f), dest);
    }
}
