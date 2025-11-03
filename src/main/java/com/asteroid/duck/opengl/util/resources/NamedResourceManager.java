package com.asteroid.duck.opengl.util.resources;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

/**
 * NamedResourceManager extends ResourceManager to provide a simple name->resource mapping.
 * It registers resources with the inherited tracking mechanisms so they are destroyed when
 * this manager is destroyed.
 * <p>
 * Example usage:
 * NamedResourceManager<Texture> nm = new NamedResourceManager<>();
 * nm.put("diffuse", texture);
 * Texture t = nm.get("diffuse");
 */
public class NamedResourceManager<R extends Resource> extends ResourceListManager<R> {
    private final Map<String, R> map = Collections.synchronizedMap(new java.util.HashMap<>());

    /**
     * Associate a name with a resource and register it for disposal.
     *
     * @param name     logical name
     * @param resource resource instance (may be null)
     */
    public void put(String name, R resource) {
        map.put(name, resource);
        if (resource != null) {
            add(resource);
        }
    }

    /**
     * Retrieve a resource by name. May return null if not present.
     *
     * @param name logical name
     * @return the associated resource or null
     */
    public R get(String name) {
        return map.get(name);
    }

    /**
     * Remove the mapping for the given name and stop tracking the resource (does not destroy it).
     *
     * @param name logical name to remove
     * @return the removed resource or null
     */
    public R removeByName(String name) {
        R r = map.remove(name);
        if (r != null) {
            super.remove(r);
        }
        return r;
    }

    /**
     * Returns true if a mapping exists for the name.
     */
    public boolean contains(String name) {
        return map.containsKey(name);
    }

    /**
     * Stream the name->resource entries (snapshot).
     */
    public Stream<Map.Entry<String, R>> entries() {
        synchronized (map) {
            return new ArrayList<>(map.entrySet()).stream();
        }
    }

    /**
     * When destroyed, delegate to parent to destroy tracked resources and clear name mappings.
     */
    @Override
    public void destroy() {
        super.destroy();
        map.clear();
    }
}
