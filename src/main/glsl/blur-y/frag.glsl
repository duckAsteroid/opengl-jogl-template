#version 460

precision mediump float;

uniform sampler2D tex;
// texture coordinates from GPU
in vec2 texCoords;
// target frag color
out vec4 fragColor;

uniform vec2 dimensions; // TODO transpose offsets from pixel distances into normalised distances
uniform float offset[3] = float[](0.0, 1.3846153846, 3.2307692308);
uniform float weight[3] = float[](0.2270270270, 0.3162162162, 0.0702702703);

void main() {
    // copy the texel straight into the pixel
    fragColor = texture2D(tex, texCoords) * weight[0];

    for (int i=1; i<offset.length; i++) {
        float oy = offset[i] / dimensions.y;
        // plus Y
        fragColor +=
            texture2D(tex, (texCoords + vec2(0.0, oy)))
            * weight[i] ;
        // minus Y
        fragColor +=
            texture2D(tex, (texCoords - vec2(0.0, oy)))
            * weight[i];
    }

    //fragColor *= 0.8;
}
