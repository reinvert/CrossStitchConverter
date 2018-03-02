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
class ImageTools {
	private ImageTools() {
		throw new AssertionError();
	}

	/**
	 * Converts the size of the image to read or just read.
	 * 
	 * @param file
	 *            - the image {@link java.io.File file}.
	 * @param scaled
	 *            - true if converts the size of the image.
	 * @return {@link java.awt.BufferedImage BufferedImage} file.
	 * @throws IOException
	 *             occurs when the image file can't read.
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

	/**
	 * Read {@link java.awt.BufferedImage BufferedImage} to
	 * {@link java.util.ArrayList ArrayList}<{@link java.util.ArrayList
	 * ArrayList}<{@link java.awt.Color Color}>>.
	 * 
	 * @param image
	 *            - the {@link java.awt.BufferedImage BufferedImage} to convert.
	 * @return the {@link java.util.ArrayList ArrayList}<{@link java.util.ArrayList
	 *         ArrayList}<{@link java.awt.Color Color}>> array.
	 */
	static ArrayList<ArrayList<StitchColor>> readPixelArray(final BufferedImage image) {
		final int w = image.getWidth(), h = image.getHeight();
		final ArrayList<ArrayList<StitchColor>> output = new ArrayList<ArrayList<StitchColor>>();
		for (int i = 0; i < w; i++) {
			output.add(new ArrayList<StitchColor>());
			final ArrayList<StitchColor> x = output.get(i);
			for (int j = 0; j < h; j++) {
				x.add(new StitchColor(new java.awt.Color(image.getRGB(i, j)), ""));
			}
		}
		return output;
	}

	/**
	 * Gets the scale between two size. Always same or lower than 1.0.
	 * 
	 * @param iMasterSize
	 *            - the master size.
	 * @param iTargetSize
	 *            - the target size.
	 * @return the scale of two size.
	 */
	static double getScaleFactor(final int iMasterSize, final int iTargetSize) {
		double dScale = 1;
		if (iMasterSize > iTargetSize) {
			dScale = (double) iTargetSize / (double) iMasterSize;
		} else {
			dScale = (double) iTargetSize / (double) iMasterSize;
		}
		return dScale;
	}

	/**
	 * Gets the scale between two {@link java.awt.Dimension Dimension}. Always same
	 * or lower than 1.0.
	 * 
	 * @param original
	 *            - the original {@link java.awt.Dimension Dimension}.
	 * @param toFit
	 *            - the target {@link java.awt.Dimension Dimension}.
	 * @return the scale of two {@link java.awt.Dimension Dimension}.
	 */
	static double getScaleFactorToFit(final Dimension original, final Dimension toFit) {
		double dScale = 1d;
		if (original != null && toFit != null) {
			final double dScaleWidth = getScaleFactor(original.width, toFit.width);
			final double dScaleHeight = getScaleFactor(original.height, toFit.height);
			dScale = Math.min(dScaleHeight, dScaleWidth);
		}
		return dScale;
	}

	/**
	 * Calculate difference between two {@link java.awt.Color Color}.
	 * 
	 * @param originalColor
	 *            - the original {@link java.awt.Color Color}.
	 * @param targetColor
	 *            - the target {@link java.awt.Color Color}.
	 * @return difference between two {@link java.awt.Color Color}.
	 */
	static double calculateDifference(final StitchColor originalColor, final StitchColor targetColor) {
		final double or = originalColor.getRed();
		final double og = originalColor.getGreen();
		final double ob = originalColor.getBlue();

		final double tr = targetColor.getRed();
		final double tg = targetColor.getGreen();
		final double tb = targetColor.getBlue();

		return Math.abs(or - tr) + Math.abs(og - tg) + Math.abs(ob - tb);
	}

	static StitchColor calculateRemoveString(final StitchImage stitchImage, final HashMap<String, Integer> usedColorCount) {
		StitchColor uselessColor = null;
		double difference = 255 + 255 + 255;
		final ArrayList<PixelList> list = new ArrayList<PixelList>(stitchImage.getPixelLists());
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				final StitchColor originalColor = list.get(i).getColor();
				final double originalRed = originalColor.getRed();
				final double originalGreen = originalColor.getGreen();
				final double originalBlue = originalColor.getBlue();
				final StitchColor targetColor = list.get(j).getColor();
				final double targetRed = targetColor.getRed();
				final double targetGreen = targetColor.getGreen();
				final double targetBlue = targetColor.getBlue();
				final double avgcolor = Math.abs(originalRed - targetRed) + Math.abs(originalGreen - targetGreen)
						+ Math.abs(originalBlue - targetBlue);
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

	static BufferedImage resize(final BufferedImage img, final int newW, final int newH) {
		final java.awt.Image originalImage = img.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
		final BufferedImage outputImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		final Graphics2D g2d = outputImage.createGraphics();
		g2d.drawImage(originalImage, 0, 0, null);
		g2d.dispose();

		return outputImage;
	}
}
