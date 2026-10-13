package com.stitch.converter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Optional;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

public final class UpdateChecker {

    private static final String TAG_NAME_KEY = "tag_name";
    private static final String VERSION_KEY = "version";
    
    private UpdateChecker() {
    	throw new AssertionError("Singleton class should not be instantized.");
    }
    
    public static boolean hasUpdate() {
        try {
            final URL updateUrl = getUpdateURL();
            final Optional<Integer> latestVersion =
                    fetchLatestVersion(updateUrl);

            if (latestVersion.isPresent()) {
                final int currentVersion =
                        Integer.parseInt(Resources.getString(VERSION_KEY));

                return latestVersion.get() > currentVersion;
            }
        } catch (Exception e) {
            LogPrinter.print(e);
        }

        return false;
    }
    
    private static URL getUpdateURL() throws Exception {
        URI uri = new URI(Resources.getString("update_url"));
        return uri.toURL();
    }

    // Fetches the latest version from the server and parses the response
    private static Optional<Integer> fetchLatestVersion(URL url) {
        HttpURLConnection httpUrlConnection = null;
        try {
            httpUrlConnection = (HttpURLConnection) url.openConnection();
            httpUrlConnection.setRequestMethod("GET");
            httpUrlConnection.setConnectTimeout(1000);  // Set timeout for the connection
            httpUrlConnection.setReadTimeout(1000);

            if (httpUrlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (InputStreamReader inputStreamReader = new InputStreamReader(httpUrlConnection.getInputStream());
                     BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    String jsonResponse = readResponse(bufferedReader);
                    return parseVersionFromResponse(jsonResponse);
                }
            } else {
                System.err.println("Failed to fetch update, HTTP code: " + httpUrlConnection.getResponseCode());
            }
        } catch (final Exception e) {
        	LogPrinter.print(e);
        } finally {
            if (httpUrlConnection != null) {
                httpUrlConnection.disconnect();
            }
        }
        return Optional.empty();
    }

    // Reads the full response from the BufferedReader
    private static String readResponse(BufferedReader bufferedReader) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line).append("\n");
        }
        return stringBuilder.toString();
    }

    // Parses the version number from the JSON response
    private static Optional<Integer> parseVersionFromResponse(
            final String jsonResponse) {
        try {
            final Object response = Jsoner.deserialize(jsonResponse);

            if (!(response instanceof JsonObject)) {
                throw new IllegalArgumentException(
                        "Update response is not a JSON object."
                );
            }

            final JsonObject jsonObject = (JsonObject) response;
            final Object tagName = jsonObject.get(TAG_NAME_KEY);

            if (tagName == null) {
                throw new IllegalArgumentException(
                        "Update response does not contain "
                                + TAG_NAME_KEY + "."
                );
            }

            return Optional.of(
                    Integer.parseInt(tagName.toString())
            );
        } catch (final JsonException | IllegalArgumentException e) {
            LogPrinter.print(e);
            return Optional.empty();
        }
    }
}