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
			public void handle(KeyEvent event) {
				KeyCode key = event.getCode();
				if (key == KeyCode.N) {
					controller.newfile();
				} else if (key == KeyCode.L) {
					controller.load();
				} else if (key == KeyCode.S) {
					controller.save();
				} else if (key == KeyCode.A) {
					controller.saveTo();
				} else if (key == KeyCode.I) {
					controller.exportImage();
				} else if (key == KeyCode.C) {
					controller.exportColor();
				} else if (key == KeyCode.B) {
					controller.exportBlueprint();
				} else if (key == KeyCode.O) {
					controller.setting();
				}
			}
		};
	}
}
