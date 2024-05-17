#version 460

precision mediump float;

uniform sampler2D tex;
// texture coordinates from GPU
in vec2 texCoords;
// target frag color
out vec4 fragColor;

uniform float offset[3] = float[](0.0, 1.3846153846, 3.2307692308);
uniform float weight[3] = float[](0.2270270270, 0.3162162162, 0.0702702703);

void main() {
    // copy the texel straight into the pixel
    fragColor = texture2D(tex, texCoords) * weight[0];

    for (int i=1; i<3; i++) {
        // plus Y
        fragColor +=
            texture2D(tex, (vec2(texCoords) + vec2(0.0, offset[i])))
            * weight[i] ;
        // minus Y
        fragColor +=
            texture2D(tex, (vec2(texCoords) - vec2(0.0, offset[i])))
            * weight[i];
    }

    //fragColor *= 0.8;
}
