package com.asteroid.duck.opengl.util.resources.texture;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

class DataFormatTest {

    @Test
    void rgbaStagingImageIsIntArgb() {
        assertEquals(BufferedImage.TYPE_INT_ARGB, DataFormat.RGBA.apply(new Dimension(2, 2)).getType());
    }

    @Test
    void rgbaPixelDataIsRgbaBytesRowByRow() {
        BufferedImage img = DataFormat.RGBA.apply(new Dimension(2, 2));
        img.setRGB(0, 0, 0x80112233); // A=0x80 R=0x11 G=0x22 B=0x33
        img.setRGB(1, 0, 0xFFFF0000);
        img.setRGB(0, 1, 0x00000000);
        img.setRGB(1, 1, 0xFF0000FF);

        ByteBuffer data = DataFormat.RGBA.pixelData(img);

        assertEquals(16, data.remaining());
        assertEquals(ByteOrder.nativeOrder(), data.order());
        assertArrayEquals(new byte[] {
                0x11, 0x22, 0x33, (byte) 0x80,
                (byte) 0xFF, 0, 0, (byte) 0xFF,
                0, 0, 0, 0,
                0, 0, (byte) 0xFF, (byte) 0xFF,
        }, bytes(data));
    }

    @Test
    void rgbaPixelDataConvertsOtherImageTypes() {
        BufferedImage bgr = new BufferedImage(2, 1, BufferedImage.TYPE_3BYTE_BGR);
        bgr.setRGB(0, 0, 0xFF102030);
        bgr.setRGB(1, 0, 0xFFA0B0C0);

        assertArrayEquals(new byte[] {
                0x10, 0x20, 0x30, (byte) 0xFF,
                (byte) 0xA0, (byte) 0xB0, (byte) 0xC0, (byte) 0xFF,
        }, bytes(DataFormat.RGBA.pixelData(bgr)));
    }

    @Test
    void rgbaPixelDataReadsSubimagesFromTheirOwnOrigin() {
        BufferedImage img = DataFormat.RGBA.apply(new Dimension(3, 3));
        img.setRGB(1, 1, 0xFF445566);
        BufferedImage sub = img.getSubimage(1, 1, 1, 1);

        assertArrayEquals(new byte[] {0x44, 0x55, 0x66, (byte) 0xFF}, bytes(DataFormat.RGBA.pixelData(sub)));
    }

    @Test
    void grayPixelDataIsLuminance() {
        BufferedImage img = DataFormat.RGBA.apply(new Dimension(3, 1));
        img.setRGB(0, 0, 0xFFFFFFFF);
        img.setRGB(1, 0, 0xFF000000);
        img.setRGB(2, 0, 0xFF00FF00);

        ByteBuffer data = DataFormat.GRAY.pixelData(img);

        assertEquals(1.0f, data.getFloat(0), 1e-5f);
        assertEquals(0.0f, data.getFloat(4), 1e-5f);
        assertEquals(0.7152f, data.getFloat(8), 1e-5f);
    }

    @Test
    void gray16PixelDataIsScaledLuminance() {
        BufferedImage img = DataFormat.RGBA.apply(new Dimension(2, 1));
        img.setRGB(0, 0, 0xFFFFFFFF);
        img.setRGB(1, 0, 0xFF000000);

        ByteBuffer data = DataFormat.GRAY_16.pixelData(img);

        assertEquals((short) 65535, data.getShort(0));
        assertEquals((short) 0, data.getShort(2));
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.duplicate().get(out);
        return out;
    }
}
