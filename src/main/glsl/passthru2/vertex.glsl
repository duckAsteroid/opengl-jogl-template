#version 460

in vec2 screenPosition;
in vec2 texturePosition;

out vec2 texCoords;

void main() {
    // pass the vertex screen position out to GL
    gl_Position = vec4(screenPosition, 1.0, 1.0);
    // pass texture coords to fragment shader
    texCoords = texturePosition;
}
