package com.asteroid.duck.opengl.util;

/**
 * A {@link CompositeRenderItem} that selects one of it's children to render
 */
public class SwitchableRenderItem extends CompositeRenderItem {
	private int selectedItem = 0;

	@Override
	public void doRender(RenderContext ctx) {
		if (!items().isEmpty()) {
			if (selectedItem < 0) {
				selectedItem = items().size() -1;
			}
			if (selectedItem >= items().size()) {
				selectedItem = 0;
			}
			items().get(selectedItem).doRender(ctx);
		}
	}

	public void next() {
		selectedItem++;
	}

	public void previous() {
    selectedItem--;
  }
}
