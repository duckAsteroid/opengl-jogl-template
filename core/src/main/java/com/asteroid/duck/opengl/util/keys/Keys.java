package com.asteroid.duck.opengl.util.keys;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Keys {
	private static final Logger LOG = LoggerFactory.getLogger(Keys.class);

	private final HashMap<Integer, Key> keysByCode = new HashMap<>();
	private final HashMap<String, Key> keysByName = new HashMap<>();
	private final HashMap<Integer, Key> modsByCode = new HashMap<>();
	private final HashMap<String, Key> modsByName = new HashMap<>();

	public static Keys instance() {
		return SINGLETON;
	}

	private static Keys SINGLETON;

	static {
		try {
			SINGLETON = new Keys();
		} catch (IllegalAccessException e) {
			LOG.error("Error loading keys", e);
		}
	}

	private Keys() throws IllegalAccessException {
		final Set<String> ignored = Set.of("GLFW_KEY_LAST");
		List<Key> ordinaryKeys = keysFromGL("GLFW_KEY_")
						.filter(f -> !ignored.contains(f.getName()))
						.map(f -> fromField(f, "GLFW_KEY_", false))
						.filter(Optional::isPresent).map(Optional::get).toList();
		for (Key key : ordinaryKeys) {
			if (!keysByCode.containsKey(key.code())) {
				keysByCode.put(key.code(), key);
				keysByName.put(key.name(), key);
			} else {
				LOG.warn("Key "+key+" already mapped to "+keysByCode.get(key.code()));
			}
		}

		List<Key> modifiers = keysFromGL("GLFW_MOD_")
						.map(f -> fromField(f, "GLFW_MOD_", true))
						.filter(Optional::isPresent).map(Optional::get).toList();
		for (Key key : modifiers) {
			if (!modsByCode.containsKey(key.code())) {
				modsByCode.put(key.code(), key);
				modsByName.put(key.name(), key);
			} else {
				LOG.warn("Modifier Key "+key+" already mapped to "+modsByCode.get(key.code()));
			}
		}
	}
	private static Optional<Key> fromField(Field f, String startsWith, boolean isModifier) {
		try {
			String keyName = f.getName().substring(startsWith.length());
			Integer keyValue = (Integer) f.get(null);
			return Optional.of(new Key(keyValue, keyName, isModifier));
		} catch (IllegalAccessException e) {
			LOG.warn("Unable to load key from field:"+f.getName(), e);
			return Optional.empty();
		}
	}
	private static Stream<Field> keysFromGL(String startsWith) {
		return Arrays.stream(GLFW.class.getFields()).filter(f -> f.getName().startsWith(startsWith));
	}

	public Optional<Key> keyFor(int key) {
		return Optional.ofNullable(keysByCode.get(key));
	}

	public Set<Key> modsFor(int mods) {
		return modsByCode.values().stream()
						.filter(k -> (k.code() & mods) == k.code())
						.collect(Collectors.toSet());
	}

	public Key keyFor(char key) {
		if (Character.isAlphabetic(key) && Character.isUpperCase(key)) {
			return keysByCode.get((int)key);
		}
		else throw new IllegalArgumentException("Invalid simple key");
	}

	public Key keyForName(String name) {
		if(!keysByName.containsKey(name)) {
			return modsByName.get(name);
		}
		return keysByName.get(name);
	}
}
