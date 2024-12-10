package com.asteroid.duck.opengl.util.resources.buffer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public record VertexElement(VertexElementType<?> type, String name) {
	public int size() {
    return type.size();
  }

	public void checkInstanceOf(Object value) throws IllegalArgumentException {
		if (value != null) {
			Class<?> actualType = value.getClass();
			Class<?> expectedType = type.getJavaType();
			if (!expectedType.isAssignableFrom(actualType)) {
				throw new IllegalArgumentException(name + ": "+actualType.getName() +" is not of type " + expectedType.getName());
			}
		}
	}

	public static Map<String, VertexElement> structure(VertexElement ... elements) {
		return structure(Arrays.asList(elements));
	}

	public static Map<String, VertexElement> structure(Collection<VertexElement> elements) {
		Map<String, VertexElement> map = new HashMap<>(elements.size());
		for (VertexElement element : elements) {
			if (map.containsKey(element.name())) {
        throw new IllegalArgumentException("Duplicate vertex element name: " + element.name());
      }
			map.put(element.name(), element);
		}
		return map;
	}
}
