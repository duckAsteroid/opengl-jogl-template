#version 460

precision mediump float;

uniform sampler2D tex;
uniform sampler2D palette;

in vec2 texCoords;
out vec4 fragColor;

void main() {
    // lookup palette index for texel (the red channel, 0-1 with R16 precision)
    float pos = texture(tex, texCoords).r;
    // decode linear position into 2D palette coords using actual texture dimensions
    ivec2 palSize = textureSize(palette, 0);
    float pixelIndex = pos * float(palSize.x * palSize.y);
    float col = (mod(pixelIndex, float(palSize.x)) + 0.5) / float(palSize.x);
    float row = (floor(pixelIndex / float(palSize.x)) + 0.5) / float(palSize.y);
    fragColor = clamp(texture(palette, vec2(col, row)), 0., 1.);
}
