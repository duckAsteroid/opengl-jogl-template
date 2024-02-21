package com.asteriod.duck.opengl.util;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Keys {
	private static final Logger LOG = LoggerFactory.getLogger(Keys.class);
	public record Key(int code, String name, boolean modifier) {}

	private final HashMap<Integer, Key> keysByCode = new HashMap<>();

	public Keys() throws IllegalAccessException {
		List<Key> ordinaryKeys = keysFromGL("GLFW_KEY_")
						.map(f -> fromField(f, "GLFW_KEY_", false))
						.filter(Optional::isPresent).map(Optional::get).toList();
		ordinaryKeys.forEach(key -> keysByCode.put(key.code, key));

		List<Key> modifiers = keysFromGL("GLFW_MOD_")
						.map(f -> fromField(f, "GLFW_MOD_", true))
						.filter(Optional::isPresent).map(Optional::get).toList();
		modifiers.forEach(key -> keysByCode.put(key.code, key));
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
}
