package com.asteroid.duck.opengl.util.keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

/**
 * Key registry handles
 */
public class KeyRegistry implements Iterable<KeyAction> {
	private static final Logger LOG = LoggerFactory.getLogger(KeyRegistry.class);

	private final Map<KeyCombination, KeyAction> keyActions = new HashMap<>();
	private final LinkedList<KeyAction> orderedActions = new LinkedList<>();

	public void registerKeyAction(int key, Runnable runnable, String description) {
		Key knownKey = Keys.instance().keyFor(key).orElseThrow(() -> new IllegalArgumentException("Unknown key code: " + key));
		registerKeyAction(new KeyCombination(Collections.singleton(knownKey), Collections.emptySet()), runnable, description);
	}
	public void registerKeyAction(int key, int mod, Runnable runnable, String description) {
		Key knownKey = Keys.instance().keyFor(key).orElseThrow(() -> new IllegalArgumentException("Unknown key code: " + key));
		Set<Key> knownMods = Keys.instance().modsFor(mod);
		registerKeyAction(new KeyCombination(Collections.singleton(knownKey), knownMods), runnable, description);
	}

	public void registerKeyAction(Set<KeyCombination> keyCombinations, Runnable runnable, String description) {
		for(KeyCombination combo : keyCombinations) {
			registerKeyAction(combo, runnable, description);
		}
	}

	public KeyAction registerKeyAction(KeyCombination combo, Runnable runnable, String description) {
		if (keyActions.containsKey(combo)) {
			throw new IllegalArgumentException("Action already registered for keys="+combo.asSimpleString());
		}
		if (description == null) {
			description = "Unknown";
		}
		KeyAction keyAction = new KeyAction(this, combo, runnable, description);
		keyActions.put(combo, keyAction);
		orderedActions.add(keyAction);
		return keyAction;
	}

	boolean unregister(KeyCombination combo) {
		return keyActions.remove(combo) != null;
	}

	public void handleCallback(KeyCombination combo) {
		if (keyActions.containsKey(combo)) {
			KeyAction action = keyActions.get(combo);
			action.run();
		}
	}

	@Override
	public Iterator<KeyAction> iterator() {
		return stream().iterator();
	}

	public Stream<KeyAction> stream() {
		return orderedActions.stream();
	}
}
