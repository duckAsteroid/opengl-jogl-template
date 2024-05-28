#version 460

precision mediump float;

uniform usampler2D tex;
uniform sampler1D palette;
uniform vec2 dimensions;
uniform float paletteWidth;

in vec2 texCoords;
out vec4 fragColor;

void main() {
    // lookup palette index
    uvec4 texel = texture(tex, texCoords);
    // lookup palette colour
    fragColor = texture(palette, texel.r / paletteWidth);
}

