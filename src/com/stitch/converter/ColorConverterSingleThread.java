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
public class ColorConverterSingleThread extends Thread {
	/**
	 * Builder of {@link ColorConverterSingleThread}
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
		 * Build and returns {@link ColorConverterSingleThread} instance.
		 * 
		 * @return this instance.
		 */
		public ColorConverterSingleThread build() {
			return new ColorConverterSingleThread(this);
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

	private ColorConverterSingleThread() {
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
	private ColorConverterSingleThread(final Builder builder) {
		this.image = builder.image;
		this.stitchImage = builder.stitchImage;
		this.colorList = builder.colorList;
		this.thread = builder.thread;
	}

	@Override
	public void run() {
		for(int y=0; y<image.getHeight(); y++) {
			for(int x=0; x<image.getWidth(); x++) {
				final Pixel oldPixel = new Pixel(x, y, new StitchColor(new Color(image.getRGB(x, y)), ""));
				final StitchColor targetColor = oldPixel.getColor();
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
				final Pixel newPixel = new Pixel(x, y, outputColor);
				final double quantErrorRed = oldPixel.getColor().getRed() - newPixel.getColor().getRed();
				final double quantErrorGreen = oldPixel.getColor().getGreen() - newPixel.getColor().getGreen();
				final double quantErrorBlue = oldPixel.getColor().getBlue() - newPixel.getColor().getBlue();
				
				//System.out.println(quantErrorRed + ", " + quantErrorGreen + ", " + quantErrorBlue);
				
				try {
					Color originalColor = new Color(image.getRGB(x+1, y));
					Color convertedColor = new Color(originalColor.getRed()+(int)(7d/16d * quantErrorRed), originalColor.getGreen()+(int)(7d/16d * quantErrorGreen), originalColor.getBlue()+(int)(7d/16d * quantErrorBlue));
					image.setRGB(x+1, y, convertedColor.getRGB());
				} catch(final Exception e) { 
					
				}
				try {
					Color originalColor = new Color(image.getRGB(x-1, y+1));
					Color convertedColor = new Color(originalColor.getRed()+(int)(3d/16d * quantErrorRed), originalColor.getGreen()+(int)(3d/16d * quantErrorGreen), originalColor.getBlue()+(int)(3d/16d * quantErrorBlue));
					image.setRGB(x-1, y+1, convertedColor.getRGB());
				} catch(final Exception e) { 
					
				}
				try {
					Color originalColor = new Color(image.getRGB(x, y+1));
					Color convertedColor = new Color(originalColor.getRed()+(int)(5d/16d * quantErrorRed), originalColor.getGreen()+(int)(5d/16d * quantErrorGreen), originalColor.getBlue()+(int)(5d/16d * quantErrorBlue));
					image.setRGB(x, y+1, convertedColor.getRGB());
				} catch(final Exception e) { 
					
				}
				try {
					Color originalColor = new Color(image.getRGB(x+1, y+1));
					Color convertedColor = new Color(originalColor.getRed()+(int)(1d/16d * quantErrorRed), originalColor.getGreen()+(int)(1d/16d * quantErrorGreen), originalColor.getBlue()+(int)(1d/16d * quantErrorBlue));
					image.setRGB(x+1, y+1, convertedColor.getRGB());
				} catch(final Exception e) { 
					
				}
				stitchImage.add(newPixel);
			}
		}
	}
}
