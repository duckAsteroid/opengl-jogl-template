#version 460
in vec2 position;
in vec2 texturePosition;
in vec4 color;

out vec2 texCoords;
out vec4 spriteColor;

void main()
{
    texCoords = texturePosition;
    spriteColor = color;
    gl_Position = vec4(position.x, position.y, 0.0, 1.0);
}
