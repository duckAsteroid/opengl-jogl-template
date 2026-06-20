# Key Input, Cross-Thread Actions, and Colours

## Key input

```java
// init():
ctx.getKeyRegistry().registerKeyAction(
    KeyCombination.simple('A'),
    () -> { /* runs on GL thread when A is pressed */ },
    "Human-readable description for help display");

// With modifiers:
ctx.getKeyRegistry().registerKeyAction(
    KeyCombination.simpleWithMods('A', "SHIFT"),
    () -> { /* Shift+A */ },
    "Description");
```

---

## Cross-thread render actions (RenderActionQueue)

Use this when a non-GL thread (e.g. key callback, audio thread) needs to mutate GL state.
Singleton types keep only the most recent enqueued action, discarding stale intermediate values.

```java
// Fields:
private static final String ACTION_COLOUR = "colour";  // singleton — only latest matters
private static final String ACTION_FLASH  = "flash";   // non-singleton — every event matters
private final RenderActionQueue renderActions =
    new RenderActionQueue(ACTION_COLOUR);               // pass singleton names to constructor

// From any thread:
renderActions.enqueue(ACTION_COLOUR, ctx -> uColour.set(newColour));
renderActions.enqueue(ACTION_FLASH,  ctx -> triggerFlash());

// In doRender() — must be called every frame:
renderActions.processAll(ctx);
```

---

## Colours

`StandardColors` is a pre-built enum of common RGBA colours:

```java
// As Vector4f:
Vector4f red  = StandardColors.RED.color;
Vector4f half = StandardColors.BLUE.withAlpha(0.5f);

// All values (useful for random colour selection):
StandardColors[] all = StandardColors.values();
Vector4f random = all[rng.nextInt(all.length)].color;
```
