package com.stitch.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.stitch.converter.model.StitchColor;

public class Preferences {
    private static final String CONFIG_FILE = "config.properties";
    private static final SortedMap<String, String> keyStore = new TreeMap<>();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    static {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                Files.createFile(configPath);
            }
            load();
        } catch (IOException e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
        }
    }

    private static void load() throws IOException {
        Path configPath = Paths.get(CONFIG_FILE);
        try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
            bufferedReader.lines().forEach(line -> {
                try {
                    String[] splitLine = line.trim().split("=");
                    if (splitLine.length == 2) {
                        keyStore.put(splitLine[0].trim(), splitLine[1].trim());
                    }
                } catch (ArrayIndexOutOfBoundsException ignored) {
                }
            });
        }
    }

    private static final Runnable storeAction = () -> {
        Path configPath = Paths.get(CONFIG_FILE);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(configPath)) {
            for (var entry : keyStore.entrySet()) {
                bufferedWriter.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        } catch (IOException e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("write_failed", Resources.getString("setting_file")));
        }
    };

    public static void store() {
        executor.submit(storeAction);
    }

    // Shuts down the ExecutorService gracefully
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    public static boolean getBoolean(final String key) throws NoSuchElementException {
        return Boolean.parseBoolean(getValue(key).orElseThrow());
    }

    public static boolean getBoolean(final String key, final boolean defaultValue) {
        return getValue(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    public static StitchColor getColor(final String key) throws NoSuchElementException, ClassCastException {
        return stringToColor(getValue(key).orElseThrow());
    }

    public static StitchColor getColor(final String key, final StitchColor defaultValue) {
        return getValue(key).map(Preferences::stringToColor).orElse(defaultValue);
    }

    public static double getDouble(final String key) throws NoSuchElementException, NumberFormatException {
        return Double.parseDouble(getValue(key).orElseThrow());
    }

    public static double getDouble(final String key, final double defaultValue) {
        return getValue(key).map(Double::parseDouble).orElse(defaultValue);
    }

    public static int getInteger(final String key) throws NoSuchElementException, NumberFormatException {
        return Integer.parseInt(getValue(key).orElseThrow());
    }

    public static int getInteger(final String key, final int defaultValue) {
        return getValue(key).map(Integer::parseInt).orElse(defaultValue);
    }

    public static SortedMap<String, String> getKeyStore() {
        return new TreeMap<>(keyStore);  // Immutable
    }

    public static Optional<String> getValue(final String key) {
        return Optional.ofNullable(keyStore.get(key));
    }

    public static String getValue(final String key, final String defaultValue) {
        return getValue(key).orElse(defaultValue);
    }

    public static boolean setValue(final String key, final boolean value) {
        keyStore.put(key, Boolean.toString(value));
        return true;
    }

    public static boolean setValue(final String key, final double value) {
        keyStore.put(key, String.format("%.4f", value));
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
        try {
            int red = Integer.parseInt(colorCode.substring(1, 3), 16);
            int green = Integer.parseInt(colorCode.substring(3, 5), 16);
            int blue = Integer.parseInt(colorCode.substring(5, 7), 16);
            return new StitchColor(red, green, blue, colorCode);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ClassCastException();
        }
    }

    private static String colorToString(final StitchColor color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Preferences() {
        throw new AssertionError("Singleton class should not be accessed by constructor.");
    }
}
