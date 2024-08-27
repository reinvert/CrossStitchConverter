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

	public interface Logger {
		public void alert(final String str);

		public void error(final String str);

		public void print(final String str);

		public void print(final Throwable throwable);
	}

	private static File logFile = new File(Preferences.getValue("logFile", "log.txt"));

	private static Logger logger = new Logger() {
		
		private Alert alert, error;
		
		@Override
		public void alert(final String content) {
			Platform.runLater(() -> {
                if (alert == null) {
                    alert = createAlert(AlertType.INFORMATION, "file:resources/icon/information.png", Resources.getString("information"));
                }
                alert.setContentText(content);
                alert.show();
			});
		}

        @Override
        public void error(String content) {
            Platform.runLater(() -> {
                if (error == null) {
                    error = createAlert(AlertType.ERROR, "file:resources/icon/error.png", Resources.getString("error"));
                }
                error.setContentText(content);
                error.show();
            });
        }

        @Override
        public void print(String str) {
            try {
                Resources.writeText(logFile, str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void print(Throwable throwable) {
            throwable.printStackTrace();
            try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
                throwable.printStackTrace(printWriter);
                Resources.writeText(logFile, stringWriter.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private Alert createAlert(AlertType alertType, String iconPath, String title) {
            Alert alert = new Alert(alertType);
            Image icon = new Image(iconPath);
            alert.setGraphic(new ImageView(icon));
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
            alert.setTitle(title);
            return alert;
        }
    };

    public static void alert(String content) {
        logger.alert(content);
    }

    public static void error(String content) {
        logger.error(content);
    }

    public static void print(String str) {
        logger.print(str);
    }

    public static void print(Throwable throwable) {
        logger.print(throwable);
    }

    public static void setLogFile(File file) {
        logFile = file;
    }

    public static void setPrinter(Logger newLogger) {
        throw new UnsupportedOperationException("Logger cannot be replaced at runtime");
    }

    private LogPrinter() {
        throw new AssertionError("Singleton class should not be accessed by constructor.");
    }
}
