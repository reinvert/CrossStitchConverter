package com.stitch.converter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;

import com.stitch.converter.model.StitchImage;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;

/**
 * A class that contains methods for manipulating
 * {@link java.awt.image.BufferedImage BufferedImage}.
 * 
 * @author Reinvert
 *
 */
final class ImageTools {
	/**
	 * Calculate difference between two {@link java.awt.Color Color}.
	 * 
	 * @param originalColor - the original {@link java.awt.Color Color}.
	 * @param targetColor   - the target {@link java.awt.Color Color}.
	 * @return difference between two {@link java.awt.Color Color}.
	 */
	static double calculateDifference(final StitchColor originalColor, final StitchColor targetColor) {
		final int originalRed = originalColor.getRed();
		final int originalGreen = originalColor.getGreen();
		final int originalBlue = originalColor.getBlue();

		final int targetRed = targetColor.getRed();
		final int targetGreen = targetColor.getGreen();
		final int targetBlue = targetColor.getBlue();

		return Math.sqrt(Math.pow(originalRed - targetRed, 2) + Math.pow(originalGreen - targetGreen, 2) + Math.pow(originalBlue - targetBlue, 2));
	}

	static StitchColor calculateRemoveString(final StitchImage stitchImage,
			final HashMap<String, Integer> usedColorCount) {
		StitchColor uselessColor = null;
		double difference = 256;
		final ArrayList<PixelList> list = new ArrayList<PixelList>(stitchImage.getPixelLists());
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				final StitchColor originalColor = list.get(i).getColor();
				final StitchColor targetColor = list.get(j).getColor();

				final double avgcolor = calculateDifference(originalColor, targetColor);
				if (difference > avgcolor) {
					final int orgcount = usedColorCount.get(list.get(i).getColor().getName());
					final int tarcount = usedColorCount.get(list.get(j).getColor().getName());
					difference = avgcolor;
					if (orgcount >= tarcount) {
						uselessColor = list.get(j).getColor();
					} else {
						uselessColor = list.get(i).getColor();
					}
				}
			}
		}
		return uselessColor;
	}

	/**
	 * Gets the scale between two size. Always same or lower than 1.0.
	 * 
	 * @param originalSize - the master size.
	 * @param targetSize   - the target size.
	 * @return the scale of two size.
	 */
	static double getScaleFactor(final int originalSize, final int targetSize) {
		double scale = 1d;
		if (originalSize > targetSize) {
			scale = (double) targetSize / (double) originalSize;
		} else {
			scale = (double) originalSize / (double) targetSize;
		}
		return scale;
	}

	/**
	 * Gets the scale between two {@link java.awt.Dimension Dimension}. Always same
	 * or lower than 1.0.
	 * 
	 * @param originalSize - the original {@link java.awt.Dimension Dimension}.
	 * @param targetSize   - the target {@link java.awt.Dimension Dimension}.
	 * @return the scale of two {@link java.awt.Dimension Dimension}.
	 */
	static double getScaleFactorToFit(final Dimension originalSize, final Dimension targetSize)
			throws NullPointerException {
			final double widthScale = getScaleFactor(originalSize.width, targetSize.width);
			final double heightScale = getScaleFactor(originalSize.height, targetSize.height);
			return Math.min(widthScale, heightScale);
	}

	/**
	 * Converts the size of the image to read or just read.
	 * 
	 * @param file   - the image {@link java.io.File file}.
	 * @param scaled - true if converts the size of the image.
	 * @return {@link java.awt.BufferedImage BufferedImage} file.
	 * @throws IOException occurs when the image file can't read.
	 */
	static BufferedImage readImage(final File file, final boolean scaled, final int width, final int height)
			throws IOException {
		BufferedImage image = ImageIO.read(file);
		if (scaled) {
			final double scaleFactor = Math.min(1d, ImageTools.getScaleFactorToFit(
					new Dimension(image.getWidth(), image.getHeight()), new Dimension(width, height)));
			final int scaleWidth = (int) Math.round(image.getWidth() * scaleFactor);
			final int scaleHeight = (int) Math.round(image.getHeight() * scaleFactor);
			image = ImageTools.resize(image, scaleWidth, scaleHeight);
		}
		final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		final GraphicsConfiguration graphicsConfiguration = graphicsEnvironment.getDefaultScreenDevice()
				.getDefaultConfiguration();
		final BufferedImage backgroundFilledBufferedImage = graphicsConfiguration
				.createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.OPAQUE);
		backgroundFilledBufferedImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
		backgroundFilledBufferedImage.setAccelerationPriority(1);
		return backgroundFilledBufferedImage;
	}

	static BufferedImage resize(final BufferedImage img, final int newW, final int newH) {
		final java.awt.Image originalImage = img.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
		final BufferedImage outputImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		final Graphics2D g2d = outputImage.createGraphics();
		g2d.drawImage(originalImage, 0, 0, null);
		g2d.dispose();

		return outputImage;
	}

	private ImageTools() {
		throw new AssertionError("Singleton class should not be accessed by constructor.");
	}
}
