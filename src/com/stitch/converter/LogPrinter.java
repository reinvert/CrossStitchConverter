package com.stitch.converter;

import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class LogPrinter {

	private static String logFile = "log.txt";

	private static Logger logger = new Logger() {
		@Override
		public void print(final String str) {
			try {
				Resources.writeText(logFile, str);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void alert(final String content) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle(Resources.getString("information"));
					alert.setContentText(content);
					alert.showAndWait();
				}
			});
		}

		@Override
		public void error(final String content) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle(Resources.getString("error"));
					alert.setContentText(content);
					alert.showAndWait();
				}
			});
			
		}
	};

	private LogPrinter() {
		throw new AssertionError();
	}

	public static void setPrinter(Logger logger) {
		LogPrinter.logger = logger;
	}

	public static void setLogFile(String fileName) {
		logFile = fileName;
	}

	public static void print(String str) {
		logger.print(str);
	}

	public static void alert(final String content) {
		logger.alert(content);
	}

	public static void error(final String content) {
		logger.error(content);
	}

	public interface Logger {
		public void print(final String str);

		public void alert(final String str);

		public void error(final String str);
	}
}
