#version 460

precision mediump float;

uniform sampler2D tex;
uniform usampler2D map;
uniform vec2 dimensions;

in vec2 texCoords;
out float fragColor;

void main() {
    // find the source texel coordinates for the current texel from the map
    uvec2 mappedCoords = texture(map, texCoords).xy; // point x & y
    // convert the texel coordinates to normalised form
    vec2 normalizedCoords = vec2(mappedCoords) / dimensions;
    // get the color from the source texture at that location
    fragColor = texture(tex, normalizedCoords).r;
}
