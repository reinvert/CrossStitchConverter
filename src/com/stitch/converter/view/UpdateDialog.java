package com.stitch.converter.view;

import java.util.Optional;

import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;

import javafx.application.HostServices;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

final class UpdateDialog {

    private UpdateDialog() {
        throw new AssertionError("Utility class should not be instantiated.");
    }

    public static void show(final HostServices hostServices) {
        final ButtonType update =
                new ButtonType(Resources.getString("update"), ButtonData.YES);
        final ButtonType notUpdate =
                new ButtonType(Resources.getString("update_no"), ButtonData.NO);
        final ButtonType neverRemind =
                new ButtonType(
                        Resources.getString("update_never_remind"),
                        ButtonData.OTHER
                );

        final Alert alert = createAlert(update, notUpdate, neverRemind);
        final Optional<ButtonType> result = alert.showAndWait();

        result.ifPresent(buttonType -> {
            if (buttonType == update) {
                final String url =
                        Resources.getString("url") + "/releases";
                hostServices.showDocument(url);
            } else if (buttonType == neverRemind) {
                Preferences.setValue("updateNeverRemind", true);
            }
        });
    }

    private static Alert createAlert(
            final ButtonType update,
            final ButtonType notUpdate,
            final ButtonType neverRemind) {

        final Alert alert = new Alert(AlertType.INFORMATION);

        alert.getDialogPane()
                .getStylesheets()
                .add(Resources.getCSS());

        alert.setTitle(Resources.getString("information"));
        alert.setHeaderText(Resources.getString("update_header"));
        alert.setContentText(Resources.getString("update_content"));

        final Image icon = new Image("file:resources/icon/update.png");
        alert.setGraphic(new ImageView(icon));

        ((Stage) alert.getDialogPane()
                .getScene()
                .getWindow())
                .getIcons()
                .add(icon);

        alert.getButtonTypes().setAll(update, notUpdate, neverRemind);

        return alert;
    }
}