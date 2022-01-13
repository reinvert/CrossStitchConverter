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

import com.stitch.converter.model.StitchImage;

import javafx.scene.paint.Color;

import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;

/**
 * Convert and save the output image.
 * 
 * @author Reinvert
 */
public final class GraphicsEngine implements Runnable {
	/**
	 * Builder of {@link GraphicsEngine}.
	 * 
	 * @author Reinvert
	 */
	public final static class Builder {
		private StitchColor backgroundColor = new StitchColor(Color.WHITE, null);
		private int colorLimit = -1, thread;

		private final File csv;
		private final File file;
		private final List<Listener> listenerList = new ArrayList<>();
		private Mode loadMode = Mode.NEW_FILE;
		private boolean scaled = false;

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
		 * Build and returns {@link GraphicsEngine} instance.
		 * 
		 * @return the {@link GraphicsEngine} instance.
		 */
		public GraphicsEngine build() {
			return new GraphicsEngine(this);
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

		public Builder setListener(final Listener listener) {
			listenerList.add(listener);
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
		 * Set whether or not to resize the image.
		 * 
		 * @param resize - the boolean whether or not to resize the images.
		 * @return this instance.
		 */
		public Builder setScaled(final boolean scaled) {
			this.scaled = scaled;
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

	private final StitchColor backgroundColor;
	private final int colorLimit, thread;
	private final File csv;
	private final File file;
	private final List<Listener> listenerList;

	private final Mode loadMode;
	private final boolean scaled;

	/**
	 * Construct {@link GraphicsEngine} defined by {@link Builder}.
	 * 
	 * @param builder - the {@link Builder} that defines {@link GraphicsEngine}.
	 */
	private GraphicsEngine(final Builder builder) {
		file = builder.file;
		csv = builder.csv;
		colorLimit = builder.colorLimit;
		scaled = builder.scaled;
		backgroundColor = builder.backgroundColor;
		loadMode = builder.loadMode;
		thread = builder.thread;
		listenerList = builder.listenerList;
	}

	/**
	 * Create a new blueprint from {@link java.util.ArrayList
	 * ArrayList}<{@link java.io.File File}>.
	 * 
	 * @param file - the list of image files.
	 */
	private void makeNewFile(final File file) throws NullPointerException, IOException {
		final ArrayList<StitchColor> colorList;
		try {
			final String csvInput = new String(Files.readAllBytes(csv.toPath()));
			final ArrayList<ArrayList<String>> csvArray = CSVReader.read(csvInput);
			colorList = CSVReader.readColorList(csvArray);
		} catch (final IOException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("read_failed"));
			return;
		} catch (final NoSuchElementException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("rgb_missing", e.getMessage()));
			return;
		} catch (final NumberFormatException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("rgb_not_integer", e.getMessage()));
			return;
		} catch (final IllegalArgumentException e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("rgb_out_of_range", e.getMessage()));
			return;
		}

		final StitchImage stitchImage = new StitchImage();
		stitchImage.setChanged(true);
		final int resizeLength = Preferences.getInteger("resizeLength", 200);
		final BufferedImage image = ImageTools.readImage(file, scaled, resizeLength, resizeLength);
		boolean onceRunned = false;
		StitchColor toRemove = null;
		final HashMap<String, Integer> usedColorCount = new HashMap<String, Integer>();
		do {
			stitchImage.setSize(image.getWidth(), image.getHeight());
			if (onceRunned == true) {
				removeColor(colorList, toRemove);
			}
			usedColorCount.clear();

			final ColorConverter converter = new ColorConverter.Builder(image, stitchImage, colorList).setThread(thread).build();
			converter.start();

			try {
				converter.join();
			} catch (final InterruptedException e) {
				LogPrinter.print(e);
				LogPrinter.error(Resources.getString("error_has_occurred"));
				return;
			}

			stitchImage.setBackground(backgroundColor);

			for (final PixelList pixelList : stitchImage.getPixelLists()) {
				usedColorCount.put(pixelList.getColor().getName(), pixelList.getCount());
			}
			onceRunned = true;
			toRemove = ImageTools.calculateRemoveString(stitchImage, usedColorCount);
		} while (colorLimit != 0 && stitchImage.getPixelLists().size() > colorLimit);
		stitchImage.setChanged(true);
		onFinished(stitchImage);
	}

	private void onFinished(final StitchImage image) {
		for (final Listener listener : listenerList) {
			listener.onFinished(image);
		}
	}

	/**
	 * Read from saved *.dmc {@link File} to {@link StitchImage} instance.
	 * 
	 * @param file - saved *.dmc {@link File}.
	 */
	private void readFromSavedFile(final File file) throws NullPointerException, ClassNotFoundException, IOException {
		final StitchImage stitchImage = (StitchImage) Resources.readObject(file);
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
	public void run() throws RuntimeException {
		try {
			if (loadMode.equals(Mode.NEW_FILE)) {
				makeNewFile(file);
			} else if (loadMode.equals(Mode.LOAD)) {
				readFromSavedFile(file);
			}
		} catch (final Exception e) {
			LogPrinter.print(e);
			LogPrinter.error(Resources.getString("cant_read_image"));
		}
	}
}