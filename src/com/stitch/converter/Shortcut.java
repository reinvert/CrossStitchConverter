package com.stitch.converter;

import com.stitch.converter.view.OverviewController;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

class Shortcut {
	static EventHandler<KeyEvent> get(final OverviewController controller) {
		return new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				final KeyCode code = event.getCode();
				switch (code) {
				case W:
				case S:
				case A:
				case D:
					controller.highlightPixel(code);
				default:
					return;
				}
			}
		};
	}

	private Shortcut() {
		throw new AssertionError();
	}
}
