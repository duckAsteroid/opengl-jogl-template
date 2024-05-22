A framework and experimental application framework for OpenGL Shaders.

Loosely based on https://github.com/tsoding/opengl-template but written
in Java using LWJGL https://www.lwjgl.org/ OpenGL API.


# TO DO

After reading we should be able to create a 2D `GL_R16UI` image - this uses a one channel (red), 16 bit
unsigned integers (0-65535) for the pixels. 

This can be read in a shader sampler using `usampler2D` type:

```glsl
#version 430

uniform usampler2D tex;

void main() {
    uint value = texture(tex, gl_FragCoord.xy / resolution).r;
    // ... use the value ...
}
```

It can be created using:

```c
glTexImage2D(GL_TEXTURE_2D, 0, GL_R16UI, width, height, 0, GL_RED_INTEGER, GL_UNSIGNED_SHORT, data);
```

`data` would be 16 bit unsigned ints in platform native (Litte endian) order. Addressing as: `data[y * width + x]`

To write to such a texture in a framebuffer, individual pixels values in a fragment shader should be:
```glsl
#version 430
layout(location = 0) out uint outputColor;

void main() {
    // ... perform some calculations ...
    outputColor = calculatedValue; // calculatedValue is of type uint
}
```

Just to get started we could use GL_R8UI like the old Cthugha did?

## Translate Map

If we store the translation map into a texture of type `GL_RG16UI` we will have
two components (RG) with a 16 bit value. This is enough to map pixels in a 65535 x 65535 
area.

We can read the two components of the translation map texture in a shader as follows:

```glsl
uniform usampler2D tex;

void main() {
    uvec4 color = utexture(tex, vec2(0.5, 0.5));
    // color.r and color.g now contain your texture data
    // color.b and color.a will be zero
}

```
Alternatively we could use 
