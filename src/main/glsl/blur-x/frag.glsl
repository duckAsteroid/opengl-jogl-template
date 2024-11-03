#version 460
#define MAX_KERNEL_SIZE 64

precision mediump float;

uniform sampler2D tex;
// texture coordinates from GPU
in vec2 texCoords;
// target frag color
out vec4 fragColor;

uniform vec2 dimensions; // TODO transpose offsets from pixel distances into normalised distances

layout(std140) uniform OffsetWeightData {
    float offsets[MAX_KERNEL_SIZE];
    float weights[MAX_KERNEL_SIZE];
    int size;
};


void main() {
    fragColor = texture2D(tex, texCoords) * weights[0];

    float oy = 0.0;
    for (int i = 1; i < size; i++) {
        float ox = offsets[i] / dimensions.x;
        // plus X
        fragColor += texture2D(tex, (texCoords + vec2(ox, oy))) *
            weights[i];
        // minus X
        fragColor += texture2D(tex, (texCoords - vec2(ox, oy))) *
            weights[i];
    }
}

