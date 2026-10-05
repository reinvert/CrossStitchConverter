package com.stitch.converter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public final class LogPrinter {

	private static File logFile = new File(Preferences.getValue("logFile", "log.txt"));
	private static Alert alert, error;
    
    public static void installDefaultExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            LogPrinter.print(throwable);
            LogPrinter.error(throwable.getMessage());
        });
    }
    
    private static Alert createAlert(
            final AlertType alertType,
            final String iconPath,
            final String title) {

        final Alert alert = new Alert(alertType);
        final Image icon = new Image(iconPath);

        alert.setGraphic(new ImageView(icon));
        ((Stage) alert.getDialogPane().getScene().getWindow())
            .getIcons()
            .add(icon);
        alert.setTitle(title);

        return alert;
    }

    public static void alert(final String content) {
        Platform.runLater(() -> {
            if (alert == null) {
                alert = createAlert(
                    AlertType.INFORMATION,
                    "file:resources/icon/information.png",
                    Resources.getString("information")
                );
            }
            alert.setContentText(content);
            alert.show();
        });
    }

    public static void error(final String content) {
        Platform.runLater(() -> {
            if (error == null) {
                error = createAlert(
                    AlertType.ERROR,
                    "file:resources/icon/error.png",
                    Resources.getString("error")
                );
            }
            error.setContentText(content);
            error.show();
        });
    }

    public static void print(final String str) {
        try {
            Resources.appendText(logFile, str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void print(final Throwable throwable) {
        try (
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter)
        ) {
            throwable.printStackTrace(printWriter);

            final String stackTrace = stringWriter.toString();

            System.err.print(stackTrace);
            Resources.appendText(logFile, stackTrace);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setLogFile(final File file) {
        logFile = file;
    }
    
    private LogPrinter() {
        throw new AssertionError("Singleton class should not be accessed by constructor.");
    }
}
