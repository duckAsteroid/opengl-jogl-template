#version 460

precision mediump float;

uniform usampler2D tex;
uniform sampler1D palette;
uniform int offset;

in vec2 texCoords;
out vec4 fragColor;

uint mod_u32( uint u32_bas , uint u32_div ){
    float   flt_res =  mod( float(u32_bas), float(u32_div));
    uint    u32_res = uint( flt_res );
    return( u32_res );
}

void main() {
    // lookup palette index
    uvec4 texel = texture(tex, texCoords);
    // lookup palette colour
    uint index = mod_u32(texel.r + offset, 256);
    fragColor = texture(palette, index / 256.0);
}

