package com.stitch.converter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.stitch.converter.model.StitchImage;
import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.StitchColor;

/**
 * Color converting class.
 * 
 * @author Reinvert
 *
 */
public class ColorConverter extends CustomThread {
	/**
	 * Builder of {@link ColorConverter}
	 * 
	 * @author Reinvert
	 *
	 */
	public static class Builder {
		private final Collection<StitchColor> colorList;
		private final StitchImage convertedImage;
		private final BufferedImage image;
		private int thread = Runtime.getRuntime().availableProcessors() + 1;

		/**
		 * The Constructor of {@link Builder}.
		 * 
		 * @param image
		 *            - the original {@link BufferedImage}.
		 * @param convertedImage
		 *            - the target {@link StitchImage} to save the converted pixels.
		 * @param colorList
		 *            - Stitch color lists.
		 */
		public Builder(final BufferedImage image, final StitchImage convertedImage,
				final Collection<StitchColor> colorList) {
			this.image = image;
			this.convertedImage = convertedImage;
			this.colorList = colorList;
		}

		/**
		 * Sets the number of threads to be used for the operation.
		 * 
		 * @param thread
		 *            - the number of threads.
		 * @return this instance.
		 */
		public Builder setThread(final int thread) {
			if (thread == 0) {
				this.thread = Runtime.getRuntime().availableProcessors() + 1;
			} else {
				this.thread = thread;
			}
			return this;
		}
		
		/**
		 * Build and returns {@link ColorConverter} instance.
		 * @return this instance.
		 */
		public ColorConverter build() {
			return new ColorConverter(image, convertedImage, colorList, thread);
		}
	}

	/**
	 * Manages the converting color threads.
	 * @author Reinvert
	 *
	 */
	private class Converter implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					final Pixel pixel = inputQueue.take();
					if (pixel == poisonPill) {
						return;
					}
					final StitchColor targetColor = pixel.getColor();
					double difference = 256 + 256 + 256;
					StitchColor outputColor = null;
					double calculatedDifference = 0;

					for (final StitchColor calculateColor : colorList) {
						calculatedDifference = ImageTools.calculateDifference(calculateColor, targetColor);
						if (calculatedDifference < difference) {
							outputColor = calculateColor;
							difference = calculatedDifference;
						}
					}
					pixel.setColor(outputColor);
					outputQueue.put(pixel);
				} catch (final InterruptedException e) {

				}
			}
		}
	}
	
	/**
	 * Reads the image and put pixels to queue.
	 * @author Reinvert
	 *
	 */
	private class ImageReader implements Runnable {
		@Override
		public void run() {
			for (int x = 0; x < convertedImage.getWidth(); x++) {
				for (int y = 0; y < convertedImage.getHeight(); y++) {
					try {
						inputQueue.put(new Pixel(x, y, new StitchColor(new Color(image.getRGB(x, y)), "")));
					} catch (final InterruptedException e) {

					}
				}
			}
			for (int threadCount = 0; threadCount < thread; threadCount++) {
				try {
					inputQueue.put(poisonPill);
				} catch (InterruptedException e) {

				}
			}
		}
	}
	
	/**
	 * Reads the pixel from queue and write the converted pixels to {@link StitchImage}.
	 * @author Reinvert
	 *
	 */
	private class ImageWriter implements Runnable {
		@Override
		public void run() {
			while (true) {
				try {
					final Pixel pixel = outputQueue.take();
					if (pixel == poisonPill) {
						return;
					}
					image.setRGB(pixel.getX(), pixel.getY(), pixel.getColor().asAWT().getRGB());
					convertedImage.add(pixel);
				} catch (final InterruptedException e) {

				}
			}
		}
	}

	private final Collection<StitchColor> colorList;
	private final StitchImage convertedImage;
	private final BufferedImage image;
	private final BlockingQueue<Pixel> inputQueue = new ArrayBlockingQueue<>(16),
			outputQueue = new ArrayBlockingQueue<>(16);
	private final Pixel poisonPill = new Pixel(-1, -1, null);
	private final int thread;
	private final ArrayList<Thread> threadList = new ArrayList<>();

	private ColorConverter() {
		throw new AssertionError();
	}

	/**
	 * The constructor of the {@ColorConverter}.
	 * 
	 * @param image
	 *            - the original {@link BufferedImage}.
	 * @param convertedImage
	 *            - the target {@link StitchImage} to save the converted pixels.
	 * @param colorList
	 *            - Stitch color lists.
	 * @param thread
	 *            - the number of threads.
	 */
	private ColorConverter(final BufferedImage image, final StitchImage convertedImage,
			final Collection<StitchColor> colorList, final int thread) {
		this.image = image;
		this.convertedImage = convertedImage;
		this.colorList = colorList;
		this.thread = thread;
	}

	@Override
	public void run() {
		new Thread(new ImageReader()).start();
		for (int createThread = 0; createThread < thread; createThread++) {
			threadList.add(new Thread(new Converter()));
		}
		for (final Thread colorThread : threadList) {
			colorThread.start();
		}
		final Thread writeThread = new Thread(new ImageWriter());
		writeThread.start();
		for (final Thread colorThread : threadList) {
			try {
				colorThread.join();
			} catch (final InterruptedException e) {

			}
		}
		try {
			for (int createThread = 0; createThread < thread; createThread++) {
				outputQueue.put(poisonPill);
			}
		} catch (final InterruptedException e) {

		}
		sendGraphics(convertedImage, Preferences.getDouble("scale", 15d));
	}
}
