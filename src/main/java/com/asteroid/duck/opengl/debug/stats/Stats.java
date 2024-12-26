package com.asteroid.duck.opengl.debug.stats;

public interface Stats {
  default void add(long value) {}
  default void ping() {}
  default String getFrameRate() { return "disabled"; };
}
