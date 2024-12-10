package com.asteroid.duck.opengl.util.resources.buffer;

import java.util.*;
import java.util.stream.Stream;

/**
 * Defines the structure of {@link VertexElement}s that make up the data for each vertex in a
 * {@link VertexDataBuffer}.
 */
public class VertexDataStructure implements Iterable<VertexElement> {
	private final List<VertexElement> structure;
	private final int size;

	public VertexDataStructure(VertexElement ... elements) {
		this(Arrays.asList(elements));
	}

	/**
	 * Create a structure using the elements in the specific order given
	 * @param elements the elements of the structure
	 * @throws IllegalArgumentException if duplicate named elements are included
	 * @throws IllegalArgumentException if the elements are empty
	 * @throws NullPointerException if elements are null
	 */
	public VertexDataStructure(List<VertexElement> elements) {
		Objects.requireNonNull(elements, "elements");
		if (elements.isEmpty()) {
			throw new IllegalArgumentException("Vertex elements list must not be empty");
		}

		Set<String> names = new HashSet<>();
		for(VertexElement element : elements) {
			if (names.contains(element.name())) {
				throw new IllegalArgumentException("Duplicate vertex element name: " + element.name());
			}
			names.add(element.name());
		}
		this.structure = elements;
		this.size = elements.stream().mapToInt(VertexElement::size).sum();
	}

	public Map<VertexElement, Object> asMap(Object ... values) {
		return asMap(Arrays.asList(values));
	}

	/**
   * Create a map using the elements in the specific order given
   * @param values the values to populate the map with
   * @return a map of the values for the elements
   * @throws IllegalArgumentException if the number of values is not equal to the structure size
	 */
	public Map<VertexElement, Object> asMap(List<?> values) {
		Objects.requireNonNull(values, "values");
		if (values.size() > structure.size()) {
			throw new IllegalArgumentException("Too many values ("+values.size()+") for structure size="+structure.size());
		}
		Map<VertexElement, Object> result = new HashMap<>();
		for (int i = 0; i < structure.size(); i++) {
			VertexElement element = structure.get(i);
			Object value = null;
			if (i < values.size()) {
				value = values.get(i);
			}
			element.checkInstanceOf(value);
			result.put(element, value);
		}
		return result;
	}

	@Override
	public Iterator<VertexElement> iterator() {
		return structure.iterator();
	}

	public Stream<VertexElement> stream() {
    return structure.stream();
  }

	/**
	 * The size (in bytes) of all elements in the structure
	 * @return size in bytes of the structure
	 */
	public int size() {
		return size;
	}
}
