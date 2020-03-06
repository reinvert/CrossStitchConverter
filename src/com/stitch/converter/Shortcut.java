package com.stitch.converter;

import com.stitch.converter.view.OverviewController;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Shortcut {
	private Shortcut() {
		throw new AssertionError();
	}

	public static EventHandler<KeyEvent> get(final OverviewController controller) {
		return new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				if(event.isControlDown() == true) {
					final KeyCode key = event.getCode();
					switch(key) {
					case L: 
						controller.load();
						return;
					case S:
						controller.save();
						return;
					case A:
						controller.saveTo();
						return;
					case I:
						controller.exportImage();
						return;
					case C:
						controller.exportImage();
						return;
					case B:
						controller.exportBlueprint();
						return;
					case O:
						controller.setting();
						return;
					default:
						return;
					}
				}
			}
		};
	}
}
