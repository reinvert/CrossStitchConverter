package com.stitch.converter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import com.stitch.converter.model.StitchImage;

import javafx.scene.paint.Color;

import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;

/**
 * Creates Blueprint image and used thread list.
 * 
 * @author Reinvert
 */
public class GraphicsEngine extends CustomThread {
	/**
	 * Defines {@link GraphicsEngine}.
	 * 
	 * @author Reinvert
	 */
	public static class Builder {
		private final File csv;
		private final File file;

		private final ArrayList<MessageListener> listenerArray = new ArrayList<MessageListener>();
		private Mode loadMode = Mode.NEW_FILE;
		private int maximumColor = -1, thread;
		private boolean resize = false;
		private Color backgroundColor = Color.WHITE;

		/**
		 * Builder of {@link GraphicsEngine}.
		 * 
		 * @param csv
		 *            - the csv {@link java.io.File File} containing color information.
		 * @param file
		 *            - the image {@link java.io.File File} to convert.
		 */
		public Builder(final File csv, final File file) {
			this.file = file;
			this.csv = csv;
			thread = Runtime.getRuntime().availableProcessors() + 1;
		}

		/**
		 * Sets the {@link MessageListener} to receive messages.
		 * 
		 * @param listener
		 *            - the {@link MessageListener} to receive messages.
		 * @return this instance.
		 */
		public Builder addListener(final MessageListener listener) {
			listenerArray.add(listener);
			return this;
		}

		/**
		 * Set whether or not to resize the image.
		 * 
		 * @param resize
		 *            - the boolean whether or not to resize the images.
		 * @return this instance.
		 */
		public Builder setResize(final boolean resize) {
			this.resize = resize;
			return this;
		}

		/**
		 * Set background {@link javafx.scene.paint.Color Color} of image.
		 * 
		 * @param backgroundColor
		 *            - the background {@link javafx.scene.paint.Color Color} of image.
		 * @return this instance.
		 */
		public Builder setBackground(final Color backgroundColor) {
			this.backgroundColor = backgroundColor;
			return this;
		}

		/**
		 * Set the maximum number of colors to be used.
		 * 
		 * @param maximumColor
		 *            - maximum number of colors to be used.
		 * @return this instance.
		 */
		public Builder setMaximumColor(final int maximumColor) {
			this.maximumColor = maximumColor;
			return this;
		}

		/**
		 * Sets whether it is a new file or an existing one.
		 * 
		 * @param loadMode
		 *            - the {@link Mode} whether it is a new file or an existing one.
		 * @return this instance.
		 */
		public Builder setMode(final Mode loadMode) {
			this.loadMode = loadMode;
			return this;
		}

		/**
		 * Sets the number of {@link java.lang.Thread Threads} to work with.
		 * 
		 * @param thread
		 *            - the number of {@link java.lang.Thread Threads} to work with.
		 * @return this instance.
		 */
		public Builder setThread(final int thread) {
			this.thread = thread;
			return this;
		}

		/**
		 * Build {@link GraphicsEngine} instance.
		 * 
		 * @return the {@link GraphicsEngine} instance.
		 */
		public GraphicsEngine build() {
			return new GraphicsEngine(this);
		}

	}

	/**
	 * the {@link Mode} whether it is a new file or an existing one.
	 * @author Reinvert
	 *
	 */
	public enum Mode {
		LOAD, NEW_FILE
	}
	
	private ArrayList<StitchColor> colorList;
	private StitchImage convertedImage;
	private final File csv;
	private final File file;
	private BufferedImage image;
	private final Color backgroundColor;
	private final Mode loadMode;

	private final int maxColor, thread;
	private final boolean scaled;

	private long totalTime;

	private final HashMap<String, Integer> usedColorCount = new HashMap<String, Integer>();

	/**
	 * Construct {@link GraphicsEngine} defined by {@link Builder}.
	 * 
	 * @param builder
	 *            - the {@link Builder} that defines {@link GraphicsEngine}.
	 */
	private GraphicsEngine(final Builder builder) {
		totalTime = System.currentTimeMillis();

		file = builder.file;
		csv = builder.csv;
		maxColor = builder.maximumColor;
		scaled = builder.resize;
		backgroundColor = builder.backgroundColor;
		loadMode = builder.loadMode;
		thread = builder.thread;

		addListener(builder.listenerArray);
	}

	/**
	 * Add count to {@link java.util.HashMap HashMap}. It is used to determine which
	 * {@link java.awt.Color Color} is used the most.
	 * 
	 * @param key
	 *            - the name of used {@link java.awt.Color Color}.
	 */
	private synchronized void addCount(final String key) {
		try {
			usedColorCount.put(key, usedColorCount.get(key) + 1);
		} catch (final NullPointerException e) {
			usedColorCount.put(key, 1);
		}
	}

	/**
	 * Draw converted pixel to image.
	 * 
	 * @param pixel
	 *            - the {@link Pixel} contains x, y, {@link java.awt.Color Color}
	 *            value.
	 * @param convertedImage
	 *            - the {@link StitchImage} contains name of {@link java.awt.Color
	 *            Color}.
	 */
	private void drawPixelToImage(final Pixel pixel, final StitchImage convertedImage) {
		final StitchColor targetColor = pixel.getColor();
		final int x = pixel.getX(), y = pixel.getY();
		image.setRGB(x, y, targetColor.asAWT().getRGB());
		addCount(targetColor.getName());
	}

	/**
	 * Gets the {@link StitchImage} used for the {@link Blueprint}.
	 * 
	 * @return the {@link StitchImage}.
	 */
	public StitchImage getConvertedImage() {
		return convertedImage;
	}

	/**
	 * Create a new blueprint from {@link java.util.ArrayList
	 * ArrayList}<{@link java.io.File File}>.
	 * 
	 * @param file
	 *            - the list of image files.
	 */
	private void makeNewFile(final File file) {
		try {
			final String csvInput = new String(Files.readAllBytes(csv.toPath()));
			final ArrayList<ArrayList<String>> csvArray = CSVReader.read(csvInput);
			colorList = CSVReader.readColorList(csvArray);
		} catch (final IOException e) {
			LogPrinter.print(Resources.getString("read_failed"));
			return;
		} catch (final NoSuchElementException e) {
			LogPrinter.print(Resources.getString("rgb_missing"));
			return;
		} catch (final NumberFormatException e) {
			LogPrinter.print(Resources.getString("rgb_not_integer"));
			return;
		} catch (final IllegalArgumentException e) {
			LogPrinter.print(Resources.getString("rgb_out_of_range"));
			return;
		}
		long time = System.currentTimeMillis();

		convertedImage = new StitchImage();
		LogPrinter.print(Resources.getString("load_file", file.getPath()));
		try {
			int resizeLength = Preferences.getInteger("resizeLength", 200);
			image = ImageTools.readImage(file, scaled, resizeLength, resizeLength);
		} catch (final Exception e) {
			e.printStackTrace();
			LogPrinter.print(Resources.getString("cant_read_image"));
			return;
		}
		boolean onceRunned = false;
		String removestring = "";
		Collection<PixelList> pixelListArray = null;
		do {
			convertedImage.setSize(image.getWidth(), image.getHeight());
			if (onceRunned) {
				removeColor(colorList, removestring);
			}
			usedColorCount.clear();

			final ColorConverter converter = new ColorConverter.Builder(image, convertedImage, colorList)
					.setThread(thread).build();
			converter.addListener(new MessageListener() {

				@Override
				public void onTaskCompleted(final StitchImage stitchImage, final double scale) {
					stitchImage.setBackground(new StitchColor(backgroundColor, ""));
					sendGraphics(stitchImage, scale);
				}

			});
			converter.start();
			/*
			 * final MessageListener blueprintListener = new MessageListener() {
			 * 
			 * @Override public void onGraphicsChanged(final Canvas canvas) {
			 * sendGraphics(canvas); }
			 * 
			 * @Override public void onStringReceived(String str) { sendString(str); }
			 * 
			 * @Override public void onTaskCompleted() {
			 * 
			 * } }; final String imageFileName = resources.getString("blueprint_image",
			 * file.getPath().replaceFirst("[.][^.]+$", "")); final File imageFile = new
			 * File(imageFileName); try { blueprint = new Blueprint.Builder(convertedWidth,
			 * convertedHeight, convertedImage, imageFile)
			 * .setListener(blueprintListener).setStyle(style).build(); } catch (final
			 * OutOfMemoryError e) { sendString(resources.getString("out_of_memory"));
			 * return; } blueprint.start();
			 */
			try {
				converter.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			pixelListArray = convertedImage.getPixelLists();

			for (final PixelList pixelList : pixelListArray) {
				final TreeSet<Pixel> pixelSet = pixelList.getPixelSet();
				for (final Pixel pixel : pixelSet) {
					drawPixelToImage(pixel, convertedImage);
				}
			}
			onceRunned = true;
			removestring = ImageTools.calculateRemoveString(convertedImage, usedColorCount);
		} while (maxColor != 0 && convertedImage.getPixelLists().size() > maxColor);
		/*
		 * try { blueprint.join(); } catch (InterruptedException e) { }
		 */
		try {
			writeFiles(file);
		} catch (IOException e) {
			LogPrinter.print(e.getMessage());
			return;
		}
		time = System.currentTimeMillis() - time;
		LogPrinter.print(Resources.getString("task_completed", file.getName(), time));
	}

	/**
	 * Read from saved *.dmc file to ConvertedImage instance.
	 * 
	 * @param file
	 *            - saved *.dmc file.
	 */
	private void readFromSavedFile(final File file) {
		convertedImage = new StitchImage();
		ObjectInputStream ois = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			convertedImage = (StitchImage) ois.readObject();
		} catch (final Exception e) {
			e.printStackTrace();
			LogPrinter.print(Resources.getString("read_failed", Resources.getString("dmc")));
			return;
		} finally {
			try {
				ois.close();
				fis.close();
			} catch (final Exception e) {
			}
		}

		if (this.loadMode == Mode.NEW_FILE) {
			for (final PixelList pixelList : convertedImage.getPixelLists()) {
				for (final Pixel pixel : pixelList.getPixelSet()) {
					drawPixelToImage(pixel, convertedImage);
				}
			}
		}

		totalTime = System.currentTimeMillis() - totalTime;
		LogPrinter.print(Resources.getString("task_completed", Resources.getString("load"), totalTime));
		sendGraphics(convertedImage, 15.0);
	}

	/**
	 * Delete the color thread from the list. Only works if maximum color is
	 * limited.
	 * 
	 * @param colorName
	 *            - the {@link java.lang.String String} Array containing the names
	 *            of all threads.
	 * @param colorList
	 *            - the {@link java.awt.color Color} Array containing the colors of
	 *            all threads.
	 * @param toRemove
	 *            - the name of the thread to delete.
	 */
	private void removeColor(final ArrayList<StitchColor> colorList, final String toRemove) {
		int num;
		for (num = 0; num < colorList.size(); num++) {
			if (colorList.get(num).getName().equals(toRemove)) {
				return;
			}
		}
		colorList.remove(num);
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

	/**
	 * Write result image, used thread list on text, blueprint {@link java.io.File
	 * File}. The name of each {@link java.io.File File} is defined by the language
	 * xml file.(ex. en-us, ko-kr)
	 * 
	 * @param file
	 *            - original image {@link java.io.File File}.
	 * @throws IOException
	 *             occurs when there is a problem writing the {@link java.io.File
	 *             File}.
	 */
	public void writeFiles(final File file) throws IOException {
		// final String fileName = file.getPath().replace("[.][^.]+$", "")+".dmc";
		final String fileName = file.getParent() + File.separator
				+ file.getName().substring(0, file.getName().lastIndexOf(".")) + ".dmc";
		try {
			Resources.write(fileName, convertedImage);
			LogPrinter.print(Resources.getString("file_saved", fileName, Resources.getString("dmc_file")));
		} catch (final IOException e) {
			e.printStackTrace();
			throw new IOException(Resources.getString("save_failed", Resources.getString("dmc_file")));
		}
	}
}