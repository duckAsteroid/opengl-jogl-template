package com.asteroid.duck.opengl.util.resources.buffer.vbo;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.asteroid.duck.opengl.util.resources.buffer.vbo.VertexElementType.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL11.GL_INT;

class VertexDataStructureTest {

	@Test
	public void testNormalUsage() {
		VertexDataStructure subject = new VertexDataStructure(VertexBufferObjectTest.TEST_ELEMENTS);

		assertEquals(32, subject.size());
		assertEquals("aValue,pos,texture,color", subject.stream().map(VertexElement::name).collect(Collectors.joining(",")));

		Map<VertexElement, Object> object = subject.asMap(1f, new Vector2f(2f, 3f), new Vector2f(4f, 5f), new Vector3f(6f, 7f, 8f));
		assertNotNull(object);
		assertEquals(1f, object.get(VertexBufferObjectTest.TEST_ELEMENTS[0]));
		assertEquals(new Vector2f(2f, 3f), object.get(VertexBufferObjectTest.TEST_ELEMENTS[1]));
		assertEquals(new Vector2f(4f, 5f), object.get(VertexBufferObjectTest.TEST_ELEMENTS[2]));
		assertEquals(new Vector3f(6f, 7f, 8f), object.get(VertexBufferObjectTest.TEST_ELEMENTS[3]));
	}

	@Test
	public void shouldThrowIllegalArgumentExceptionWhenValuesListSizeIsGreaterThanStructureSize() {
		VertexElement element1 = new VertexElement(FLOAT, "element1");
		VertexElement element2 = new VertexElement(VEC_2F, "element2");
		VertexDataStructure subject = new VertexDataStructure(element1, element2);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			subject.asMap(1f, new Vector2f(2f, 3f), new Vector3f(4f, 5f, 6f));
		});
		assertEquals("Too many values (3) for structure size=2", exception.getMessage());
	}

	@Test
	public void shouldReturnMapWithNullReplacementValuesWhenValuesListIsSmallerThanStructureSize() {
		VertexElement element1 = new VertexElement(FLOAT, "element1");
		VertexElement element2 = new VertexElement(VEC_2F, "element2");
		VertexElement element3 = new VertexElement(VEC_3F, "element3");
		VertexDataStructure subject = new VertexDataStructure(element1, element2, element3);

		Map<VertexElement, Object> result = subject.asMap(1f);

		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals(1f, result.get(element1));
		assertEquals(element2.type().nullReplacementValue(), result.get(element2));
		assertEquals(element3.type().nullReplacementValue(), result.get(element3));
	}

	@Test
	public void shouldHandleEmptyValuesListAndReturnMapWithAllElementsMappedToNullReplacements() {
		VertexElement element1 = new VertexElement(FLOAT, "element1");
		VertexElement element2 = new VertexElement(VEC_2F, "element2");
		VertexElement element3 = new VertexElement(VEC_3F, "element3");
		VertexDataStructure subject = new VertexDataStructure(element1, element2, element3);

		Map<VertexElement, Object> result = subject.asMap(Collections.emptyList());

		assertNotNull(result);
		assertEquals(3, result.size());
		assertEquals(element1.type().nullReplacementValue(), result.get(element1));
		assertEquals(element2.type().nullReplacementValue(), result.get(element2));
		assertEquals(element3.type().nullReplacementValue(), result.get(element3));
	}

	@Test
	public void shouldThrowExceptionIfValueDoesNotMatchExpectedType() {
		VertexElement element1 = new VertexElement(FLOAT, "element1");
		VertexElement element2 = new VertexElement(VEC_2F, "element2");
		VertexDataStructure subject = new VertexDataStructure(element1, element2);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			subject.asMap(1f, new Vector3f(2f, 3f, 4f)); // Vector3f instead of Vector2f
		});
		assertEquals("element2: org.joml.Vector3f is not of type org.joml.Vector2f", exception.getMessage());
	}

	@Test
	public void shouldAcceptIfValueIsSubclassOfExpected() {
		VertexElementType<Number> NUMBER = new VertexElementType<>(Number.class, 1, GL_INT) {
			@Override
			public void serialize(Number obj, ByteBuffer buffer) {
				buffer.asDoubleBuffer().put(obj.doubleValue());
			}

			@Override
			public Double deserialize(ByteBuffer buffer) {
				return buffer.asDoubleBuffer().get();
			}

			@Override
			public Object nullReplacementValue() {
				return 0.0d;
			}
		};
		VertexElement element1 = new VertexElement(FLOAT, "element1");
		VertexElement element2 = new VertexElement(NUMBER, "element2");
		VertexDataStructure subject = new VertexDataStructure(element1, element2);

		Map<VertexElement, Object> map = subject.asMap(1f, Integer.valueOf(1234));// Vector3f instead of Vector2f
		assertNotNull(map);
		assertEquals(1f, map.get(element1));
		assertEquals(1234, map.get(element2));
	}

	@Test
	public void shouldThrowNullPointerExceptionWhenInputListIsNull() {
		NullPointerException exception = assertThrows(NullPointerException.class, () -> {
			new VertexDataStructure((List<VertexElement>) null);
		});
		assertEquals("elements", exception.getMessage());
	}

	@Test
	public void shouldThrowIllegalArgumentExceptionWhenInputListContainsDuplicateElementNames() {
		VertexElement element1 = new VertexElement(FLOAT, "duplicate");
		VertexElement element2 = new VertexElement(FLOAT, "duplicate");
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new VertexDataStructure(Arrays.asList(element1, element2));
		});
		assertEquals("Duplicate vertex element name: duplicate", exception.getMessage());
	}

	@Test
	public void shouldThrowIllegalArgumentExceptionWhenInputListIsEmpty() {
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			new VertexDataStructure(Collections.emptyList());
		});
		assertEquals("Vertex elements list must not be empty", exception.getMessage());
	}

	@Test
	public void shouldCorrectlyCalculateSizeWithOneElement() {
		VertexElement element = new VertexElement(VEC_2F, "singleElement");
		VertexDataStructure subject = new VertexDataStructure(Collections.singletonList(element));

		assertEquals(8, subject.size());
	}

	@Test
	public void shouldCorrectlyCalculateSizeWithMultipleElements() {
		VertexElement element1 = new VertexElement(FLOAT, "element1"); //4
		VertexElement element2 = new VertexElement(VEC_2F, "element2"); //8
		VertexElement element3 = new VertexElement(VEC_3F, "element3"); //12
		VertexDataStructure subject = new VertexDataStructure(element1, element2, element3);

		assertEquals(24, subject.size());
	}
}
