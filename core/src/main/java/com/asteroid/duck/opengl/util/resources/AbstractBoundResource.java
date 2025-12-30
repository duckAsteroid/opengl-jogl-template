package com.asteroid.duck.opengl.util.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread safe basis for objects that can be bound and unbound (e.g. buffers)
 * Clients should call {@link #bind()} and {@link #unbind()}.
 * Subclasses should implement {@link #bindImpl()} and {@link #unbindImpl()} to do the actual
 * work of binding/unbinding
 */
public abstract class AbstractBoundResource implements Resource {
    private static final Logger log = LoggerFactory.getLogger(AbstractBoundResource.class);
    // lock to keep bound state thread safe
    private final ReentrantLock lock = new ReentrantLock(false);
    // actual bound state of this resource
    private boolean bound = false;

    /**
     * An exception thrown by subclasses when they fail to bind/unbind
     */
    protected static class BindingException extends Exception {
        public BindingException(String message) { super( message); }
        public BindingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Bind this resource (if not already bound). Calling this multiple times has no effect and
     * subsequent calls are ignored.
     */
    public void bind() {
        if(bound) return;
        lock.lock();
        try {
            if (!bound) {
                bindImpl();
                bound = true;
            }
        }
        catch (BindingException e) {
            log.error("Binding failed", e);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This is where the actual work of binding should take place in a subclass. If there is a problem binding
     * then throw {@link BindingException}.
     * This will only be called when the object is not bound.
     * @throws BindingException if the object cannot be bound (the object remains unbound)
     */
    protected abstract void bindImpl() throws BindingException;

    /**
     * Is this object bound
     * @return true if bound
     */
    public boolean isBound() {
        return bound;
    }

    /**
     * Unbind this resource (only if the resource is currently bound). Calling this method mutliple times
     * or on an unbound resource has no effect and is ignored.
     */
    public void unbind() {
        if(!bound) return;
        lock.lock();
        try {
            if (bound) {
                unbindImpl();
                bound = false;
            }
        }
        catch (BindingException e) {
            log.error("Unbinding failed", e);
        }
        finally {
            lock.unlock();
        }
    }

    /**
     * This is where the actual work of unbinding should take place in a subclass. If there is a problem unbinding
     * then throw {@link BindingException}.
     * This will only be called when the object is bound.
     * @throws BindingException if the object cannot be unbound (the object remains bound)
     */
    protected abstract void unbindImpl() throws BindingException;
}
