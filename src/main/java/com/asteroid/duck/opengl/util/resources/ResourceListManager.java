package com.asteroid.duck.opengl.util.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Simple container that tracks a set of Resource instances and disposes them when requested.
 *
 * <p>This class intentionally does one thing: own a list of Resources of a single kind (R).
 * It provides add/remove/stream operations and will destroy (dispose) all contained resources
 * when destroy() is called.
 *
 * Use this as the basic building block for resource ownership. Filesystem loaders,
 * texture factories, and shader loaders should be implemented separately and can
 * register produced Resources with an instance of this class.
 *
 * @param <R> the specific Resource subtype tracked by this manager
 */
public class ResourceListManager<R extends Resource> implements Resource {
	// Thread-safe list wrapper for simple concurrent access
	private final Collection<R> resources = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Add a resource to be tracked by this manager. Null values are ignored.
	 *
	 * @param resource the resource to track
	 */
	public void add(R resource) {
		if (resource == null) return;
		resources.add(resource);
	}

	/**
	 * Remove a resource from tracking. Does not destroy the resource.
	 *
	 * @param resource the resource to stop tracking
	 * @return true if the collection contained the resource
	 */
	public boolean remove(R resource) {
		if (resource == null) return false;
		return resources.remove(resource);
	}

	/**
	 * Stream the currently tracked resources.
	 *
	 * @return a Stream of tracked Resource instances
	 */
	public Stream<R> stream() {
		synchronized (resources) {
			return new ArrayList<>(resources).stream();
		}
	}

	/**
	 * Destroy and remove all tracked resources. After this call the manager will be empty.
	 *
	 * Note: previously this logic lived in clear(); it is now performed directly by destroy().
	 */
	@Override
	public void destroy() {
		Collection<R> snapshot;
		synchronized (resources) {
			snapshot = new ArrayList<>(resources);
			resources.clear();
		}
		snapshot.forEach(r -> {
			try {
				r.destroy();
			} catch (Exception ignored) {
				// best-effort cleanup, swallow to ensure all resources are attempted
			}
		});
	}

	/**
	 * Convenience: number of tracked resources.
	 *
	 * @return tracked resource count
	 */
	public int size() {
		return resources.size();
	}

	@Override
	public String toString() {
		return "ResourceManager[size=" + size() + "]";
	}

}

