#version 410 core
uniform uint lineColor;
uniform vec2 resolution;
out uint color;
void main() {
    vec2 norm = gl_FragCoord.xy / resolution;
    float f = 1 - abs((norm.x * 2.0) - 1.0f);
    color = lineColor;
}
