#version 330 core

in vec2 position;
out vec2 texCoord;

void main(){
    vec4 positionVec4 = vec4(position, 0.0, 1.0);
    gl_Position = positionVec4;
    texCoord = position;
}
