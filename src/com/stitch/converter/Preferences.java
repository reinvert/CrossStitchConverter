package com.stitch.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

import com.stitch.converter.model.StitchColor;

public class Preferences {
	private final static String directory = "config.properties";
	private static SortedMap<String, String> keyStore = new TreeMap<>();

	static {
		try {
			final File file = new File(directory);
			if (file.exists() == false) {
				file.createNewFile();
			}
			load();
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
		}
	}
	
	private static boolean load() throws IOException {
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
		}
		return true;
	}
	
	private static Runnable storeAction = new Runnable() {
		@Override
		public void run() {
			try (final FileWriter fileWriter = new FileWriter(directory)) {
				try (final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
					for (final String key : keyStore.keySet()) {
						bufferedWriter.write(new StringBuilder(key).append("=").append(keyStore.get(key)).append("\n").toString());
					}
					bufferedWriter.flush();
				}
			} catch (final IOException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
			}
		}
	};

	public static void store() {
		new Thread(storeAction).start();
	}


	public static boolean getBoolean(final String key) throws NoSuchElementException {
		return Boolean.parseBoolean(getValue(key));
	}

	public static boolean getBoolean(final String key, final boolean defaultValue) {
		try {
			return getBoolean(key);
		} catch (final NoSuchElementException e) {
			setValue(key, defaultValue);
			return defaultValue;
		}
	}

	public static StitchColor getColor(final String key) throws NoSuchElementException, ClassCastException {
		return stringToColor(getValue(key));
	}

	public static StitchColor getColor(final String key, final StitchColor defaultValue) {
		try {
			return getColor(key);
		} catch (final NoSuchElementException | ClassCastException e) {
			setValue(key, defaultValue);
			return defaultValue;
		}
	}

	public static double getDouble(final String key) throws NoSuchElementException, NumberFormatException {
		return Double.parseDouble(getValue(key));
	}

	public static double getDouble(final String key, final double defaultValue) {
		try {
			return getDouble(key);
		} catch (final NoSuchElementException | NumberFormatException e) {
			setValue(key, defaultValue);
			return defaultValue;
		}
	}

	public static int getInteger(final String key) throws NoSuchElementException, NumberFormatException {
		return Integer.parseInt(getValue(key));
	}

	public static int getInteger(final String key, final int defaultValue) {
		try {
			return getInteger(key);
		} catch (final NoSuchElementException | NumberFormatException e) {
			setValue(key, defaultValue);
			return defaultValue;
		}
	}

	public static SortedMap<String, String> getKeyStore() {
		final SortedMap<String, String> output = new TreeMap<>();
		for(final String key:keyStore.keySet()) {
			output.put(key, keyStore.get(key));
		}
		return output;
	}

	public static String getString(final String key) throws NoSuchElementException {
		return getValue(key);
	}

	public static String getString(final String key, final String defaultValue) {
		return getValue(key, defaultValue);
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
			setValue(key, defaultValue);
			return defaultValue;
		}
	}
	
	public static boolean setValue(final String key, final boolean value) {
		keyStore.put(key, Boolean.toString(value));
		return true;
	}

	public static boolean setValue(final String key, final double value) {
		keyStore.put(key, String.format("%.4f",value));
		return true;
	}

	public static boolean setValue(final String key, final int value) {
		keyStore.put(key, Integer.toString(value));
		return true;
	}

	public static boolean setValue(final String key, final StitchColor value) {
		keyStore.put(key, colorToString(value));
		return true;
	}

	public static boolean setValue(final String key, final String value) {
		keyStore.put(key, value);
		return true;
	}

	private static StitchColor stringToColor(final String colorCode) throws ClassCastException {
		final int red, green, blue;
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
		return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
	}

	private Preferences() {
		throw new AssertionError("Singleton class should not be accessed by constructor.");
	}
}
