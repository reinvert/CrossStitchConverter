package com.stitch.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.NoSuchElementException;

import com.stitch.converter.model.StitchColor;

public class Preferences {
	private static HashMap<String, String> keyStore = new HashMap<>();
	private final static String directory = "config.properties";

	static {
		try {
			final File file = new File(directory);
			if (file.exists() == false) {
				file.createNewFile();
			}
			load();
		} catch (final IOException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
		}
	}

	private Preferences() {
		throw new AssertionError();
	}

	private static void load() {
		try (final FileReader fileReader = new FileReader(directory)) {
			try (final BufferedReader bufferedReader = new BufferedReader(fileReader)) {
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					try {
						final String[] splitLine = line.split("=");
						final String key = splitLine[0];
						final String value = splitLine[1];
						keyStore.put(key, value);
					} catch (final ArrayIndexOutOfBoundsException e) {
						continue;
					}
				}
			}
		} catch (final IOException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
		}
	}

	private static void store() {
		try (final FileWriter fileWriter = new FileWriter(directory)) {
			try (final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
				for (final String key : keyStore.keySet()) {
					bufferedWriter.write(key + "=" + keyStore.get(key) + "\n");
				}
				bufferedWriter.flush();
			}
		} catch (final IOException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
		}
	}

	@SuppressWarnings("unchecked")
	public static HashMap<String, String> getKeyStore() {
		return (HashMap<String, String>) keyStore.clone();
	}

	public static void setValue(final String key, final String value) {
		keyStore.put(key, value);
		store();
	}

	public static String getValue(final String key) throws NoSuchElementException {
		final String value = keyStore.get(key);
		if (value == null) {
			throw new NoSuchElementException();
		}
		return value;
	}

	public static String getValue(final String key, final String defaultValue) {
		try {
			return getValue(key);
		} catch (final NoSuchElementException e) {
			keyStore.put(key, defaultValue);
			store();
			return defaultValue;
		}
	}

	public static String getString(final String key) throws NoSuchElementException {
		return getValue(key);
	}

	public static String getString(final String key, final String defaultValue) {
		return getValue(key, defaultValue);
	}

	public static int getInteger(final String key) throws NoSuchElementException, NumberFormatException {
		final String value = keyStore.get(key);
		if (value == null) {
			throw new NoSuchElementException();
		}
		return Integer.parseInt(value);
	}

	public static int getInteger(final String key, final int defaultValue) {
		try {
			return getInteger(key);
		} catch (final NoSuchElementException | ClassCastException e) {
			keyStore.put(key, Integer.toString(defaultValue));
			store();
			return defaultValue;
		}
	}

	public static double getDouble(final String key) throws NoSuchElementException, NumberFormatException {
		final String value = keyStore.get(key);
		if (value == null) {
			throw new NoSuchElementException();
		}
		return Double.parseDouble(value);
	}

	public static double getDouble(final String key, final double defaultValue) {
		try {
			return getDouble(key);
		} catch (final NoSuchElementException | ClassCastException e) {
			keyStore.put(key, Double.toString(defaultValue));
			store();
			return defaultValue;
		}
	}

	public static boolean getBoolean(final String key) throws NoSuchElementException {
		final String value = keyStore.get(key);
		if (value == null) {
			throw new NoSuchElementException();
		}
		return Boolean.parseBoolean(value);
	}

	public static boolean getBoolean(final String key, final boolean defaultValue) {
		try {
			return getBoolean(key);
		} catch (final NoSuchElementException e) {
			keyStore.put(key, Boolean.toString(defaultValue));
			store();
			return defaultValue;
		}
	}

	public static StitchColor getColor(final String key) throws NoSuchElementException, ClassCastException {
		final String value = keyStore.get(key);
		if (value == null) {
			throw new NoSuchElementException();
		}
		return stringToColor(value);
	}

	public static StitchColor getColor(final String key, final StitchColor defaultValue) {
		try {
			return getColor(key);
		} catch (final NoSuchElementException | ClassCastException e) {
			keyStore.put(key, colorToString(defaultValue));
			store();
			return defaultValue;
		}
	}

	private static StitchColor stringToColor(final String colorCode) throws ClassCastException {
		int red, green, blue;
		try {
			red = Integer.valueOf(colorCode.substring(1, 3), 16);
			green = Integer.valueOf(colorCode.substring(3, 5), 16);
			blue = Integer.valueOf(colorCode.substring(5, 7), 16);
		} catch (final NumberFormatException | IndexOutOfBoundsException e) {
			throw new ClassCastException();
		}
		return new StitchColor(red, green, blue, colorCode);
	}

	private static String colorToString(final StitchColor color) {
		String colorCode = "#";
		colorCode += Integer.toHexString((int) color.getRed());
		colorCode += Integer.toHexString((int) color.getGreen());
		colorCode += Integer.toHexString((int) color.getBlue());
		return colorCode;
	}
}
