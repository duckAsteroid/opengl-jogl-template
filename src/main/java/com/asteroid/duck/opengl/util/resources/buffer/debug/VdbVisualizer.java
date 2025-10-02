package com.asteroid.duck.opengl.util.resources.buffer.debug;

import com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer;
import com.asteroid.duck.opengl.util.resources.buffer.VertexDataStructure;
import com.asteroid.duck.opengl.util.resources.buffer.VertexElement;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A class that can visualise the contents of a Vertex Data Buffer (VDB).
 * @see com.asteroid.duck.opengl.util.resources.buffer.VertexDataBuffer
 */
public class VdbVisualizer {
	private final VertexDataBuffer buffer;

	public VdbVisualizer(VertexDataBuffer buffer) {
		this.buffer = buffer;
	}

	public String byteString() {
		return buffer.byteStream()
						.map((b) -> Integer.toHexString(b & 0xFF).toUpperCase())
						.map((s) -> s.length() == 1 ? "0" + s : s)
						.collect(Collectors.joining(","));
	}

	public String verticeString() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < buffer.size(); i++) {
			Map<VertexElement, ?> data = buffer.get(i);
			for(VertexElement ve : buffer.getStructure()) {
				String text = "^" + i ;
				text = truncateAndPad(text, ve.type().byteSize() * 3);
				text = text.substring(0, text.length() - 2) + "^";
				result.append(text).append(' ');
			}
		}
		return result.toString();
	}

	public String dataString() {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < buffer.size(); i++) {
			Map<VertexElement, ?> data = buffer.get(i);
			for(VertexElement ve : buffer.getStructure()) {
				Object value = data.get(ve);
				String text = "^" + dataString(value);
				text = truncateAndPad(text, ve.type().byteSize() * 3);
				text = text.substring(0, text.length() - 2) + "^";
				result.append(text).append(' ');
			}
		}
		return result.toString();
	}

	public static String floatToString(float f) {
		return String.format("%.4f", f);
	}

	public static String dataString(Object dataValue) {
		return switch (dataValue) {
			case Float f -> floatToString(f);
			case Vector2f v -> "[" + floatToString(v.x) + "," + floatToString(v.y) + "]";
			case Vector3f v -> "[" + floatToString(v.x) + "," + floatToString(v.y) + "," + floatToString(v.z) + "]";
			case Vector4f v -> "[" + floatToString(v.x) + "," + floatToString(v.y) + "," + floatToString(v.z) + "," + floatToString(v.w) + "]";
			default -> String.valueOf(dataValue);
		};
	}

	public String headerString() {
		return IntStream.range(0, buffer.size())
						.mapToObj((i) -> headerString(buffer.getStructure()))
						.collect(Collectors.joining(" "));
	}

    public static String headerString(VertexElement e) {
        String text = "^" + e.name();
        int maxLength = e.type().byteSize() * 3;
        String header;
        if (text.length() > maxLength) {
            header = text.substring(0, maxLength);
        } else {
            header = String.format("%-" + maxLength + "s", text).replace(' ', '-');
        }
        return header.substring(0, header.length() - 2) + "^";
    }

    public static String headerString(VertexDataStructure vds) {
        return vds.stream().map(VdbVisualizer::headerString).collect(Collectors.joining(" "));
    }

	private static String truncateAndPad(String text, int maxLength) {
		if (text.length() > maxLength) {
			return text.substring(0, maxLength);
		} else {
			return String.format("%-" + maxLength + "s", text).replace(' ', '-');
		}
	}
}
