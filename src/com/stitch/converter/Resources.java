package com.stitch.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Resources {
	private static ResourceBundle bundle;
	private static final ArrayList<Locale> supportedLocales = new ArrayList<Locale>();

	static {
		supportedLocales.add(Locale.KOREAN);
		supportedLocales.add(Locale.ENGLISH);
		try {
			final String language = Locale.getDefault().getLanguage();
			for (final Locale locale : supportedLocales) {
				if (language.equals(locale.getLanguage())) {
					bundle = ResourceBundle.getBundle("Languages", locale);
					break;
				}
			}
		} catch (final MissingResourceException e) {
			bundle = ResourceBundle.getBundle("Languages", Locale.ENGLISH);
		} catch (final Throwable t) {
			try {
				try(final StringWriter stringWriter = new StringWriter()) {
					try(final PrintWriter printWriter = new PrintWriter(stringWriter)) {
						t.printStackTrace(printWriter);
						Resources.writeText(new File("log.txt"), stringWriter.toString());
					}
				}
			} catch (final Throwable t2) {
				t2.printStackTrace();
			}
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Error");
					alert.setContentText("Failed to read resource file.");
					alert.show();
					System.exit(0);
				}
			});
		}
	}

	public static ResourceBundle getBundle() {
		return bundle;
	}

	public static String getString(final String id) throws MissingResourceException {
		try {
			return bundle.getString(id);
		} catch(final NullPointerException | ClassCastException e) {
			LogPrinter.print(e);
			LogPrinter.error(new StringBuilder("Exception on read resources from id: ").append(id).toString());
			return null;
		} catch(MissingResourceException e) {
			throw e;
		}
	}

	public static String getString(final String id, final Object... args) {
		try {
			return String.format(getString(id), args);
		} catch (final NullPointerException | MissingResourceException | IllegalFormatException e) {
			LogPrinter.print(e);
			LogPrinter.error(new StringBuilder("Exception on read resources from id: ").append(id).append(", args: ").append(args).toString());
			return null;
		}
	}

	public static Object readObject(final File file) throws IOException, ClassNotFoundException {
		try (final FileInputStream fis = new FileInputStream(file)) {
			try (final ObjectInputStream ois = new ObjectInputStream(fis)) {
				return ois.readObject();
			}
		}
	}

	public static void writeObject(final File file, final Object object) throws FileNotFoundException, IOException {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try (final FileOutputStream fos = new FileOutputStream(file)) {
					try (final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
						oos.writeObject(object);
					}
				} catch(final Exception e) {
					LogPrinter.print(e);
				}
			}
		}).start();;
		
	}

	public static boolean writeText(final File file, final String text) throws IOException {
		try (final PrintWriter printWriter = new PrintWriter(file)) {
			printWriter.println(text);
			return true;
		}
	}

	private Resources() {
		throw new AssertionError("Singleton class should not be accessed by constructor.");
	}
}
