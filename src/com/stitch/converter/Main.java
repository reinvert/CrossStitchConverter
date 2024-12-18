package com.stitch.converter;

import java.io.File;
import com.stitch.converter.model.StitchImage;
import com.stitch.converter.view.OverviewController;
import com.stitch.converter.view.ProgressWindow;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        launch(args);
    }

    private OverviewController controller;
    private ProgressWindow progressWindow;
    private ProgressListener progressListener;

    private final Listener listener = new Listener() {
        @Override
        public void onFinished(final StitchImage image) {
            Platform.runLater(() -> {
                controller.setImage(image);
            });
        }
    };

    private Stage primaryStage;

    @Override
    public void start(final Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(Resources.getString("title"));
        this.primaryStage.setMaximized(true);
        initRootLayout();
    }
    
    public void initRootLayout() {
        try {
            final FXMLLoader loader = new FXMLLoader();
            loader.setLocation(new File("resources/Overview.fxml").toURI().toURL());
            loader.setResources(Resources.getBundle());
            final BorderPane rootLayout = (BorderPane) loader.load();
            final int fontSize = Preferences.getInteger("fontSize", 13);
            final String fontType = Preferences.getValue("fontType", "Malgun Gothic");
            final String style = new StringBuilder("-fx-font: ").append(fontSize).append("px \"").append(fontType).append("\";").toString();
            rootLayout.setStyle(style);

            final Scene scene = new Scene(rootLayout);

            controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setApp(this);

            scene.addEventHandler(KeyEvent.KEY_PRESSED, Shortcut.get(controller));
            primaryStage.setScene(scene);
            try {
                final Image icon = new Image("file:resources/icon/icons8-needle-50.png");
                primaryStage.getIcons().add(icon);

            } catch (final Exception iconException) {
                LogPrinter.print(iconException);
                LogPrinter.error(Resources.getString("error_icon_load"));
            }
            progressListener = new ProgressListener() {
                @Override
                public void onProgress(double progress, String message) {
                    progressWindow.updateProgress(progress, message);
                }

    			@Override
    			public void finished() {
					progressWindow.hide();
    			}
            };
            
            primaryStage.show();
        } catch (final Exception e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("error_has_occurred"));
        }
    }
    
    public void load(final GraphicsEngine.Builder builder, GraphicsEngine.Mode mode) {
        progressWindow = new ProgressWindow(primaryStage);
        builder.setProgressListener(progressListener);
        progressWindow.show();
        new Thread(builder.setMode(mode).addListener(listener).build()).start();
    }
}
