package com.asteroid.duck.opengl.util.keys;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public record KeyCombination(Set<Key> keys, Set<Key> modifiers) implements Comparable<KeyCombination> {
	public static KeyCombination simple(char key) {
		Key keyed = Keys.instance().keyFor(key);
		return new KeyCombination(Set.of(keyed), Collections.emptySet());
	}

	public static KeyCombination simpleWithMods(char key, String ... mods) {
		Key keyed = Keys.instance().keyFor(key);
		Set<Key> modSet = Arrays.stream(mods).map(m -> Keys.instance().keyForName(m)).collect(Collectors.toSet());
		return new KeyCombination(Set.of(keyed), modSet);
	}

	public String asSimpleString() {
		String mods = "";
		if (!modifiers.isEmpty()) {
			mods = modifiers.stream().map(Key::name).collect(Collectors.joining("|", "[", "]"));
		}
		String keystr = keys.stream().map(Key::name).collect(Collectors.joining("+"));
		return keystr + mods;
	}

	@Override
	public int compareTo(KeyCombination o) {
		Iterator<Key> thisIterator = this.keys.iterator();
		Iterator<Key> otherIterator = o.keys.iterator();

		while (thisIterator.hasNext() && otherIterator.hasNext()) {
			Key thisKey = thisIterator.next();
			Key otherKey = otherIterator.next();
			int comparison = thisKey.compareTo(otherKey);
			if (comparison != 0) {
				return comparison;
			}
		}

		// If all elements are equal, compare the sizes
		return Integer.compare(this.keys.size(), o.keys.size());
	}
}
