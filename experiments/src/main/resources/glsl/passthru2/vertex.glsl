#version 460

in vec2 screenPosition;
in vec2 texturePosition;

out vec2 texCoords;

uniform mat4 projection;

void main() {
    // pass the vertex screen position out to GL
    gl_Position = projection * vec4(screenPosition, 1.0, 1.0);
    // pass texture coords to fragment shader
    texCoords = texturePosition;
}
