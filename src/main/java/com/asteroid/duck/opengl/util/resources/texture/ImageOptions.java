package com.asteroid.duck.opengl.util.resources.texture;

/**
 * Represents various options for loading images into textures.
 *
 * @param flipY If true, the image will be flipped vertically during loading.
 * @param singleLine If true, the image will be loaded as a single line texture.
 * @param dataFormat The type of the image data (e.g., RGBA, RGB, etc.).
 */
public record ImageOptions(boolean flipY, boolean singleLine, DataFormat dataFormat) {
    /**
     * A default ImageOptions instance with flipY set to true, singleLine set to false, and type set to RGBA.
     */
    public static final ImageOptions DEFAULT = new ImageOptions(true, false, DataFormat.RGBA);

    /**
     * Returns a new ImageOptions instance with the flipY flag toggled.
     * If the current flipY flag is true, the returned instance will have flipY set to false.
     * If the current flipY flag is false, the returned instance will have flipY set to true.
     *
     * @return A new ImageOptions instance with the flipY flag toggled.
     */
    public ImageOptions withNoFlip() {
        if (flipY) {
            return new ImageOptions(false, singleLine, dataFormat);
        }
        return this;
    }

    /**
     * Returns a new ImageOptions instance with the singleLine flag toggled.
     * If the current singleLine flag is false, the returned instance will have singleLine set to true.
     * If the current singleLine flag is true, the returned instance will have singleLine set to false.
     *
     * @return A new ImageOptions instance with the singleLine flag toggled.
     */
    public ImageOptions withSingleLine() {
        if (!singleLine) {
            return new ImageOptions(flipY, true, dataFormat);
        }
        return this;
    }

    /**
     * Returns a new ImageOptions instance with the type set to the specified value.
     * If the current type is equal to the specified value, the returned instance will be the same as the current instance.
     * If the current type is not equal to the specified value, the returned instance will have the type set to the specified value.
     *
     * @param dataFormat The new type for the ImageOptions instance.
     * @return A new ImageOptions instance with the type set to the specified value.
     */
    public ImageOptions withType(DataFormat dataFormat) {
        if (this.dataFormat != dataFormat) {
            return new ImageOptions(flipY, singleLine, dataFormat);
        }
        return this;
    }
}
