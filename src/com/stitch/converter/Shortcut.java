package com.stitch.converter;

import com.stitch.converter.view.OverviewController;

import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Shortcut {
	public static EventHandler<KeyEvent> get(final OverviewController controller) {
		return new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent event) {
				if(event.isControlDown() == true) {
					final KeyCode key = event.getCode();
					switch(key) {
					case L: 
						controller.loadMenu();
						return;
					case S:
						controller.saveMenu();
						return;
					case A:
						controller.saveToMenu();
						return;
					case I:
						controller.exportConvertedImageMenu();
						return;
					case C:
						controller.exportStitchListMenu();
						return;
					case B:
						controller.exportBlueprintMenu();
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

	private Shortcut() {
		throw new AssertionError();
	}
}
