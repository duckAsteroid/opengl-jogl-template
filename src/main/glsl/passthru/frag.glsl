#version 460

precision mediump float;

uniform sampler2D tex;

in vec2 texCoords;
out vec4 fragColor;

void main() {
    // copy the texel straight into the pixel
    fragColor = vec4(texture(tex, texCoords).rgb, 1.0);
}
