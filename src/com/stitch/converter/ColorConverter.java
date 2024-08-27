package com.stitch.converter;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.stitch.converter.model.*;

class ColorConverter implements Runnable {
    static class Builder {
        private final Collection<StitchColor> colorList;
        private final BufferedImage image;
        private final StitchImage stitchImage;
        private int thread = Runtime.getRuntime().availableProcessors() + 1;
        private ProgressListener progressListener = new ProgressListener() {
			@Override
			public void onProgress(double progress, String message) {
				
			}
        };
        int convertMode = GraphicsEngine.FLOYD;
        boolean isGammaBased = true;

        Builder(final BufferedImage image, final StitchImage stitchImage, final Collection<StitchColor> colorList) {
            this.image = image;
            this.stitchImage = stitchImage;
            this.colorList = colorList;
        }

        ColorConverter build() {
            return new ColorConverter(this);
        }

        Builder setThreadCount(final int thread) {
            if (thread == 0) {
                this.thread = Runtime.getRuntime().availableProcessors() + 1;
            } else if (thread < 0) {
                throw new IllegalStateException("Thread should be at least 0.");
            } else if (thread > image.getWidth()) {
                this.thread = image.getWidth();
            } else {
                this.thread = thread;
            }
            return this;
        }

        Builder setConvertMode(final int convertMode) {
            this.convertMode = convertMode;
            return this;
        }

        Builder setGammaBased(final boolean isGammaBased) {
            this.isGammaBased = isGammaBased;
            return this;
        }

        Builder setProgressListener(final ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }
    }

    private class Converter implements Runnable {
        private final int x, y, width, height;

        private Converter(final int x, final int y, final int width, final int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            final List<StitchColor> alternateList = new ArrayList<>(colorList);
            for (int x = this.x; x < this.x + width; x++) {
                for (int y = this.y; y < this.y + height; y++) {
                    final Pixel pixel = new Pixel(x, y, new StitchColor(image.getRGB(x, y), null));
                    StitchColor targetColor = pixel.getColor();
                    StitchColor outputColor = findClosestColor(targetColor, alternateList);
                    StitchColor alternateColor = findSecondClosestColor(targetColor, alternateList, outputColor);
                    pixel.setColor(outputColor);
                    try {
                        outputQueue.put(new AbstractMap.SimpleEntry<>(pixel, alternateColor));
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LogPrinter.error(Resources.getString("error_has_occurred"));
                        LogPrinter.print(e);
                    }
                }
            }
        }

        private StitchColor findClosestColor(StitchColor targetColor, List<StitchColor> colorList) {
            double difference = Double.MAX_VALUE;
            StitchColor outputColor = null;
            for (final StitchColor listColor : colorList) {
                double calculatedDifference = ImageTools.calculateDifference(listColor, targetColor);
                if (calculatedDifference < difference) {
                    outputColor = listColor;
                    difference = calculatedDifference;
                }
            }
            return outputColor;
        }

        private StitchColor findSecondClosestColor(StitchColor targetColor, List<StitchColor> colorList, StitchColor outputColor) {
            colorList.remove(outputColor);
            double alternateDifference = Double.MAX_VALUE;
            StitchColor alternateColor = null;
            for (final StitchColor listColor : colorList) {
                double calculatedDifference = ImageTools.calculateDifference(listColor, targetColor);
                if (calculatedDifference < alternateDifference) {
                    alternateColor = listColor;
                    alternateDifference = calculatedDifference;
                }
            }
            return alternateColor;
        }
    }

    private final class ImageWriter implements Runnable {
        @Override
        public void run() {
        	final int imageSize = image.getWidth()*image.getHeight();
        	int count=0;
            while (true) {
                try {
                    final Entry<Pixel, StitchColor> pixelEntry = outputQueue.take();
                    if (pixelEntry == poisonPill) {
                        return;
                    }
                    final Pixel pixel = pixelEntry.getKey();
                    final int x = pixel.getX();
                    final int y = pixel.getY();
                    final int color = pixel.getColor().getRGB();
                    image.setRGB(x, y, color);
                    stitchImage.add(pixel);
                    stitchImage.addAlternateColor(pixelEntry.getValue());
                    double progress = 1.0 - (double) count++ / imageSize;
                    progressListener.onProgress(progress, Resources.getString("conversion_processing_colors"));
                    System.out.println(Resources.getString("conversion_processing_colors") + progress);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogPrinter.print(e);
                    LogPrinter.error(Resources.getString("error_has_occurred"));
                    return;
                }
            }
        }
    }

    protected final Collection<StitchColor> colorList;
    protected final BufferedImage image;
    protected final StitchImage stitchImage;
    private final int thread;
    protected final ProgressListener progressListener;
    private final BlockingQueue<Entry<Pixel, StitchColor>> outputQueue = new ArrayBlockingQueue<>(16);
    private static final Entry<Pixel, StitchColor> poisonPill = new AbstractMap.SimpleEntry<>(new Pixel(0, 0, new StitchColor(0, 0, 0, "poison")),
            new StitchColor(0, 0, 0, "poison"));

    protected ColorConverter(final Builder builder) {
        this.image = builder.image;
        this.stitchImage = builder.stitchImage;
        this.colorList = builder.colorList;
        this.thread = builder.thread;
        this.progressListener = builder.progressListener;
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newFixedThreadPool(thread);
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            int dividedWidth = width / thread;

            for (int i = 0; i < thread; i++) {
                int threadWidth = dividedWidth * i;
                int actualWidth = (i == thread - 1) ? width - threadWidth : dividedWidth;
                executorService.execute(new Converter(threadWidth, 0, actualWidth, height));
            }

            Thread writeThread = new Thread(new ImageWriter());
            writeThread.start();

            executorService.shutdown();
            while (!executorService.isTerminated()) {
                // Wait for all threads to finish
            }

            outputQueue.put(poisonPill);
            writeThread.join();

            for (final PixelList pixelList : stitchImage.getPixelLists()) {
                stitchImage.removeAlternate(pixelList.getColor());
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            LogPrinter.print(e);
            LogPrinter.error(Resources.getString("error_has_occurred"));
        }
    }
}