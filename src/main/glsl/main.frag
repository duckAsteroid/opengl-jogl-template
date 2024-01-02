#version 330

precision mediump float;

const float PI = 3.1415926535897932384626433832795;
const float FREQ = .1;

uniform float millis;
in vec2 texCoord;

float wave(in float freq, in float phase) {
  float seconds = millis / 1000.;
  return (sin((2. * PI * freq * seconds) + radians(phase)) + 1.) / 2;
}

float wave(in float freq) {
  return wave(freq, 0.0);
}

void main() {
  vec4 col = vec4(wave(FREQ), wave(FREQ * 2.), wave(FREQ * 3.), 1.0f);
  // set pixel (frag) color
  gl_FragColor = col * texCoord.y * texCoord.x;
}


