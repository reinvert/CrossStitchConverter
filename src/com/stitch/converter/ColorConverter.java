package com.stitch.converter;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.stitch.converter.model.*;

class ColorConverter implements Runnable {	
	protected static class PaletteEntry {
        final StitchColor stitchColor;
        final ImageTools.Lab lab;
        final double chroma;

        PaletteEntry(final StitchColor stitchColor, final ImageTools.Lab lab) {
            this.stitchColor = stitchColor;
            this.lab = lab;
            chroma = Math.hypot(lab.a, lab.b);
        }
    }
	
    static class Builder {
        private final Collection<StitchColor> colorList;
        private final BufferedImage image;
        private final StitchImage stitchImage;
        private int thread = Runtime.getRuntime().availableProcessors() + 1;
        private ProgressListener progressListener = new ProgressListener() {
			@Override
			public void onProgress(double progress, String message) {
				
			}
			
			public void finished() {
				
			}
        };
        int convertMode = GraphicsEngine.FLOYD;

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
        
        Builder setProgressListener(final ProgressListener progressListener) {
            this.progressListener = progressListener;
            return this;
        }
    }

    private class Converter implements Runnable {
        private final int x, y, width, height;
        private StitchColor outputColor;
        private StitchColor alternateColor;

        private Converter(final int x, final int y, final int width, final int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        public void run() {
            for (int x = this.x; x < this.x + width; x++) {
                for (int y = this.y; y < this.y + height; y++) {
                	final Pixel pixel = new Pixel(x, y, new StitchColor(image.getRGB(x, y), null));
                	final StitchColor targetColor = pixel.getColor();
                	final ImageTools.Lab targetLab = ImageTools.rgbToLab(targetColor.asFX());

                	findClosestColors(targetLab);
                    pixel.setColor(outputColor);
                    try {
                        outputQueue.put(new AbstractMap.SimpleEntry<>(pixel, alternateColor));
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        LogPrinter.error(Resources.getString("error_has_occurred"));
                        LogPrinter.print(e);
                        return;
                    }
                }
            }
        }

        private void findClosestColors(final ImageTools.Lab targetLab) {
            double difference = Double.MAX_VALUE;
            double alternateDifference = Double.MAX_VALUE;

            outputColor = null;
            alternateColor = null;

            final double targetChroma = Math.hypot(targetLab.a, targetLab.b);

            for (final PaletteEntry entry : cachedPalette) {
                final double calculatedDifference =
                        ImageTools.calculateCIEDE2000Squared(
                                targetLab,
                                entry.lab,
                                targetChroma,
                                targetChroma
                        );

                if (calculatedDifference < difference) {
                    alternateColor = outputColor;
                    alternateDifference = difference;

                    outputColor = entry.stitchColor;
                    difference = calculatedDifference;
                } else if (!entry.stitchColor.equals(outputColor)
                        && calculatedDifference < alternateDifference) {
                    alternateColor = entry.stitchColor;
                    alternateDifference = calculatedDifference;
                }
            }

            if (alternateColor == null) {
                alternateColor = outputColor;
            }
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
                    double progress = (double) count++ / imageSize;
                    progressListener.onProgress(progress, Resources.getString("conversion_processing_colors"));
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LogPrinter.print(e);
                    LogPrinter.error(Resources.getString("error_has_occurred"));
                    return;
                }
            }
        }
    }
    
    protected final List<PaletteEntry> cachedPalette;
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

        cachedPalette = new ArrayList<>(colorList.size());

        for (final StitchColor color : colorList) {
            final ImageTools.Lab lab = ImageTools.rgbToLab(color.asFX());
            cachedPalette.add(new PaletteEntry(color, lab));
        }
    }

    @Override
    public void run() {
        final ExecutorService executorService = Executors.newFixedThreadPool(thread);
        try {
            final int width = image.getWidth();
            final int height = image.getHeight();
            final int dividedWidth = width / thread;

            for (int i = 0; i < thread; i++) {
                int threadWidth = dividedWidth * i;
                int actualWidth = (i == thread - 1) ? width - threadWidth : dividedWidth;
                executorService.execute(new Converter(threadWidth, 0, actualWidth, height));
            }

            final Thread writeThread = new Thread(new ImageWriter());
            writeThread.setDaemon(true);
            writeThread.start();

            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

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
    
    protected StitchColor findClosestColor(final ImageTools.Lab colorLab) {
        double difference = Double.MAX_VALUE;
        StitchColor closestColor = null;
        final double colorChroma =
                Math.sqrt(colorLab.a * colorLab.a + colorLab.b * colorLab.b);
        
        for (final PaletteEntry entry : cachedPalette) {
        	final double calculatedDifference =
        	        ImageTools.calculateCIEDE2000Squared(
        	                colorLab,
        	                entry.lab,
        	                colorChroma,
        	                entry.chroma);
            

            if (calculatedDifference < difference) {
                closestColor = entry.stitchColor;
                difference = calculatedDifference;
            }
        }

        return closestColor;
    }
}