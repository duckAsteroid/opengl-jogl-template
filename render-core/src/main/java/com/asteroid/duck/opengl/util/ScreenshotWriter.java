package com.asteroid.duck.opengl.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes screenshots as PNG files off the GL thread, without ever letting the JVM exit with a
 * partially written file.
 *
 * <p>Each write runs on its own non-daemon platform thread, so a normal JVM shutdown waits for it.
 * Because {@link System#exit} does not wait for non-daemon threads, a shutdown hook (registered
 * once, on first use) also joins every in-flight write, and {@link #awaitPending()} lets callers
 * such as {@link GLWindow#dispose()} block explicitly.</p>
 *
 * <p>The image is encoded into a temporary sibling file, {@linkplain FileChannel#force forced} to
 * the storage device, then atomically renamed onto the destination, so the destination path only
 * ever holds a complete, durable PNG.</p>
 */
final class ScreenshotWriter {
	private static final Logger LOG = LoggerFactory.getLogger(ScreenshotWriter.class);

	private static final Set<Thread> PENDING = ConcurrentHashMap.newKeySet();

	static {
		Runtime.getRuntime().addShutdownHook(
				Thread.ofPlatform().name("screenshot-shutdown-flush").unstarted(ScreenshotWriter::awaitPending));
	}

	private ScreenshotWriter() {}

	/** Starts writing {@code image} to {@code path} as a PNG on a non-daemon thread. */
	static void writeAsync(BufferedImage image, Path path) {
		Thread t = Thread.ofPlatform()
				.name("screenshot-writer")
				.daemon(false)
				.unstarted(() -> {
					try {
						write(image, path);
						LOG.info("Screenshot saved: {}", path.toAbsolutePath());
					} catch (IOException e) {
						LOG.error("Failed to write screenshot to {}", path, e);
					} finally {
						PENDING.remove(Thread.currentThread());
					}
				});
		// register before starting so awaitPending() can never miss a write that is under way
		PENDING.add(t);
		t.start();
	}

	/** Blocks until every screenshot write started so far has finished (successfully or not). */
	static void awaitPending() {
		boolean interrupted = false;
		for (Thread t : PENDING) {
			while (t.isAlive()) {
				try {
					t.join();
				} catch (InterruptedException e) {
					// keep waiting: returning early would let the JVM exit mid-write
					interrupted = true;
				}
			}
		}
		if (interrupted) Thread.currentThread().interrupt();
	}

	/** Synchronously encodes, fsyncs and atomically publishes a PNG at {@code path}. */
	static void write(BufferedImage image, Path path) throws IOException {
		Path target = path.toAbsolutePath();
		Path dir = target.getParent();
		Files.createDirectories(dir);
		Path tmp = Files.createTempFile(dir, "." + target.getFileName(), ".tmp");
		try {
			try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
				OutputStream out = new BufferedOutputStream(Channels.newOutputStream(ch), 1 << 16);
				if (!ImageIO.write(image, "PNG", out)) {
					throw new IOException("No ImageIO writer available for PNG");
				}
				out.flush();
				ch.force(true);
			}
			try {
				Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
		forceDirectory(dir);
	}

	/** Best-effort fsync of the directory so the rename itself is durable (a no-op where unsupported). */
	private static void forceDirectory(Path dir) {
		try (FileChannel ch = FileChannel.open(dir, StandardOpenOption.READ)) {
			ch.force(true);
		} catch (IOException | UnsupportedOperationException ignored) {
			// Windows cannot open a directory as a channel; the file data is already forced.
		}
	}
}
