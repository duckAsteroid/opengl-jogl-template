This file is here to keep notes on what this branch is all about - so
I can pick it up more quickly when work/time/life permits!

# Last Thing:

Last update: 2026-01-07

I was working on having ShaderVariable create some kind of "action"
that could be run in the render loop. I was also going to have them
ensure the correct shader was bound before updating uniforms. 

For this I started implementing a "BoundResource" interface that shaders
would implement. This would allow the render loop to check if the shader
was already bound before binding it again. We also had ExclusivityGroups 
to allow only one resource of a given group to be bound at a time.
And to avoud binding the same resource again if it was already bound.

The idea being the uniform update action would "request a bound shader" and the
context would bind it if it was not already bound.

The "setLater" on uniform could create these actions and the shadervariables
would then become more like a shader update actions handler (get rid of all the typed stuff there and
use unifomrs)

# Render Debug onscreen

So long ago now (I lose track) I was working on the internals of the Cthugha app.
Can't recall what now...

It seemed like it would be useful to be able to display debug data on the screen
like the old version of the program did.

After a bunch of research, it seemed that using an image-based font would be the easiest
option for rendering; like:

https://learnopengl.com/In-Practice/Text-Rendering+
https://github.com/SilverTiger/lwjgl3-tutorial/blob/master/src/silvertiger/tutorial/lwjgl/text/Font.java

In essence, we use Java 2D to render each "letter" from a font to a square (glyph) on a Texture. But
we need to keep track of the glyph metrics. Notably, the position of the baseline, the advance, etc. 
in order to render consecutive characters.

This approach works great; but lays bare the nightmare that is keeping Java2D (top, left) coordinates
in synch with OpenGL (bottom, left) oriented coordinates.

So I created a bunch of "vertice" helpers for corners of rectangles and for creating triangles to
represent those for rendering in a shader.

https://eng.libretexts.org/Bookshelves/Computer_Science/Applied_Programming/Introduction_to_Computer_Graphics_(Eck)/03%3A_OpenGL_1.1-_Geometry/3.01%3A_Shapes_and_Colors_in_OpenGL_1.1


### What Next?

I am writing here what I was doing, and what would do next each time I leave the project for a while:

1. Make shader programs BoundResource so that they don't need to bind if they already are.
2. Add some kind of bind tracking in the render context so that differnt types of bound resource know which one is currently bound?
3. Add a wrapper like API to shader (renderWith?) so that you can have it bind + render... use #2 to see if it needs to bind?


Test does not work because it shares the same program (same source) - it does not
reset the texture unit uniform between calls so the two passthru texture objects end
up rendering the same (last) texture (image).

Health warning PassthruTextureRenderer does not "pool" shaders - each instance creates a new shader : or they get
hellish confused about who renders which a texture unit