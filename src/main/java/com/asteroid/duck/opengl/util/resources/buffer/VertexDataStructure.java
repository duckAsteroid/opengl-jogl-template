package com.asteroid.duck.opengl.util.resources.buffer;

import java.util.*;
import java.util.stream.Stream;

/**
 * Defines the structure of {@link VertexElement}s that make up the data for each vertex in a
 * {@link VertexDataBuffer}.
 */
public class VertexDataStructure implements Iterable<VertexElement> {
	private final List<VertexElement> structure;
	private final Map<String, VertexElement> namedElementCache;
	private final int size;
	/**
	 * {@link #asMap(List)} accepts fewer values than there are elements
	 */
	private final boolean acceptFewerValues;
	/**
   * {@link #asMap(List)} accepts more values than there are elements
   */
	private final boolean acceptMoreValues;

	public VertexDataStructure(VertexElement ... elements) {
		this(Arrays.asList(elements));
	}

	public VertexDataStructure(List<VertexElement> elements) {
		this(elements, true, false);
	}
	/**
	 * Create a structure using the elements in the specific order given
	 * @param elements the elements of the structure
	 * @throws IllegalArgumentException if duplicate named elements are included
	 * @throws IllegalArgumentException if the elements are empty
	 * @throws NullPointerException if elements are null
	 */
	public VertexDataStructure(List<VertexElement> elements, boolean acceptFewerValues, boolean acceptMoreValues) {
		this.acceptFewerValues = acceptFewerValues;
		this.acceptMoreValues = acceptMoreValues;
		Objects.requireNonNull(elements, "elements");
		if (elements.isEmpty()) {
			throw new IllegalArgumentException("Vertex elements list must not be empty");
		}
		namedElementCache = new HashMap<>(elements.size());
		for(VertexElement element : elements) {
			if (namedElementCache.containsKey(element.name())) {
				throw new IllegalArgumentException("Duplicate vertex element name: " + element.name());
			}
			namedElementCache.put(element.name(), element);
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
	 */
	public Map<VertexElement, Object> asMap(List<?> values) {
		Objects.requireNonNull(values, "values");
		if (!acceptMoreValues && values.size() > structure.size()) {
			throw new IllegalArgumentException("Too many values ("+values.size()+") for structure size="+structure.size());
		}
		if (!acceptFewerValues && values.size() < structure.size()) {
			throw new IllegalArgumentException("Too few values ("+values.size()+") for structure size="+structure.size());
		}
		Map<VertexElement, Object> result = new HashMap<>();
		for (int i = 0; i < structure.size(); i++) {
			VertexElement element = structure.get(i);
			Object value = element.type().nullReplacementValue();
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

	public VertexElement get(String name) {
		return namedElementCache.get(name);
	}
}
