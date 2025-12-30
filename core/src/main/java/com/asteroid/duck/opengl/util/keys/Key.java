package com.asteroid.duck.opengl.util.keys;

public record Key(int code, String name, boolean modifier) implements Comparable<Key> {
	@Override
	public int compareTo(Key o) {
		int result = Boolean.compare(this.modifier, o.modifier);
		if (result == 0) {
			result = code - o.code;
		}
		return result;
	}
}
