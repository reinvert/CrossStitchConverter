package com.stitch.converter;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

public class Resources {
    private static final ResourceBundle bundle;
    private static final List<Locale> supportedLocales = Collections.unmodifiableList(Arrays.asList(Locale.KOREAN, Locale.ENGLISH));
    private static final Path ROOT = Paths.get("resources");

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

    private static void logException(final Throwable throwable) {
        try (
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter)
        ) {
            throwable.printStackTrace(printWriter);
            appendText(new File("log.txt"), stringWriter.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    public static String getString(final String id) throws NullPointerException, ClassCastException, MissingResourceException {
        return bundle.getString(id);
    }

    public static String getString(final String id, final Object... args) throws NullPointerException, ClassCastException, MissingResourceException {
        return String.format(getString(id), args);
    }

    public static Object readObject(final File file)
            throws IOException, ClassNotFoundException {

        try (ObjectInputStream ois =
                 new ObjectInputStream(new FileInputStream(file))) {
            return ois.readObject();
        }
    }

    public static boolean writeObject(final File file, final Object object) {
        try (
            ObjectOutputStream oos =
                new ObjectOutputStream(new FileOutputStream(file))
        ) {
            oos.writeObject(object);
            return true;
        } catch (IOException e) {
            LogPrinter.print(e);
            return false;
        }
    }
    
    public static synchronized boolean appendText(
            final File file, final String text) throws IOException {

        try (
            PrintWriter printWriter = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file, true),
                    StandardCharsets.UTF_8
                )
            )
        ) {
            printWriter.println(text);
            return true;
        }
    }
    
    private static String css, style;

    public static String getCSS() {
    	if (css == null) {
			css = path("Style.css")
			        .toUri()
			        .toString();
    	}
    	return css;
    }

    public static String getStyle() {
    	if (style == null) {
    		int fontSize = Preferences.getInteger("fontSize", 13);
            String fontType = Preferences.getValue("fontType", "Malgun Gothic");
            style = String.format("-fx-font: %dpx \"%s\";", fontSize, fontType);
    	}
    	return style;
    }
    
    /**
     * Returns the path of an external resource.
     *
     * @param relativePath path relative to the resources directory
     * @return external resource path
     */
    public static Path path(final String relativePath) {
        return ROOT.resolve(relativePath);
    }

    /**
     * Returns the URL of an external resource.
     *
     * @param relativePath path relative to the resources directory
     * @return resource URL
     * @throws IOException if the resource URL cannot be created
     */
    public static URL url(final String relativePath) throws IOException {
        return path(relativePath).toUri().toURL();
    }

    /**
     * Opens an external resource as an input stream.
     *
     * @param relativePath path relative to the resources directory
     * @return resource input stream
     * @throws IOException if the resource does not exist or cannot be opened
     */
    public static InputStream open(final String relativePath)
            throws IOException {

        return Files.newInputStream(path(relativePath));
    }

    /**
     * Checks whether an external resource exists.
     *
     * @param relativePath path relative to the resources directory
     * @return true when the resource exists
     */
    public static boolean exists(final String relativePath) {
        return Files.isRegularFile(path(relativePath));
    }
    
    // Prevent instantiation
    private Resources() {
        throw new AssertionError("Utility class should not be instantiated.");
    }
}
