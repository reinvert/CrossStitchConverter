package com.stitch.converter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Resources {
	private static ResourceBundle bundle;
	private static final ArrayList<Locale> supportedLocales = new ArrayList<Locale>();
	
	static {
		supportedLocales.add(Locale.KOREAN);
		supportedLocales.add(Locale.ENGLISH);
		try {
			for (final Locale locale : supportedLocales) {
				if (Locale.getDefault().getLanguage().equals(locale.getLanguage())) {
					bundle = ResourceBundle.getBundle("Languages", locale);
				}
			}
		} catch(MissingResourceException e) {
			bundle = ResourceBundle.getBundle("Languages", Locale.ENGLISH);
		} catch(Throwable t) {
			try {
				writeText("log.txt", t.getMessage());
			} catch(Throwable t2) {
				t2.printStackTrace();
			}
			Alert alert = new Alert(AlertType.INFORMATION);
			alert.setTitle("Error");
			alert.setContentText("Failed to read resource file.");
			alert = new Alert(AlertType.ERROR);
			alert.showAndWait();
			System.exit(0);
		}
	}

	private Resources() {
		throw new AssertionError();
	}

	public static ResourceBundle getBundle() {
		return bundle;
	}

	public static String getString(final String id) throws MissingResourceException{
		try {
			return bundle.getString(id);
		} catch(final MissingResourceException e) {
			throw e;
		}
	}

	public static String getString(final String id, final Object... args) {
		try {
			return String.format(getString(id), args);
		} catch (final MissingFormatArgumentException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error("Exception on read resources from id : " + id + args);
			return null;
		}
	}
	
	public static void writeText(final String dir, final String text) throws IOException {
		try(final PrintWriter printWriter = new PrintWriter(dir)) {
			printWriter.println(text);
		}
	}
	
	public static Object readObject(final String dir) throws IOException, ClassNotFoundException {
		try(final FileInputStream fis = new FileInputStream(dir)) {
			try(final ObjectInputStream ois  = new ObjectInputStream(fis);) {
				return ois.readObject();
			}
		}
	}
	
	public static Object readObject(final File file) throws IOException, ClassNotFoundException {
		try(final FileInputStream fis = new FileInputStream(file)) {
			try(final ObjectInputStream ois  = new ObjectInputStream(fis);) {
				return ois.readObject();
			}
		}
	}
	
	public static void writeObject(final File file, final Object object) throws FileNotFoundException, IOException {
		try (FileOutputStream fos = new FileOutputStream(file)) {
			try (ObjectOutputStream oos = new ObjectOutputStream(fos)) {
				oos.writeObject(object);
			}
		}
	}
}
