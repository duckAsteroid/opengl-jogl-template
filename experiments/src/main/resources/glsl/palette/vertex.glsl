#version 330

in vec2 screenPosition;
in vec2 texturePosition;
out vec2 texCoords;

void main() {
    // report out to open GL the screen position
    gl_Position = vec4(screenPosition, 0.0, 1.0);
    // pass tecture coords to fragment shader
    texCoords = texturePosition;
}
