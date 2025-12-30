package com.asteroid.duck.opengl.util.timer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TimerImplTest {
	private Double timestamp = 0.0;

	TimerImpl subject = new TimerImpl(this::getTime);

	private Double getTime() {
		return timestamp;
	}

	@Test
	public void testBasicOperation() {
		subject.reset();
		assertEquals(0.0, subject.elapsed());
		assertFalse(subject.isPaused());

		timestamp = 10.0;
		subject.update();
		assertEquals(10.0, subject.elapsed());

		timestamp = 20.0;
		subject.togglePaused();
		assertTrue(subject.isPaused());
		subject.update();
		assertEquals(10.0, subject.elapsed());

		timestamp = 30.0;
		subject.togglePaused();
		subject.update();
		assertEquals(20.0, subject.elapsed());

		subject.reset();
		assertEquals(0.0, subject.elapsed());

		timestamp = 40.0;
		subject.update();
		assertEquals(10.0, subject.elapsed());
	}
}
