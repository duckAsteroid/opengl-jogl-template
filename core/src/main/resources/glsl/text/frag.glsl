#version 460

precision mediump float;

uniform sampler2D tex;
uniform vec4 textColor;

in vec2 texCoords;
out vec4 fragColor;

void main() {
    vec4 color = texture(tex, texCoords);
    float mask = color.r;
    fragColor = vec4(textColor.rgb * mask, textColor.a * color.a);
}
