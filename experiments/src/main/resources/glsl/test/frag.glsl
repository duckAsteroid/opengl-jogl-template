#version 460

precision mediump float;

in vec2 texCoords;
out float fragColor;

void main() {
    // grey gradient
    fragColor = texCoords.x * texCoords.y;
}
