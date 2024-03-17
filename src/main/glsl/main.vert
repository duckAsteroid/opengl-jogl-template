#version 330
in vec2 position;
out vec2 texCoords;

void main() {
  vec4 positionVec4 = vec4(position, 0.0, 1.0);
  positionVec4.xy = positionVec4.xy * 2.0 - 1.0;
  positionVec4.y = positionVec4.y * -1.0;

  gl_Position = positionVec4;

  texCoords = position;
}
