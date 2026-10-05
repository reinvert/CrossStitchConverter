package com.stitch.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.stitch.converter.model.StitchColor;

public class Preferences {
    private static final String CONFIG_FILE = "config.properties";
    private static final Properties properties = new Properties();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    static {
        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.notExists(configPath)) {
                Files.createFile(configPath);
            }
            load();
        } catch (IOException e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("read_failed", Resources.getString("setting_file")));
        }
    }
    
    public static Set<String> getKeys() {
        return properties.stringPropertyNames();
    }

    private static void load() throws IOException {
        Path configPath = Paths.get(CONFIG_FILE);

        try (BufferedReader bufferedReader =
                 Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            properties.load(bufferedReader);
        }
    }

    private static final Runnable storeAction = () -> {
        Path configPath = Paths.get(CONFIG_FILE);

        try (BufferedWriter writer =
                 Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
            properties.store(writer, null);
        } catch (IOException e) {
            LogPrinter.print(e);
            LogPrinter.error(
                Resources.getString(
                    "write_failed",
                    Resources.getString("setting_file")
                )
            );
        }
    };

    public static void store() {
        executor.submit(storeAction);
    }

    public static void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return getOrDefault(key, defaultValue, Boolean::parseBoolean, Preferences::setValue);
    }

    public static StitchColor getColor(String key, StitchColor defaultValue) {
        return getOrDefault(key, defaultValue, Preferences::stringToColor, Preferences::setValue);
    }

    public static double getDouble(String key, double defaultValue) {
        return getOrDefault(key, defaultValue, Double::parseDouble, Preferences::setValue);
    }

    public static int getInteger(String key, int defaultValue) {
        return getOrDefault(key, defaultValue, Integer::parseInt, Preferences::setValue);
    }

    public static String getValue(final String key, final String defaultValue) {
        final String value = properties.getProperty(key);

        if (value == null) {
            setValue(key, defaultValue);
            return defaultValue;
        }

        return value;
    }

    private static <T> T getOrDefault(
            String key,
            T defaultValue,
            java.util.function.Function<String, T> parser,
            java.util.function.BiConsumer<String, T> setter) {

        final String value = properties.getProperty(key);

        if (value == null) {
            setter.accept(key, defaultValue);
            return defaultValue;
        }

        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            LogPrinter.print(e);
            setter.accept(key, defaultValue);
            return defaultValue;
        }
    }

    public static boolean setValue(String key, boolean value) {
        return setValue(key, Boolean.toString(value));
    }

    public static boolean setValue(String key, double value) {
        return setValue(key, String.format(Locale.ROOT, "%.4f", value));
    }

    public static boolean setValue(String key, int value) {
        return setValue(key, Integer.toString(value));
    }

    public static boolean setValue(String key, StitchColor value) {
        return setValue(key, colorToString(value));
    }

    public static boolean setValue(String key, String value) {
    	properties.setProperty(key, value);
        store();
        return true;
    }

    private static StitchColor stringToColor(String colorCode) throws ClassCastException {
        try {
            int red = Integer.parseInt(colorCode.substring(1, 3), 16);
            int green = Integer.parseInt(colorCode.substring(3, 5), 16);
            int blue = Integer.parseInt(colorCode.substring(5, 7), 16);
            return new StitchColor(red, green, blue, colorCode);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            throw new ClassCastException();
        }
    }

    private static String colorToString(StitchColor color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Preferences() {
        throw new AssertionError("Singleton class should not be accessed by constructor.");
    }
}
