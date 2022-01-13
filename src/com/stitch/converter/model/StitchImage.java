package com.stitch.converter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class StitchImage implements Serializable {
	private static final long serialVersionUID = 1L;

	private final TreeMap<StitchColor, Integer> alternateColors;

	private StitchColor background = new StitchColor(0xFFFFFF, "");
	private transient ArrayList<StitchColor> colorList = null;

	private transient WritableImage fxImage = null;

	private transient boolean isChanged = false;

	private boolean numberVisible = true;
	private final SortedSet<PixelList> pixelListSet;
	private double width = -1, height = -1;

	public StitchImage() {
		pixelListSet = new TreeSet<PixelList>();
		alternateColors = new TreeMap<StitchColor, Integer>();
	}

	public void add(final Pixel pixel) {
		final PixelList pixelList = new PixelList(pixel.getColor());
		if (pixelListSet.contains(pixelList)) {
			getPixelListByColor(pixel.getColor()).add(pixel);
		} else {
			pixelList.add(pixel);
			pixelListSet.add(pixelList);
		}
	}

	public void addAlternateColor(final StitchColor color) {
		if (alternateColors.containsKey(color)) {
			alternateColors.put(color, alternateColors.get(color) + 1);
		} else {
			alternateColors.put(color, 1);
		}
	}

	public void calculateSize() {
		int width = -1, height = -1;
		for (final PixelList pixelList : pixelListSet) {
			for (final Pixel pixel : pixelList.getPixelSet()) {
				int x = pixel.getX();
				int y = pixel.getY();
				if (x > width) {
					width = x;
				}
				if (y > height) {
					height = y;
				}
			}
		}
		this.width = width + 1;
		this.height = height + 1;
	}

	public List<StitchColor> getAlternate() {
		final List<Entry<StitchColor, Integer>> list = new ArrayList<>(alternateColors.entrySet());
		list.sort(Entry.comparingByValue());
		final List<StitchColor> output = new ArrayList<>();
		for (int i = list.size() - 1; i != 0; i--) {
			output.add(list.get(i).getKey());
		}
		return output;
	}

	public StitchColor getBackground() {
		return background;
	}

	public ArrayList<StitchColor> getColorList() {
		if (colorList == null) {
			colorList = new ArrayList<>();
			for (final PixelList pixelList : pixelListSet) {
				colorList.add(pixelList.getColor());
			}
		}
		return colorList;
	}

	public WritableImage getFXImage() {
		if (fxImage == null) {
			calculateSize();
			fxImage = new WritableImage((int) width, (int) height);
			final PixelWriter pixelWriter = fxImage.getPixelWriter();
			for (final PixelList pixelList : pixelListSet) {
				final Color color = pixelList.getColor().asFX();
				for (final Pixel pixel : pixelList.getPixelSet()) {
					try {
						pixelWriter.setColor(pixel.getX(), pixel.getY(), color);
					} catch(IndexOutOfBoundsException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return fxImage;
	}

	public double getHeight() {
		if (width == -1 || height == -1) {
			calculateSize();
		}
		return height;
	}

	public PixelList getPixelListByColor(final StitchColor color) {
		for (final PixelList pixelList : pixelListSet) {
			if (pixelList.getColor().equals(color)) {
				return pixelList;
			}
		}
		throw new NoSuchElementException(String.format("No Such PixelList: %s", color));
	}

	public PixelList getPixelListByName(final String name) {
		for (final PixelList pixelList : pixelListSet) {
			if (pixelList.getColor().getName().equals(name)) {
				return pixelList;
			}
		}
		throw new NoSuchElementException(String.format("No Such PixelList: %s", name));
	}

	public Collection<PixelList> getPixelLists() {
		int index = 0;
		for (final PixelList pixelList : pixelListSet) {
			pixelList.setIndex(index++);
		}
		return pixelListSet;
	}

	public double getWidth() {
		if (width == -1 || height == -1) {
			calculateSize();
		}
		return width;
	}

	public boolean isChanged() {
		return isChanged;
	}

	public boolean isNumberVisible() {
		return numberVisible;
	}

	public void removeAlternate(final StitchColor color) {
		alternateColors.remove(color);
	}

	public void setBackground(final StitchColor background) {
		this.background = background;
	}

	public void setChanged(final boolean changeStatus) {
		isChanged = changeStatus;
	}

	public void setNumberVisible(final boolean numberVisible) {
		this.numberVisible = numberVisible;
	}

	public void setSize(final int width, final int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return new StringBuilder("StitchImage [background=").append(background).append(", pixelListSet=")
				.append(pixelListSet).append(", width=").append(width).append(", height=").append(height)
				.append(", numberVisible=").append(numberVisible).append("]").toString();
	}
}
