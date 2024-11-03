#version 460

precision mediump float;

uniform sampler2D tex;
uniform sampler1D palette;

in vec2 texCoords;
out vec4 fragColor;

void main() {
    // lookup palette index for texel (the red channel value)
    float texel = texture(tex, texCoords).r;
    // lookup palette colour for offset
    // clamp in range 0 - 1
    fragColor = clamp(texture(palette, texel), 0., 1.);
}

