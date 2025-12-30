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

// Kernel size 9
//uniform float offsets_9[2] = float[](0.0, 1.3333333333333335);
//uniform float weights_9[2] = float[](0.29411764705882354, 0.3529411764705882);
// Kernel size 13
//uniform float offsets_13[3] = float[](0.0, 1.3846153846153848, 3.230769230769231);
//uniform float weights_13[3] = float[](0.22702702702702704, 0.3162162162162162, 0.07027027027027027);
// Kernel size 17
//uniform float offsets[4] = float[](0.0, 1.411764705882353, 3.294117647058824, 5.1764705882352935);
//uniform float weights[4] = float[](0.1964825501511404, 0.2969069646728344, 0.09447039785044732, 0.010381362401148057);
// Kernel size 21
//uniform float offsets_21[5] = float[](0.0, 1.4285714285714288, 3.333333333333333, 5.238095238095238, 7.142857142857143);
//uniform float weights_21[5] = float[](0.176204109737977, 0.2803247200376907, 0.11089769144348204, 0.019407096002609356, 0.0012684376472293698);
// Kernel size 25
//uniform float offsets_25[7] = float[](0.00000000000000, 1.44000000000000, 3.36000000000000, 5.28000000000000, 7.20000000000000, 9.12000000000000, 11.04000000000000);
//uniform float weights_25[7] = float[](0.16118025779724, 0.26568174362183, 0.12177079916000, 0.02865195274353, 0.00316679477692, 0.00013709068298, 0.00000149011612);
// Kernel size 29
uniform float offsets[8] = float[](0.00000000000000, 1.44827586206897, 3.37931034482759, 5.31034482758621, 7.24137931034483, 9.17241379310345, 11.10344827586207, 13.03448275862069);
uniform float weights[8] = float[](0.14944598078728, 0.25281278416514, 0.12888494879007, 0.03730880096555, 0.00581435859203, 0.00044239684939, 0.00001361221075, 0.00000010803342);

uniform vec2 dimensions; // TODO transpose offsets from pixel distances into normalised distances


void main() {
    // center colour of our matrix
    fragColor = texture2D(tex, texCoords) * (blur ? weights[0] : 1.0);
    if (blur) {
        // now iterate over surrounding texture fetches and using their weights combine to blur
        float dimension = x ? dimensions.x : dimensions.y;
        for (int i = 1; i < offsets.length; i++) {
            float delta =  offsets[i] / dimension;
            if (x) {
                // plus
                fragColor += texture2D(tex, (texCoords + vec2(delta, 0.0))) *
                weights[i];
                // minus
                fragColor += texture2D(tex, (texCoords - vec2(delta, 0.0))) *
                weights[i];
            }
            else {
                // plus
                fragColor += texture2D(tex, (texCoords + vec2(0.0, delta))) *
                weights[i];
                // minus
                fragColor += texture2D(tex, (texCoords - vec2(0.0, delta))) *
                weights[i];
            }
        }
    }
    fragColor *= multiplier;
}

