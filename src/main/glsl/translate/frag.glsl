#version 460

precision mediump float;

uniform sampler2D tex;
uniform usampler2D map;
uniform vec2 dimensions;

in vec2 texCoords;
out vec4 fragColor;

void main() {
    // copy the texel straight into the pixel
    uvec2 mappedCoords = texture(map, texCoords).xy;
    vec2 normalizedCoords = vec2(mappedCoords) / dimensions;
    fragColor = texture(tex, normalizedCoords);
}
