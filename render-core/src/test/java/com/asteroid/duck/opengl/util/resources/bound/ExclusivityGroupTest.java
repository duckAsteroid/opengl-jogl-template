package com.asteroid.duck.opengl.util.resources.bound;

import com.asteroid.duck.opengl.util.resources.Resource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ExclusivityGroupTest {

    record Thing(String name) implements Resource {
        @Override
        public void dispose() {}
    }

    private final List<String> glCalls = new ArrayList<>();

    private final ExclusivityGroup<Thing> group = new ExclusivityGroup<>(null, new Binder<>() {
        @Override
        public Class<Thing> resourceType() { return Thing.class; }

        @Override
        public void bind(Thing resource) { glCalls.add("bind " + resource.name()); }

        @Override
        public Thing unbind(Thing resource) { glCalls.add("unbind"); return null; }
    });

    private final Thing a = new Thing("a");
    private final Thing b = new Thing("b");

    @Test
    void rebindingTheBoundResourceSkipsTheGlCall() {
        group.bind(a);
        group.bind(a);
        group.bind(b);

        assertEquals(List.of("bind a", "bind b"), glCalls);
    }

    @Test
    void invalidateMakesTheNextBindIssueTheGlCall() {
        group.bind(a);
        group.invalidate(); // e.g. after a raw glBindVertexArray
        group.bind(a);

        assertEquals(List.of("bind a", "bind a"), glCalls);
    }

    @Test
    void forgetOnlyClearsTheMatchingResource() {
        group.bind(a);
        group.forget(b);
        group.bind(a);
        group.forget(a);
        group.bind(a);

        assertEquals(List.of("bind a", "bind a"), glCalls);
    }

    @Test
    void bindReleasesItsLock() throws Exception {
        group.bind(a);
        group.bind(a);

        // another thread must not block on a lock left held by bind
        CompletableFuture.runAsync(() -> group.bind(b)).get(5, TimeUnit.SECONDS);
        assertEquals(List.of("bind a", "bind b"), glCalls);
    }
}
