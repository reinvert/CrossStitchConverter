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
public class ColorConverter extends Thread {
	/**
	 * Builder of {@link ColorConverter}
	 * 
	 * @author Reinvert
	 *
	 */
	public static class Builder {
		private final Collection<StitchColor> colorList;
		private final StitchImage stitchImage;
		private final BufferedImage image;
		private int thread = Runtime.getRuntime().availableProcessors() + 1;

		/**
		 * The Constructor of {@link Builder}.
		 * 
		 * @param image
		 *            - the original {@link BufferedImage}.
		 * @param stitchImage
		 *            - the target {@link StitchImage} to save the converted pixels.
		 * @param colorList
		 *            - Stitch color lists.
		 */
		public Builder(final BufferedImage image, final StitchImage stitchImage,
				final Collection<StitchColor> colorList) {
			this.image = image;
			this.stitchImage = stitchImage;
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
		 * 
		 * @return this instance.
		 */
		public ColorConverter build() {
			return new ColorConverter(this);
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
		
		private Converter(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}
		
		@Override
		public void run() {
			for(int x=this.x; x<this.x+width; x++) {
				for(int y=this.y; y<this.y+height; y++) {
					final Pixel pixel = new Pixel(x, y, new StitchColor(new Color(image.getRGB(x, y)), ""));
					final StitchColor targetColor = pixel.getColor();
					double difference = 256 + 256 + 256;
					StitchColor outputColor = null;
					double calculatedDifference = 0;

					for (final StitchColor listColor : colorList) {
						calculatedDifference = ImageTools.calculateDifference(listColor, targetColor);
						if (calculatedDifference < difference) {
							outputColor = listColor;
							difference = calculatedDifference;
						}
					}
					pixel.setColor(outputColor);
					try {
						outputQueue.put(pixel);
					} catch (InterruptedException e) {
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
					stitchImage.add(pixel);
				} catch (final InterruptedException e) {

				}
			}
		}
	}

	private final Collection<StitchColor> colorList;
	private final StitchImage stitchImage;
	private final BufferedImage image;
	private final BlockingQueue<Pixel> outputQueue = new ArrayBlockingQueue<>(16);
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
	 * @param stitchImage
	 *            - the target {@link StitchImage} to save the converted pixels.
	 * @param colorList
	 *            - Stitch color lists.
	 * @param thread
	 *            - the number of threads.
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
				final int width=image.getWidth();
				final int height=image.getHeight();
				final float dividedWidth = width/thread;
				if(createThread != thread-1) {
					threadList.add(new Thread(new Converter((int)(dividedWidth*createThread), 0, (int)(dividedWidth), height)));
				} else {
					threadList.add(new Thread(new Converter((int)(dividedWidth*createThread), 0, width-(int)(dividedWidth*createThread), height)));
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
		} catch (final InterruptedException e) {

		}
	}
}
