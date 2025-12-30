#version 460

precision mediump float;

// the texture coordinates passed in from the vertex shader
in vec2 texCoords;
// the output color of the pixel
out vec4 fragColor;
// the texture to sample from
uniform sampler2D tex;

void main() {
    // copy the texel straight into the pixel
    fragColor = texture(tex, texCoords);
}
