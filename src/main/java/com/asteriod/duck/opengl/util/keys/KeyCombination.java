package com.asteriod.duck.opengl.util.keys;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public record KeyCombination(Set<Key> keys, Set<Key> modifiers) {
	public static KeyCombination simple(char key) {
		Key keyed = Keys.instance().keyFor(key);
		return new KeyCombination(Set.of(keyed), Collections.emptySet());
	}

	public static KeyCombination simpleWithMods(char key, String ... mods) {
		Key keyed = Keys.instance().keyFor(key);
		Set<Key> modSet = Arrays.stream(mods).map(m -> Keys.instance().keyForName(m)).collect(Collectors.toSet());
		return new KeyCombination(Set.of(keyed), modSet);
	}
}
