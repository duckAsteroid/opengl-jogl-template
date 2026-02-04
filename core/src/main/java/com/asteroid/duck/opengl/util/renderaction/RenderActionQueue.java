package com.asteroid.duck.opengl.util.renderaction;

import com.asteroid.duck.opengl.util.RenderContext;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Queue for render actions.
 * Actions have a "type" (String).
 * Certain types can be marked as "singleton"; this means that only the latest action of that type is kept in the 
 * queue. (e.g. variable updates where only the latest value matters)
 */
public class RenderActionQueue {
    private final Queue<Action> queue = new ConcurrentLinkedQueue<>();
    private final Map<String, Action> latestByType = new ConcurrentHashMap<>();
    private final Set<String> singletonTypes;

    public RenderActionQueue(String ... singletonTypes) {
        this(new HashSet<>(Arrays.asList(singletonTypes)));
    }
    /**
     * Constructs a new RenderActionQueue with a given list of singleton action types.
     * @param singletonTypes the set of action types that are singleton
     */
    public RenderActionQueue(Set<String> singletonTypes) {
        this.singletonTypes = new HashSet<>(singletonTypes);
    }
    
    public void addSingletonType(String actionType) {
        singletonTypes.add(actionType);
    }
    
    public void removeSingletonType(String actionType) {
        singletonTypes.remove(actionType);
    }

    /**
     * Enqueues an action with the given type.
     * If the action type is singleton, it replaces any existing action of that type in the queue.
     * Otherwise, it is simply added to the end of the queue.
     * @param actionType the type of the action
     * @param action the action to enqueue
     */
    public void enqueue(String actionType, Action action) {
        if (action != null) {
            if (singletonTypes.contains(actionType)) {
                latestByType.put(actionType, action);
            } else {
                queue.offer(action);
            }
        }
    }

    /**
     * Drains the queue, returning a list of all actions in the order they were added.
     * Deduplicated actions are included as the latest version.
     * @return a list of all actions in the queue
     */
    public List<Action> drain() {
        List<Action> actions = new ArrayList<>(queue);
        queue.clear();
        actions.addAll(latestByType.values());
        latestByType.clear();
        return actions;
    }

    public void processAll(RenderContext ctx) {
        for (Action action : drain()) {
            action.onRender(ctx);
        }
    }
}
