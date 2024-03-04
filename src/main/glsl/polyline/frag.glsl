#version 410 core
uniform vec4 lineColor;
uniform vec2 resolution;
out vec4 color;
void main() {
    vec2 norm = gl_FragCoord.xy / resolution;
    float f = 1 - abs((norm.x * 2.0) - 1.0f);
    color = f * lineColor;
}
