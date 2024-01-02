#version 330

precision mediump float;

uniform float millis;

in vec2 texCoord;

void main() {
  float sin = (sin(millis / 200) + 1.0 ) / 2.;
  vec4 col = vec4(sin, sin, sin, 1.0f);
  // set pixel (frag) color
  gl_FragColor = col;
}
