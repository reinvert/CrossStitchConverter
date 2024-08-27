package com.stitch.converter;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Resources {
    private static final ResourceBundle bundle;
    private static final List<Locale> supportedLocales = Collections.unmodifiableList(Arrays.asList(Locale.KOREAN, Locale.ENGLISH));

    static {
        bundle = initializeResourceBundle();
    }

    private static ResourceBundle initializeResourceBundle() {
        try {
            Locale defaultLocale = Locale.getDefault();
            for (Locale locale : supportedLocales) {
                if (locale.getLanguage().equals(defaultLocale.getLanguage())) {
                    return ResourceBundle.getBundle("Languages", locale);
                }
            }
            // Fallback to English if locale not supported
            return ResourceBundle.getBundle("Languages", Locale.ENGLISH);
        } catch (MissingResourceException e) {
            logAndShowError("Failed to read resource file.", e);
            return ResourceBundle.getBundle("Languages", Locale.ENGLISH);
        } catch (Throwable t) {
            logAndShowError("Unexpected error while loading resources.", t);
            return ResourceBundle.getBundle("Languages", Locale.ENGLISH);
        }
    }

    private static void logAndShowError(String message, Throwable t) {
        logException(t);
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.show();
        });
        // Allow application to handle error instead of forcing exit
    }

    private static void logException(Throwable t) {
        try (StringWriter stringWriter = new StringWriter(); PrintWriter printWriter = new PrintWriter(stringWriter)) {
            t.printStackTrace(printWriter);
            writeText(new File("log.txt"), stringWriter.toString());
        } catch (IOException e) {
            e.printStackTrace(); // Fallback to standard logging
        }
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static String getString(final String id) throws MissingResourceException {
        try {
            return bundle.getString(id);
        } catch (NullPointerException | ClassCastException | MissingResourceException e) {
            LogPrinter.print(e);
            LogPrinter.error("Exception on reading resource for id: " + id);
            return null;
        }
    }

    public static String getString(final String id, final Object... args) {
        try {
            return String.format(getString(id), args);
        } catch (NullPointerException | IllegalFormatException | MissingResourceException e) {
            LogPrinter.print(e);
            LogPrinter.error("Exception on reading resource for id: " + id + ", args: " + Arrays.toString(args));
            return null;
        }
    }

    public static Object readObject(final File file) throws IOException, ClassNotFoundException {
        try (FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
            return ois.readObject();
        }
    }

    public static void writeObject(final File file, final Object object) {
        CompletableFuture.runAsync(() -> {
            try (FileOutputStream fos = new FileOutputStream(file); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                oos.writeObject(object);
            } catch (IOException e) {
                LogPrinter.print(e);
            }
        });
    }

    public static boolean writeText(final File file, final String text) throws IOException {
        try (PrintWriter printWriter = new PrintWriter(file)) {
            printWriter.println(text);
            return true;
        }
    }

    // Prevent instantiation
    private Resources() {
        throw new AssertionError("Utility class should not be instantiated.");
    }
}
