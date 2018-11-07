package com.stitch.converter;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.stitch.converter.model.StitchImage;
import com.stitch.converter.view.OverviewController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	enum FileStatus {
		ERROR, SIZE_TOO_LARGE, SUCCESS
	}

	private GraphicsEngine graphicsEngine;

	private OverviewController controller;
	private Stage primaryStage;
	public BorderPane rootLayout;

	private Listener listener = new Listener() {
		@Override
		public void onFinished(StitchImage image) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					controller.setImage(image);
				}
			});
		}
	};

	public static void main(String[] args) {
		launch(args);
	}

	public void initRootLayout() {
		try {
			final FXMLLoader loader = new FXMLLoader(new File("resources/Overview.fxml").toURI().toURL()
					, Resources.getBundle());
			rootLayout = (BorderPane) loader.load();

			final Scene scene = new Scene(rootLayout);

			controller = loader.getController();
			controller.setStage(primaryStage);
			controller.setApp(this);

			scene.addEventHandler(KeyEvent.KEY_PRESSED, Shortcut.get(controller));
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void load(final GraphicsEngine.Builder builder) {
		graphicsEngine = builder.setMode(GraphicsEngine.Mode.LOAD).setListener(listener).build();
		final Thread engineThread = new Thread(graphicsEngine);
		engineThread.start();
	}

	public void startConversion(GraphicsEngine.Builder builder) {
		graphicsEngine = builder.setMode(GraphicsEngine.Mode.NEW_FILE).setListener(listener).build();
		final Thread engineThread = new Thread(graphicsEngine);
		engineThread.start();
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle(Resources.getString("title"));
		this.primaryStage.setMaximized(true);
		initRootLayout();
	}

}
