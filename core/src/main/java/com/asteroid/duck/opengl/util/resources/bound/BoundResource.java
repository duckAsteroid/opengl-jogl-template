package com.asteroid.duck.opengl.util.resources.bound;

import com.asteroid.duck.opengl.util.resources.Resource;

public class BoundResource<T extends Resource> {
    private final ExclusivityGroup<T> exclusivityGroup;
    private final T resource;

    public BoundResource(ExclusivityGroup<T> exclusivityGroup, T resource) {
        this.exclusivityGroup = exclusivityGroup;
        this.resource = resource;
    }

    public boolean unbind() {
        try {
            exclusivityGroup.unbind(resource);
            return true;
        }
        catch(BindingException e) {
            return false;
        }
    }
}
