package com.asteroid.duck.opengl.util.keys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeyAction implements Comparable<KeyAction> {
	private static final Logger LOG = LoggerFactory.getLogger(KeyAction.class);
	private final KeyCombination combination;
	private final KeyRegistry registry;
	private final Runnable runnable;
	private final String description;
	private boolean enabled = true;

	public KeyAction(KeyRegistry registry, KeyCombination combination, Runnable runnable, String description) {
		this.registry = registry;
		this.combination = combination;
		this.runnable = runnable;
		this.description = description;
	}

	public void run() {
    if (enabled) {
      runnable.run();
    }
		else {
	    if (LOG.isInfoEnabled()) {
		    LOG.info("{} is disabled", combination);
	    }
    }
  }

	public void unregister() {
		if (!registry.unregister(combination)) {
			throw new IllegalStateException("No longer registered");
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getDescription() {
		return description;
	}

	public KeyCombination getCombination() {
		return combination;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public int compareTo(KeyAction o) {
		return combination.compareTo(o.combination);
	}
}
