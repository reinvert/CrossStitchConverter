package com.stitch.converter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Provides access to resources distributed outside the application JAR.
 *
 * The application intentionally keeps user-editable resources outside the JAR
 * so that users can customize files such as FXML without modifying Java code.
 */
public final class ResourceFiles {

    private static final Path ROOT = Paths.get("resources");

    private ResourceFiles() {
        // Utility class.
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
}