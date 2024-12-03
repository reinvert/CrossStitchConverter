package com.stitch.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;
import com.stitch.converter.model.StitchImage;

import javafx.scene.paint.Color;

public final class GraphicsEngine implements Runnable{

    public static final int FLOYD = 0, SIERRA = 1;

    public static class Builder {
        private StitchColor backgroundColor = new StitchColor(Color.WHITE, null);
        private int colorLimit = 0;
        private int threadCount;
        private int convertMode = Preferences.getInteger("convertMode", 0);
        private boolean isGammaBased = Preferences.getBoolean("isGammaBased", true);
        private ProgressListener progressListener;

        private final File csvFile;
        private final File imageFile;
        private final List<Listener> listeners = new ArrayList<>();
        private Mode loadMode = Mode.NEW_FILE;
        private boolean scaled = Preferences.getBoolean("resizeImage", true);

        public Builder(final File csvFile, final File imageFile) {
            this.csvFile = csvFile;
            this.imageFile = imageFile;
            this.threadCount = Runtime.getRuntime().availableProcessors() + 1;
        }

        public GraphicsEngine build() {
            return new GraphicsEngine(this);
        }

        public Builder setBackground(final StitchColor backgroundColor) {
            this.backgroundColor = backgroundColor;
            return this;
        }

        public Builder setColorLimit(final int colorLimit) {
            this.colorLimit = colorLimit;
            return this;
        }

        public Builder addListener(final Listener listener) {
            listeners.add(listener);
            return this;
        }

        public Builder setMode(final Mode loadMode) {
            this.loadMode = loadMode;
            return this;
        }

        public Builder setScaled(final boolean scaled) {
            this.scaled = scaled;
            return this;
        }

        public Builder setThreadCount(final int threadCount) {
            if (threadCount > 0) {
                this.threadCount = threadCount;
            }
            return this;
        }

        public Builder setGammaBased(final boolean isGammaBased) {
            this.isGammaBased = isGammaBased;
            return this;
        }

        public Builder setConvertMode(final int convertMode) {
            if (convertMode == FLOYD || convertMode == SIERRA) {
                this.convertMode = convertMode;
            }
            return this;
        }

        public Builder setProgressListener(final ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }
    }

    public enum Mode {
        LOAD, NEW_FILE
    }

    private final StitchColor backgroundColor;
    private final int colorLimit;
    private final int threadCount;
    private final int convertMode;
    private final File csvFile;
    private final File imageFile;
    private final List<Listener> listeners;
    private final Mode loadMode;
    private final boolean scaled;
    private final boolean isGammaBased;
    private final ProgressListener progressListener;

    private GraphicsEngine(final Builder builder) {
        this.csvFile = builder.csvFile;
        this.imageFile = builder.imageFile;
        this.colorLimit = builder.colorLimit;
        this.scaled = builder.scaled;
        this.backgroundColor = builder.backgroundColor;
        this.loadMode = builder.loadMode;
        this.threadCount = builder.threadCount;
        this.listeners = builder.listeners;
        this.isGammaBased = builder.isGammaBased;
        this.convertMode = builder.convertMode;
        this.progressListener = builder.progressListener;
    }

    @Override
    public void run() {
        try {
            if (loadMode == Mode.NEW_FILE) {
                processNewFile(imageFile);
            } else if (loadMode == Mode.LOAD) {
                loadFromSavedFile(imageFile);
            }
        } catch (Exception e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("cant_read_image"));
        } finally {
        	progressListener.finished();
        }
    }

    private void processNewFile(final File file) throws IOException {
        List<StitchColor> colorList;
        StitchImage stitchImage;
        BufferedImage image;
        try (CSVReader csvReader = new CSVReader(new FileReader(csvFile))) {
            colorList = readColorList(csvReader.readAll());
        } catch (CsvException | NoSuchElementException | IllegalArgumentException | NullPointerException e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("read_failed", Resources.getString("color_table")));
            return;
        }
        
        try {
            stitchImage = new StitchImage();
            stitchImage.setChanged(true);
            image = scaled ? ImageTools.readImage(file, Preferences.getInteger("resizeLength", 200), Preferences.getInteger("resizeLength", 200)) : ImageTools.readImage(file);
        } catch (NullPointerException e) {
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("read_failed", file.getName()));
            return;
        }
        
        boolean firstRun = true;
        StitchColor colorToRemove = null;
        Map<String, Integer> usedColorCount = new HashMap<>();

        do {
            stitchImage.setSize(image.getWidth(), image.getHeight());
            if (!firstRun) {
                removeColor(colorList, colorToRemove);
            }
            usedColorCount.clear();

            ColorConverter converter = createColorConverter(image, stitchImage, colorList);
            Thread converterThread = new Thread(converter);
            converterThread.setDaemon(true);
            converterThread.start();
            try {
                converterThread.join();
            } catch (InterruptedException e) {
                LogPrinter.print(e);
                LogPrinter.error(Resources.getString("error_has_occurred"));
                return;
            }

            stitchImage.setBackground(backgroundColor);
            for (PixelList pixelList : stitchImage.getPixelLists()) {
                usedColorCount.put(pixelList.getColor().getName(), pixelList.getCount());
            }
            firstRun = false;
            colorToRemove = ImageTools.calculateRemoveString(stitchImage, usedColorCount);

        } while (0 < colorLimit && colorLimit < stitchImage.getPixelLists().size());

        stitchImage.setChanged(true);
        notifyListeners(stitchImage);
        progressListener.onProgress(1.0, Resources.getString("conversion_completed"));
    }

    private void loadFromSavedFile(final File file) throws IOException, ClassNotFoundException {
        StitchImage stitchImage = (StitchImage) Resources.readObject(file);
        notifyListeners(stitchImage);
    }

    private void notifyListeners(final StitchImage image) {
        for (Listener listener : listeners) {
            listener.onFinished(image);
        }
    }

    private ColorConverter createColorConverter(BufferedImage image, StitchImage stitchImage, List<StitchColor> colorList) {
        ColorConverter.Builder builder = new ColorConverter.Builder(image, stitchImage, colorList)
                .setThreadCount(threadCount)
                .setGammaBased(isGammaBased)
                .setConvertMode(convertMode)
                .setProgressListener(progressListener);

        return Preferences.getBoolean("isDither", true) ? new DitheredColorConverter(builder) : builder.build();
    }
    private static void removeColor(final Collection<StitchColor> colorList, final StitchColor toRemove) {
        colorList.removeIf(color -> color.equals(toRemove));
    }

    private static List<StitchColor> readColorList(final List<String[]> csv) {
        List<StitchColor> colors = new ArrayList<>();
        int lineNumber = 0;
        try {
            for (String[] row : csv) {
                String name = row[0];
                int red = Integer.parseInt(row[1]);
                int green = Integer.parseInt(row[2]);
                int blue = Integer.parseInt(row[3]);
                colors.add(new StitchColor(red, green, blue, name));
                lineNumber++;
            }
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Error at line " + (lineNumber + 1), e);
        }
        return colors;
    }
}
