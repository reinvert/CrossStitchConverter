package com.stitch.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import com.stitch.converter.model.StitchImage;

import javafx.scene.paint.Color;

import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;

/**
 * Convert and save the output image.
 * 
 * @author Reinvert
 */
public class GraphicsEngine implements Runnable {
	/**
	 * Builder of {@link GraphicsEngine}.
	 * 
	 * @author Reinvert
	 */
	public static class Builder {
		private final File csv;
		private final File file;

		private Mode loadMode = Mode.NEW_FILE;
		private int colorLimit = -1, thread;
		private boolean resize = false;
		private StitchColor backgroundColor = new StitchColor(Color.WHITE, "");
		private List<Listener> listenerList = new ArrayList<>();

		/**
		 * Constructor of the {@link Builder}.
		 * 
		 * @param csv  - the csv {@link File} containing color information.
		 * @param file - the image {@link File} to convert.
		 */
		public Builder(final File csv, final File file) {
			this.file = file;
			this.csv = csv;
			thread = Runtime.getRuntime().availableProcessors() + 1;
		}

		/**
		 * Set whether or not to resize the image.
		 * 
		 * @param resize - the boolean whether or not to resize the images.
		 * @return this instance.
		 */
		public Builder setResize(final boolean resize) {
			this.resize = resize;
			return this;
		}

		/**
		 * Set background {@link StitchColor} of image.
		 * 
		 * @param backgroundColor - the background {@link StitchColor} of image.
		 * @return this instance.
		 */
		public Builder setBackground(final StitchColor backgroundColor) {
			this.backgroundColor = backgroundColor;
			return this;
		}

		/**
		 * Set the limit of colors.
		 * 
		 * @param colorLimit - the limit of colors. 0 means unlimited.
		 * @return this instance.
		 */
		public Builder setColorLimit(final int colorLimit) {
			this.colorLimit = colorLimit;
			return this;
		}

		/**
		 * Set whether it is a new file or an existing one.
		 * 
		 * @param loadMode - the {@link Mode} whether it is a new file or an existing
		 *                 one.
		 * @return this instance.
		 */
		public Builder setMode(final Mode loadMode) {
			this.loadMode = loadMode;
			return this;
		}

		/**
		 * Set the number of {@link java.lang.Thread Threads} to work with.
		 * 
		 * @param thread - the number of {@link java.lang.Thread Threads} to work with.
		 * @return this instance.
		 */
		public Builder setThread(final int thread) {
			this.thread = thread;
			return this;
		}

		public Builder setListener(final Listener listener) {
			listenerList.add(listener);
			return this;
		}

		/**
		 * Build and returns {@link GraphicsEngine} instance.
		 * 
		 * @return the {@link GraphicsEngine} instance.
		 */
		public GraphicsEngine build() {
			return new GraphicsEngine(this);
		}

	}

	/**
	 * Determine whether it is a new file or an existing one.
	 * 
	 * @author Reinvert
	 *
	 */
	public enum Mode {
		LOAD, NEW_FILE
	}

	private final File csv;
	private final File file;
	private BufferedImage image;
	private final StitchColor backgroundColor;
	private final Mode loadMode;
	private final List<Listener> listenerList;

	private final int colorLimit, thread;
	private final boolean scaled;

	private final HashMap<String, Integer> usedColorCount = new HashMap<String, Integer>();

	/**
	 * Construct {@link GraphicsEngine} defined by {@link Builder}.
	 * 
	 * @param builder - the {@link Builder} that defines {@link GraphicsEngine}.
	 */
	private GraphicsEngine(final Builder builder) {
		file = builder.file;
		csv = builder.csv;
		colorLimit = builder.colorLimit;
		scaled = builder.resize;
		backgroundColor = builder.backgroundColor;
		loadMode = builder.loadMode;
		thread = builder.thread;
		listenerList = builder.listenerList;
	}

	/**
	 * Add count to {@link java.util.HashMap HashMap}. It is used to determine which
	 * {@link java.awt.Color Color} is used the most.
	 * 
	 * @param key - the name of used {@link java.awt.Color Color}.
	 */
	private void addCount(final String key) {
		try {
			usedColorCount.put(key, usedColorCount.get(key) + 1);
		} catch (final NullPointerException e) {
			usedColorCount.put(key, 1);
		}
	}

	/**
	 * Draw converted pixel to image.
	 * 
	 * @param pixel          - the {@link Pixel} contains x, y, color value.
	 * @param convertedImage - the {@link StitchImage} contains name of
	 *                       {@link java.awt.Color Color}.
	 */
	private void drawPixelToImage(final Pixel pixel, final StitchImage convertedImage) {
		final StitchColor targetColor = pixel.getColor();
		final int x = pixel.getX(), y = pixel.getY();
		image.setRGB(x, y, targetColor.asAWT().getRGB());
		addCount(targetColor.getName());
	}

	/**
	 * Create a new blueprint from {@link java.util.ArrayList
	 * ArrayList}<{@link java.io.File File}>.
	 * 
	 * @param file - the list of image files.
	 */
	private void makeNewFile(final File file) {
		ArrayList<StitchColor> colorList;
		try {
			final String csvInput = new String(Files.readAllBytes(csv.toPath()));
			final ArrayList<ArrayList<String>> csvArray = CSVReader.read(csvInput);
			colorList = CSVReader.readColorList(csvArray);
		} catch (final IOException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("read_failed"));
			return;
		} catch (final NoSuchElementException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("rgb_missing", e.getMessage()));
			return;
		} catch (final NumberFormatException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("rgb_not_integer", e.getMessage()));
			return;
		} catch (final IllegalArgumentException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("rgb_out_of_range", e.getMessage()));
			return;
		}

		final StitchImage stitchImage = new StitchImage();
		stitchImage.setChanged(true);
		try {
			int resizeLength = Preferences.getInteger("resizeLength", 200);
			image = ImageTools.readImage(file, scaled, resizeLength, resizeLength);
		} catch (final NullPointerException | IOException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("cant_read_image"));
			return;
		}
		boolean onceRunned = false;
		StitchColor toRemove = null;
		Collection<PixelList> pixelLists = null;
		do {
			stitchImage.setSize(image.getWidth(), image.getHeight());
			if (onceRunned == true) {
				removeColor(colorList, toRemove);
			}
			usedColorCount.clear();

			final ColorConverter converter = new ColorConverter.Builder(image, stitchImage, colorList).setThread(thread)
					.build();
			converter.start();

			try {
				converter.join();
			} catch (final InterruptedException e) {

			}

			/*
			 * final ColorConverterSingleThread converter = new
			 * ColorConverterSingleThread.Builder(image, stitchImage,
			 * colorList).setThread(thread) .build(); converter.start(); try {
			 * converter.join(); } catch (final InterruptedException e) {
			 * 
			 * }
			 */
			stitchImage.setBackground(backgroundColor);
			pixelLists = stitchImage.getPixelLists();

			for (final PixelList pixelList : pixelLists) {
				final TreeSet<Pixel> pixelSet = pixelList.getPixelSet();
				for (final Pixel pixel : pixelSet) {
					drawPixelToImage(pixel, stitchImage);
				}
			}
			onceRunned = true;
			toRemove = ImageTools.calculateRemoveString(stitchImage, usedColorCount);
		} while (colorLimit != 0 && stitchImage.getPixelLists().size() > colorLimit);
		stitchImage.setChanged(true);
		onFinished(stitchImage);
	}

	/**
	 * Read from saved *.dmc {@link File} to {@link StitchImage} instance.
	 * 
	 * @param file - saved *.dmc {@link File}.
	 */
	private void readFromSavedFile(final File file) {
		final StitchImage stitchImage;
		try {
			stitchImage = (StitchImage) Resources.readObject(file);
		} catch (final ClassNotFoundException | IOException e) {
			LogPrinter.print(e.getMessage());
			LogPrinter.error(Resources.getString("read_failed", file.getName()));
			return;
		}

		if (this.loadMode == Mode.NEW_FILE) {
			for (final PixelList pixelList : stitchImage.getPixelLists()) {
				for (final Pixel pixel : pixelList.getPixelSet()) {
					drawPixelToImage(pixel, stitchImage);
				}
			}
		}
		onFinished(stitchImage);
	}

	/**
	 * Remove the color from the {@link Collection}. Only works if maximum color is
	 * limited.
	 * 
	 * @param colorName - the {@link java.lang.String String} Array containing the
	 *                  names of all threads.
	 * @param colorList - the {@link java.awt.color Color} Array containing the
	 *                  colors of all threads.
	 * @param toRemove  - the name of the thread to delete.
	 */
	private void removeColor(final Collection<StitchColor> colorList, final StitchColor toRemove) {
		final Iterator<StitchColor> iterator = colorList.iterator();
		StitchColor color;
		while ((color = iterator.next()) != null) {
			if (color.equals(toRemove)) {
				colorList.remove(color);
				return;
			}
		}
	}

	/**
	 * Creates {@link Blueprint} image and used thread list.
	 */
	@Override
	public void run() {
		if (loadMode.equals(Mode.NEW_FILE)) {
			makeNewFile(file);
		} else if (loadMode.equals(Mode.LOAD)) {
			readFromSavedFile(file);
		}
	}

	public void onFinished(final StitchImage image) {
		for (final Listener listener : listenerList) {
			listener.onFinished(image);
		}
	}
}