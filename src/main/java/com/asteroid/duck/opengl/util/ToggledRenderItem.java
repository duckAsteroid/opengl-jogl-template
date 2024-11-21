package com.asteroid.duck.opengl.util;

import com.asteroid.duck.opengl.util.keys.KeyCombination;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;

public class ToggledRenderItem implements RenderedItem {
	private final Function<RenderContext, Boolean> enabled;
	private final RenderedItem target;
	private Consumer<RenderContext> initHandler;

	public ToggledRenderItem(Function<RenderContext, Boolean> enabled, RenderedItem target) {
		this.enabled = enabled;
		this.target = target;
	}

	void setInitHandler(Consumer<RenderContext> initHandler) {
		this.initHandler = initHandler;
	}

	@Override
	public void init(RenderContext ctx) throws IOException {
		if (initHandler != null) {
			initHandler.accept(ctx);
		}
		target.init(ctx);
	}

	@Override
	public void doRender(RenderContext ctx) {
		if (enabled.apply(ctx)) {
			target.doRender(ctx);
		}
	}

	@Override
	public void dispose() {
		target.dispose();
	}

	public static class Toggle implements Function<RenderContext, Boolean> {
		private boolean enabled = true;
		@Override
		public Boolean apply(RenderContext renderContext) {
			return enabled;
		}

		public void toggle() {
			enabled =!enabled;
		}
	};

	public static class Frequency implements Function<RenderContext, Boolean> {
		// period of enabled (seconds)
		private final long period;
		private Double lastTime = null;

		public Frequency(long period) {
			this.period = period;
		}

		@Override
		public Boolean apply(RenderContext ctx) {
			double now = ctx.getTimer().elapsed();
			if (lastTime == null || (now - lastTime) >= period) {
				// firing
				System.out.println("Fired");
				lastTime = now;
				return true;
			}
			return false;
		}
	}

	public static ToggledRenderItem wrap(RenderedItem target, char toggleKey, String description) {
		Toggle toggle = new Toggle();
		ToggledRenderItem result = new ToggledRenderItem(toggle, target);
		result.setInitHandler((ctx) -> {
			ctx.getKeyRegistry().registerKeyAction(KeyCombination.simple(toggleKey), toggle::toggle, description);
		});
		return result;
	}
}
