package com.stitch.converter;

import java.awt.image.BufferedImage;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.stitch.converter.model.StitchImage;

import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;

/**
 * Color converting class.
 * 
 * @author Reinvert
 *
 */
final class ColorConverter extends Thread {
	/**
	 * Builder of {@link ColorConverter}
	 * 
	 * @author Reinvert
	 *
	 */
	static class Builder {
		private final Collection<StitchColor> colorList;
		private final BufferedImage image;
		private final StitchImage stitchImage;
		private int thread = Runtime.getRuntime().availableProcessors() + 1;

		/**
		 * The Constructor of {@link Builder}.
		 * 
		 * @param image       - the original {@link BufferedImage}.
		 * @param stitchImage - the target {@link StitchImage} to save the converted
		 *                    pixels.
		 * @param colorList   - Stitch color lists.
		 */
		Builder(final BufferedImage image, final StitchImage stitchImage, final Collection<StitchColor> colorList) {
			this.image = image;
			this.stitchImage = stitchImage;
			this.colorList = colorList;
		}

		/**
		 * Build and returns {@link ColorConverter} instance.
		 * 
		 * @return this instance.
		 */
		ColorConverter build() {
			return new ColorConverter(this);
		}

		/**
		 * Sets the number of threads to be used for the operation.
		 * 
		 * @param thread - the number of threads.
		 * @return this instance.
		 */
		Builder setThread(final int thread) {
			if (thread == 0) {
				this.thread = Runtime.getRuntime().availableProcessors() + 1;
			} else if (thread < 0) {
				throw new IllegalStateException("Thread should be at least 0.");
			} else {
				this.thread = thread;
			}
			return this;
		}
	}

	/**
	 * Manages the converting color threads.
	 * 
	 * @author Reinvert
	 *
	 */
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
			final ArrayList<StitchColor> alternateList = new ArrayList<>();
			for (int x = this.x; x < this.x + width; x++) {
				for (int y = this.y; y < this.y + height; y++) {
					final Pixel pixel = new Pixel(x, y, new StitchColor(image.getRGB(x, y), null));
					final StitchColor targetColor = pixel.getColor();
					double difference = 256, alternateDifference = 256;
					StitchColor outputColor = null, alternateColor = null;
					double calculatedDifference = 0;

					for (final StitchColor listColor : colorList) {
						calculatedDifference = ImageTools.calculateDifference(listColor, targetColor);
						if (calculatedDifference < difference) {
							outputColor = listColor;
							difference = calculatedDifference;
						}
					}
					alternateList.clear();
					alternateList.addAll(colorList);
					alternateList.remove(outputColor);
					for (final StitchColor listColor : alternateList) {
						calculatedDifference = ImageTools.calculateDifference(listColor, targetColor);
						if (calculatedDifference < alternateDifference) {
							alternateColor = listColor;
							alternateDifference = calculatedDifference;
						}
					}
					pixel.setColor(outputColor);
					try {
						outputQueue.put(new AbstractMap.SimpleEntry<>(pixel, alternateColor));
					} catch (final InterruptedException e) {
						LogPrinter.error(Resources.getString("error_has_occurred"));
						LogPrinter.print(e);
					}
				}
			}
		}
	}

	/**
	 * Reads the pixel from queue and write the converted pixels to
	 * {@link StitchImage}.
	 * 
	 * @author Reinvert
	 *
	 */
	private final class ImageWriter implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					final Entry<Pixel, StitchColor> pixel = outputQueue.take();
					if (pixel == poisonPill) {
						return;
					}
					image.setRGB(pixel.getKey().getX(), pixel.getKey().getY(), pixel.getKey().getColor().getRGB());
					stitchImage.add(pixel.getKey());
					stitchImage.addAlternateColor(pixel.getValue());
				} catch (final InterruptedException e) {
					LogPrinter.print(e);
					LogPrinter.error(Resources.getString("error_has_occurred"));
					return;
				}
			}
		}
	}

	private final Collection<StitchColor> colorList;
	private final BufferedImage image;
	private final BlockingQueue<Entry<Pixel, StitchColor>> outputQueue = new ArrayBlockingQueue<>(16);
	private final Entry<Pixel, StitchColor> poisonPill = new AbstractMap.SimpleEntry<>(new Pixel(0, 0, new StitchColor(0, 0, 0, "poison")),
			new StitchColor(0, 0, 0, "poison"));
	private final StitchImage stitchImage;
	private final int thread;
	private final ArrayList<Thread> threadList = new ArrayList<>();

	private ColorConverter() {
		throw new AssertionError();
	}

	/**
	 * The constructor of the {@ColorConverter}.
	 * 
	 * @param image       - the original {@link BufferedImage}.
	 * @param stitchImage - the target {@link StitchImage} to save the converted
	 *                    pixels.
	 * @param colorList   - Stitch color lists.
	 * @param thread      - the number of threads.
	 */
	private ColorConverter(final Builder builder) {
		this.image = builder.image;
		this.stitchImage = builder.stitchImage;
		this.colorList = builder.colorList;
		this.thread = builder.thread;
	}

	@Override
	public void run() {
		try {
			for (int createThread = 0; createThread < thread; createThread++) {
				final int width = image.getWidth();
				final int height = image.getHeight();
				final float dividedWidth = width / thread;
				if (createThread != thread - 1) {
					threadList.add(new Thread(
							new Converter((int) (dividedWidth * createThread), 0, (int) (dividedWidth), height)));
				} else {
					threadList.add(new Thread(new Converter((int) (dividedWidth * createThread), 0,
							width - (int) (dividedWidth * createThread), height)));
				}
			}
			for (final Thread colorThread : threadList) {
				colorThread.start();
			}
			final Thread writeThread = new Thread(new ImageWriter());
			writeThread.start();
			for (final Thread colorThread : threadList) {
				colorThread.join();
			}
			outputQueue.put(poisonPill);
			writeThread.join();
			for (final PixelList pixelList : stitchImage.getPixelLists()) {
				stitchImage.removeAlternate(pixelList.getColor());
			}
		} catch (final InterruptedException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("error_has_occurred"));
		}
	}
}
