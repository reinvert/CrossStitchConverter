package com.stitch.converter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.stitch.converter.model.StitchColor;

public class Preferences {
    private static final String CONFIG_FILE = "config.properties";
    private static final SortedMap<String, String> keyStore = new TreeMap<>();
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

    private static void load() throws IOException {
        Path configPath = Paths.get(CONFIG_FILE);
        try (BufferedReader bufferedReader = Files.newBufferedReader(configPath)) {
            keyStore.putAll(bufferedReader.lines()
                .map(line -> line.split("="))
                .filter(splitLine -> splitLine.length == 2)
                .collect(Collectors.toMap(
                    splitLine -> splitLine[0].trim(),
                    splitLine -> splitLine[1].trim()
                )));
        }
    }

    private static final Runnable storeAction = () -> {
        Path configPath = Paths.get(CONFIG_FILE);
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(configPath)) {
            keyStore.forEach((key, value) -> {
                try {
                    bufferedWriter.write(key + "=" + value + "\n");
                } catch (IOException e) {
                    LogPrinter.print(e);
                    LogPrinter.error(Resources.getString("write_failed", Resources.getString("setting_file")));
                }
            });
        } catch (IOException e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("write_failed", Resources.getString("setting_file")));
        }
    };

    public static void store() {
        executor.submit(storeAction);
    }

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

    public static SortedMap<String, String> getKeyStore() {
        return new TreeMap<>(keyStore); // Immutable copy
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

    public static String getValue(String key, String defaultValue) {
        return keyStore.computeIfAbsent(key, k -> defaultValue);
    }

    private static <T> T getOrDefault(String key, T defaultValue, java.util.function.Function<String, T> parser, java.util.function.BiConsumer<String, T> setter) {
        return Optional.ofNullable(keyStore.get(key))
            .map(parser)
            .orElseGet(() -> {
                setter.accept(key, defaultValue);
                return defaultValue;
            });
    }

    public static boolean setValue(String key, boolean value) {
        return setValue(key, Boolean.toString(value));
    }

    public static boolean setValue(String key, double value) {
        return setValue(key, String.format("%.4f", value));
    }

    public static boolean setValue(String key, int value) {
        return setValue(key, Integer.toString(value));
    }

    public static boolean setValue(String key, StitchColor value) {
        return setValue(key, colorToString(value));
    }

    public static boolean setValue(String key, String value) {
        keyStore.put(key, value);
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
