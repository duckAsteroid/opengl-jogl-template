package com.asteroid.duck.opengl.util.resources.bound;

import com.asteroid.duck.opengl.util.resources.Resource;

import java.util.concurrent.locks.ReentrantLock;

/**
 * An exclusivity group manages the binding of resources of a specific type, ensuring that only one resource of that
 * type is bound at any given time.
 * @param <T> the type of resource managed by this exclusivity group
 */
public class ExclusivityGroup<T extends Resource> {
    // The binding context this exclusivity group belongs to
    private final BindingContext bindingContext;
    // The binder used to bind and unbind resources of type T
    private final Binder<T> binder;
    // The currently bound resource of type T, or null if no resource is bound
    private T boundResource = null;
    // A lock to ensure thread-safe access to the binding and unbinding operations
    private final ReentrantLock lock = new ReentrantLock(false);

    public ExclusivityGroup(BindingContext bindingContext, Binder<T> binder) {
        this.bindingContext = bindingContext;
        this.binder = binder;
    }

    public BindingContext context() {
        return bindingContext;
    }

    public BoundResource<T> bind(T resource) {
        try {
            lock.lock();
            binder.bind(resource);
            return new BoundResource<>(this, resource);
        }
        catch(BindingException e) {
            return null;
        }
    }

    public void unbind(T resource) throws BindingException {
        try {
            lock.lock();
            boundResource = binder.unbind(resource);
        }
        finally {
            lock.unlock();
        }
    }
}
