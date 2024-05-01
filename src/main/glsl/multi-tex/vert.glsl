#version 330
in vec2 position;
out vec2 texCoords;

void main() {
  // each vertex has 2 coords X&Y - we need XYZ+Depth
  vec4 positionVec4 = vec4(position, 0.0, 1.0);
  // convert the position into positive coords > 0
  positionVec4.xy = positionVec4.xy * 2.0 - 1.0;
  // flip the Y because the image runs top to bottom
  // whereas GL coord space is bottom to top
  positionVec4.y = positionVec4.y * -1.0;

  gl_Position = positionVec4;

  texCoords = position;
}
