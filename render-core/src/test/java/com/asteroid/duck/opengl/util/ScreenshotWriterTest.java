package com.asteroid.duck.opengl.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ScreenshotWriterTest {

    @TempDir
    Path dir;

    private static BufferedImage image(int rgb) {
        BufferedImage img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < img.getHeight(); y++)
            for (int x = 0; x < img.getWidth(); x++)
                img.setRGB(x, y, rgb ^ (x * 31 + y));
        return img;
    }

    @Test
    void asyncWriteIsCompleteOnceAwaitPendingReturns() throws IOException {
        Path out = dir.resolve("nested/shot.png");
        BufferedImage src = image(0x336699);

        ScreenshotWriter.writeAsync(src, out);
        ScreenshotWriter.awaitPending();

        BufferedImage read = ImageIO.read(out.toFile());
        assertNotNull(read, "file should be a complete, decodable PNG");
        assertEquals(src.getWidth(), read.getWidth());
        assertEquals(src.getHeight(), read.getHeight());
        assertEquals(src.getRGB(5, 7) & 0xFFFFFF, read.getRGB(5, 7) & 0xFFFFFF);
    }

    @Test
    void leavesNoTempFilesAndReplacesExisting() throws IOException {
        Path out = dir.resolve("shot.png");
        Files.writeString(out, "not a png");

        ScreenshotWriter.write(image(0xFF0000), out);

        assertNotNull(ImageIO.read(out.toFile()));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count(), "temp file should have been renamed away");
        }
    }
}
