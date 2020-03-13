package com.stitch.converter;

import java.io.File;
import java.io.IOException;

import com.stitch.converter.model.StitchImage;
import com.stitch.converter.view.OverviewController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	enum FileStatus {
		ERROR, SIZE_TOO_LARGE, SUCCESS
	}

	public static void main(String[] args) {
		System.setProperty("prism.lcdtext", "false");
		launch(args);
	}
	private OverviewController controller;
	private Listener listener = new Listener() {
		@Override
		public void onFinished(final StitchImage image) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					controller.setImage(image);
				}
			});
		}
	};

	private Stage primaryStage;

	public BorderPane rootLayout;

	public void initRootLayout() {
		try {
			final FXMLLoader loader = new FXMLLoader(new File("resources/Overview.fxml").toURI().toURL(),
					Resources.getBundle());
			rootLayout = (BorderPane) loader.load();
			final StringBuilder style = new StringBuilder();
			style.append("-fx-font: ");
			style.append(Preferences.getString("fontSize", "11")).append("px ");
			style.append(Preferences.getString("fontType", "Dotum")).append(";");
			rootLayout.setStyle(style.toString());

			final Scene scene = new Scene(rootLayout);

			controller = loader.getController();
			controller.setStage(primaryStage);
			controller.setApp(this);

			scene.addEventHandler(KeyEvent.KEY_PRESSED, Shortcut.get(controller));
			primaryStage.setScene(scene);

			final Image image = new Image(new File("resources/icons8-needle-50.png").toURI().toURL().toString());
			primaryStage.getIcons().add(image);
			primaryStage.show();
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("error_has_occurred"));
		}
	}

	public void load(final GraphicsEngine.Builder builder) {
		Platform.runLater(new Thread(builder.setMode(GraphicsEngine.Mode.LOAD).setListener(listener).build()));
	}

	@Override
	public void start(final Stage primaryStage) {
		this.primaryStage = primaryStage;
		this.primaryStage.setTitle(Resources.getString("title"));
		this.primaryStage.setMaximized(true);
		initRootLayout();
	}

	public void startConversion(final GraphicsEngine.Builder builder) {
		Platform.runLater(new Thread(builder.setMode(GraphicsEngine.Mode.NEW_FILE).setListener(listener).build()));
	}

}
