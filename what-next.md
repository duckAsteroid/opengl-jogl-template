This file is here to keep notes on what this branch is all about - so
I can pick it up more quickly when work/time/life permits!

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

## Update: 2025-06-14

Now we can render a single letter on screen - look at using the Orthographic projection matrix to 
render to screen coordinates. 

We should also be able to blend a color with the texture to draw the text in a different color. 
We want to respect the alpha channel of the texture, but swap the color channels proportionally.

Then move on to rendering a string of text along a baseline.

# Shader Enhancements

Since every time you change shader programs you have to reinit uniforms - it would be nice to have a
class to manage this (so whenever it is used it sorts out the uniforms). 
This could be combined with our Variable like clases to manage updating the uniforms.
Thinking of a few kinds of variables:
* Static - set once, never changes
* Push - cache the value between render calls
* Pull - grab the value during each render call
* Event - update the value when an event occurs

Given we have all the uniforms names etc - we can also validate the variables during initialization.