package com.asteroid.duck.opengl.util.resources.bound;

import com.asteroid.duck.opengl.util.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An exclusivity group manages the binding of resources of a specific type, ensuring that only one resource of that
 * type is bound at any given time.
 * @param <T> the type of resource managed by this exclusivity group
 */
public class ExclusivityGroup<T extends Resource> {
    private static final Logger LOG = LoggerFactory.getLogger(ExclusivityGroup.class);
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
        Objects.requireNonNull(resource, "Resource must not be null");
        try {
            lock.lock();
            // is this the same resource as the currently bound one?
            if(Objects.equals(boundResource, resource)) {
                return new BoundResource<>(this, resource);
            }
            // bind the new resource
            binder.bind(resource);
            this.boundResource = resource;
            if (LOG.isTraceEnabled()) {
                LOG.trace("Bound exclusive resource type {}: {}", binder.resourceType().getSimpleName(), resource);
            }
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Unbound exclusive resource type {}: {}", binder.resourceType().getSimpleName(), resource);
            }
        }
        finally {
            lock.unlock();
        }
    }
}
