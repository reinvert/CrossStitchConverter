package com.stitch.converter.view;

import com.stitch.converter.Resources;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressWindow {

    private final Stage stage;
    private final ProgressBar progressBar;
    private final Label progressLabel;

    public ProgressWindow(Stage owner) {
        stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(Resources.getString("conversion_progress"));

        progressBar = new ProgressBar();
        progressLabel = new Label(Resources.getString("conversion_starting"));

        VBox layout = new VBox(10, progressBar, progressLabel);
        layout.setStyle(
                "-fx-padding: 10; " +
                "-fx-alignment: center; " +
                "-fx-border-color: gray; " +        // Outline color
                "-fx-border-width: 1;"               // Outline width
            );
        Scene scene = new Scene(layout);

        stage.setScene(scene);
        stage.setWidth(300);
        stage.setHeight(150);
    }

    public void show() {
        Platform.runLater(stage::show);
    }

    public void hide() {
        Platform.runLater(stage::hide);
    }

    public void updateProgress(double progress, String message) {
        Platform.runLater(() -> {
            progressBar.setProgress(progress);
            progressLabel.setText(message);
        });
    }
}
