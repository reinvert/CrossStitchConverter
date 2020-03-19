package com.stitch.converter;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public final class LogPrinter {

	public interface Logger {
		public void alert(final String str);

		public void error(final String str);

		public void print(final String str);

		public void print(final Throwable throwable);
	}

	private static String logFile = "log.txt";

	private static Logger logger = new Logger() {
		@Override
		public void alert(final String content) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					final Alert alert = new Alert(AlertType.INFORMATION);
					alert.setTitle(Resources.getString("information"));
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
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle(Resources.getString("error"));
					alert.setContentText(content);
					alert.show();
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

	public static void setLogFile(final String fileName) {
		logFile = fileName;
	}

	public static void setPrinter(final Logger logger) {
		LogPrinter.logger = logger;
	}

	private LogPrinter() {
		throw new AssertionError("Singleton class should not be accessed by constructor.");
	}
}
