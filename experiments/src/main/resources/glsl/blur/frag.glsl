#version 460
#define MAX_KERNEL_SIZE 64

precision mediump float;

uniform sampler2D tex;
// texture coordinates from GPU
in vec2 texCoords;
// target frag color
out vec4 fragColor;
// X or Y axis blurring
uniform bool x;
// is blur enabled
uniform bool blur;
// a multiplier for the final color
uniform float multiplier = 0.99;
// number of discrete kernel samples (including center)
uniform int kernelSize;
// linear-interpolation sample offsets and weights, supplied from Java
uniform float offsets[MAX_KERNEL_SIZE];
uniform float weights[MAX_KERNEL_SIZE];

uniform vec2 dimensions;


void main() {
    // center colour of our matrix
    fragColor = texture(tex, texCoords) * (blur ? weights[0] : 1.0);
    if (blur) {
        // now iterate over surrounding texture fetches and using their weights combine to blur
        float dimension = x ? dimensions.x : dimensions.y;
        for (int i = 1; i < kernelSize; i++) {
            float delta =  offsets[i] / dimension;
            if (x) {
                // plus
                fragColor += texture(tex, (texCoords + vec2(delta, 0.0))) *
                weights[i];
                // minus
                fragColor += texture(tex, (texCoords - vec2(delta, 0.0))) *
                weights[i];
            }
            else {
                // plus
                fragColor += texture(tex, (texCoords + vec2(0.0, delta))) *
                weights[i];
                // minus
                fragColor += texture(tex, (texCoords - vec2(0.0, delta))) *
                weights[i];
            }
        }
    }
    fragColor *= multiplier;
}
