#version 330
in vec2 texturePosition;
in vec2 screenPosition;
out vec2 texCoords;

void main() {
    // report out to open GL the
    gl_Position = vec4(screenPosition, 0.0, 1.0);;
    // pass coords to vertex shader
    texCoords = texturePosition;
}
