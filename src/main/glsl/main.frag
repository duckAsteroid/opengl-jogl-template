#version 330

precision mediump float;

const float PI = 3.1415926535897932384626433832795;
const float FREQ = .1;

uniform float seconds;
uniform sampler2D molly;
in vec2 texCoord;

float wave(in float freq, in float phase) {
  return (sin((2. * PI * freq * seconds) + radians(phase)) + 1.) / 2;
}

float wave(in float freq) {
  return wave(freq, 0.0);
}

void main() {
  vec4 col = vec4(wave(FREQ), wave(FREQ * 2.), wave(FREQ * 3.), 1.0f);
  // set pixel (frag) color
  vec2 newPos = texCoord;
  newPos = newPos + (sin(newPos * 12.)/12.) * (sin(seconds/.8)/2. + 0.5);
  newPos.y = newPos.y * -1.0;
  vec4 molly = texture2D(molly, newPos);
  gl_FragColor = molly * col * (1.6 * texCoord.y) * (1.6 * texCoord.x);
}


