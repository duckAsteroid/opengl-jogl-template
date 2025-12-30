#version 330 core

uniform sampler2D tex;

in vec2 texCoords;

layout (location = 0) out float fragColor;

void main() {
    // copy the texel straight into the pixel
    fragColor = texture(tex, texCoords).r;
}
