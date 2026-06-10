#version 460

in vec2 screenPosition;
in vec2 texturePosition;

out vec2 texCoords;

uniform mat4 projection;
uniform mat4 model;

void main() {
    gl_Position = projection * model * vec4(screenPosition, 1.0, 1.0);
    texCoords = texturePosition;
}
