#version 460

precision mediump float;

uniform sampler2D tex0;
uniform sampler2D tex1;
uniform float amount;

in vec2 texCoords;
out vec4 fragColor;

void main() {
  vec4 c1 = texture(tex0, texCoords);
  vec4 c2 = texture(tex1, texCoords);
  fragColor = mix(c1, c2, amount);
}


