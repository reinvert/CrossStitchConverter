package com.stitch.converter;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

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

    private static final String OVERVIEW_FXML = "Overview.fxml";
    private static final String APPLICATION_ICON =
            "icon/icons8-needle-50.png";

    private OverviewController controller;
    private ProgressWindow progressWindow;
    private ProgressListener progressListener;
    private Stage primaryStage;

    /**
     * Receives completed GraphicsEngine results.
     *
     * GraphicsEngine runs outside the JavaFX Application Thread, so UI
     * updates must be scheduled through Platform.runLater().
     */
    private final Listener listener = new Listener() {

        @Override
        public void onFinished(final StitchImage image) {
            Platform.runLater(() -> {
                if (controller != null) {
                    controller.setImage(image);
                }
            });
        }
    };

    public static void main(final String[] args) {
        /*
         * Preserve the application's existing JavaFX text-rendering
         * configuration.
         */
        System.setProperty("prism.lcdtext", "false");

        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
        this.primaryStage = Objects.requireNonNull(primaryStage);

        this.primaryStage.setTitle(
                Resources.getString("title")
        );

        this.primaryStage.setMaximized(true);

        initRootLayout();
    }

    /**
     * Initializes the application's main window and FXML controller.
     */
    private void initRootLayout() {
        try {
            final FXMLLoader loader = createFXMLLoader();

            final BorderPane rootLayout =
                    (BorderPane) loader.load();

            applyFontSettings(rootLayout);

            final Scene scene = new Scene(rootLayout);

            controller = loader.getController();
            controller.setStage(primaryStage);
            controller.setApp(this);

            scene.addEventHandler(
                    KeyEvent.KEY_PRESSED,
                    Shortcut.get(controller)
            );

            primaryStage.setScene(scene);

            loadApplicationIcon();

            progressListener = createProgressListener();

            primaryStage.show();

        } catch (final IOException e) {
            LogPrinter.print(e);
            LogPrinter.error(
                    Resources.getString("error_has_occurred")
            );
        } catch (final RuntimeException e) {
            LogPrinter.print(e);
            LogPrinter.error(
                    Resources.getString("error_has_occurred")
            );
        }
    }

    /**
     * Creates the FXML loader for the externally distributed FXML file.
     *
     * Keeping FXML outside the JAR is intentional so that users can modify
     * the application's layout without modifying Java code.
     */
    private FXMLLoader createFXMLLoader() throws IOException {
        final URL fxmlLocation =
                ResourceFiles.url(OVERVIEW_FXML);

        final FXMLLoader loader =
                new FXMLLoader(fxmlLocation);

        loader.setResources(Resources.getBundle());

        return loader;
    }

    /**
     * Applies user-configurable font settings to the root layout.
     */
    private void applyFontSettings(final BorderPane rootLayout) {
        final int fontSize =
                Preferences.getInteger("fontSize", 13);

        final String fontType =
                Preferences.getValue(
                        "fontType",
                        "Malgun Gothic"
                );

        final String style =
                "-fx-font: "
                        + fontSize
                        + "px \""
                        + fontType
                        + "\";";

        rootLayout.setStyle(style);
    }

    /**
     * Loads the application icon from the externally distributed resources
     * directory.
     */
    private void loadApplicationIcon() {
        try {
            final URL iconLocation =
                    ResourceFiles.url(APPLICATION_ICON);

            final Image icon =
                    new Image(iconLocation.toExternalForm());

            primaryStage.getIcons().add(icon);

        } catch (final Exception e) {
            LogPrinter.print(e);
            LogPrinter.error(
                    Resources.getString("error_icon_load")
            );
        }
    }

    /**
     * Creates the progress listener used by GraphicsEngine.
     *
     * Progress callbacks may originate from a worker thread, so every UI
     * update is explicitly dispatched to the JavaFX Application Thread.
     */
    private ProgressListener createProgressListener() {
        return new ProgressListener() {

            @Override
            public void onProgress(
                    final double progress,
                    final String message) {

                Platform.runLater(() -> {
                    if (progressWindow != null) {
                        progressWindow.updateProgress(
                                progress,
                                message
                        );
                    }
                });
            }

            @Override
            public void finished() {
                Platform.runLater(() -> {
                    if (progressWindow != null) {
                        progressWindow.hide();
                    }
                });
            }
        };
    }

    /**
     * Starts a GraphicsEngine operation.
     */
    public void load(
            final GraphicsEngine.Builder builder,
            final GraphicsEngine.Mode mode) {

        Objects.requireNonNull(builder);
        Objects.requireNonNull(mode);

        progressWindow =
                new ProgressWindow(primaryStage);

        builder.setProgressListener(progressListener);

        progressWindow.show();

        new Thread(
                builder
                        .setMode(mode)
                        .addListener(listener)
                        .build()
        ).start();
    }
}