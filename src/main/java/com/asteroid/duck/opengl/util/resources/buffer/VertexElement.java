package com.asteroid.duck.opengl.util.resources.buffer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a single "element" of data assigned to each vertex in a {@link VertexDataBuffer}.
 * @param type The type of data held in each element
 * @param name The name of the variable used in the vertex shader
 */
public record VertexElement(VertexElementType<?> type, String name) {

	public String headerString() {
		String text = "^" + name;
		int maxLength = type.byteSize() * 3;
		String header;
		if (text.length() > maxLength) {
			 header = text.substring(0, maxLength);
		} else {
			header = String.format("%-" + maxLength + "s", text).replace(' ', '-');
		}
		return header.substring(0, header.length() - 2) + "^";
	}

	/**
	 * Checks if the given value is an instance of the type defined by this element.
	 * If the value is null, it will not throw an exception.
	 * @param value The value to check
	 * @throws IllegalArgumentException if the value is not of the expected type
	 */
	public void checkInstanceOf(Object value) throws IllegalArgumentException {
		if (value != null) {
			Class<?> actualType = value.getClass();
			Class<?> expectedType = type.getJavaType();
			if (!expectedType.isAssignableFrom(actualType)) {
				throw new IllegalArgumentException(name + ": "+actualType.getName() +" is not of type " + expectedType.getName());
			}
		}
	}

}
