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

	private static File logFile = new File(Preferences.getString("logFile", "log.txt"));

	private static Logger logger = new Logger() {
		
		private Alert alert, error;
		
		@Override
		public void alert(final String content) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if(alert == null) {
						alert = new Alert(AlertType.INFORMATION);
						
						final Image icon = new Image("file:resources/icon/information.png");
						alert.setGraphic(new ImageView(icon));
						((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
						
						alert.setTitle(Resources.getString("information"));
					}
					alert.setContentText(content);
					alert.show();
				}
			});
		}

		@Override
		public void error(final String content) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if(error == null) {
						error = new Alert(AlertType.ERROR);
						
						final Image icon = new Image("file:resources/icon/error.png");
						error.setGraphic(new ImageView(icon));
						((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(icon);
						
						error.setTitle(Resources.getString("error"));
					}
					error.setContentText(content);
					error.show();
				}
			});
		}

		@Override
		public void print(final String str) {
			try {
				Resources.writeText(logFile, str);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void print(final Throwable throwable) {
			throwable.printStackTrace();
			try (final StringWriter stringWriter = new StringWriter()) {
				try (final PrintWriter printWriter = new PrintWriter(stringWriter)) {
					throwable.printStackTrace(printWriter);
					Resources.writeText(logFile, stringWriter.toString());
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	};

	public static void alert(final String content) {
		logger.alert(content);
	}

	public static void error(final String content) {
		logger.error(content);
	}

	public static void print(final String str) {
		logger.print(str);
	}

	public static void print(final Throwable throwable) {
		logger.print(throwable);
	}

	public static void setLogFile(final File file) {
		logFile = file;
	}

	public static void setPrinter(final Logger logger) {
		LogPrinter.logger = logger;
	}

	private LogPrinter() {
		throw new AssertionError("Singleton class should not be accessed by constructor.");
	}
}
