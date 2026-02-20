#version 330
// the screen position of the vertex
in vec2 screenPosition;
// the texture coordinates of the vertex
in vec2 texturePosition;


// output of texture coordinates to fragment shader
out vec2 texCoords;

void main() {
    // each vertex has 2 coords X&Y - we need XYZ+Depth for gl_Position
    gl_Position = vec4(screenPosition, 0.0, 1.0);
    // pass texture coordinates to vertex shader
    texCoords = texturePosition;
}
