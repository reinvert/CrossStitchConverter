package com.stitch.converter;

import java.io.IOException;
import java.util.ArrayList;

import com.stitch.converter.model.StitchImage;
import com.stitch.converter.view.ControllerListener;
import com.stitch.converter.view.OverviewController;
import com.sun.javafx.application.HostServicesDelegate;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	enum FileStatus {
		ERROR, SIZE_TOO_LARGE, SUCCESS
	}

	private GraphicsEngine graphicsEngine;

	private OverviewController controller;
	private Stage primaryStage;
	private BorderPane rootLayout;

	private ArrayList<ControllerListener> controllerListener = new ArrayList<ControllerListener>();
	private final MessageListener listener = new MessageListener() {
		@Override
		public void onTaskCompleted(final StitchImage stitchImage, final double scale) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					controller.setImage(stitchImage, scale);
				}
			});
		}
	};

	public static void main(String[] args) {
		launch(args);
	}

	public void addListener(final ControllerListener listener) {
		controllerListener.add(listener);
	}

	public void initRootLayout() {
		try {
			final FXMLLoader loader = new FXMLLoader(Main.class.getResource("view/Overview.fxml"),
					Resources.getBundle());
			rootLayout = (BorderPane) loader.load();

			final Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.show();

			controller = loader.getController();
			controller.setStage(primaryStage);
			controller.setApp(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load(final GraphicsEngine.Builder builder) {
		graphicsEngine = builder.addListener(listener).setMode(GraphicsEngine.Mode.LOAD).build();
		graphicsEngine.start();
	}

	public void startConversion(GraphicsEngine.Builder builder) {
		graphicsEngine = builder.addListener(listener).setMode(GraphicsEngine.Mode.NEW_FILE).build();
		graphicsEngine.start();
	}

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle(Resources.getString("title"));
		this.primaryStage.setMaximized(true);
		initRootLayout();
	}

}
