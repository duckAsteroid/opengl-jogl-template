#version 460
in vec2 texCoords;
in vec4 spriteColor;

uniform sampler2D image;

out vec4 FragColor;

void main()
{
    FragColor = spriteColor * texture(image, texCoords);
}
