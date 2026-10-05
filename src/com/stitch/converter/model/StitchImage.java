package com.stitch.converter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
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

	private transient WritableImage fxImage = null;

	private transient boolean isChanged = false;

	private boolean numberVisible = true;
	private final SortedSet<PixelList> pixelListSet;
	private double width = -1, height = -1;

	public StitchImage() {
	    pixelListSet = new TreeSet<>(
	        Comparator.comparing(PixelList::getColor)
	    );
	    alternateColors = new TreeMap<StitchColor, Integer>();
	}
	
	private transient boolean indexChanged = true;
	
	public void add(final Pixel pixel) {
	    PixelList pixelList = null;

	    for (final PixelList current : pixelListSet) {
	        if (current.getColor().equals(pixel.getColor())) {
	            pixelList = current;
	            break;
	        }
	    }

	    if (pixelList == null) {
	        pixelList = new PixelList(pixel.getColor());
	        pixelListSet.add(pixelList);
			indexChanged = true;
	    }

	    pixelList.add(pixel);
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
	    final List<Entry<StitchColor, Integer>> list =
	            new ArrayList<>(alternateColors.entrySet());

	    list.sort(Entry.comparingByValue());

	    final List<StitchColor> output = new ArrayList<>(list.size());

	    for (int i = list.size() - 1; i >= 0; i--) {
	        output.add(list.get(i).getKey());
	    }

	    return output;
	}

	public StitchColor getBackground() {
		return background;
	}

	public WritableImage getFXImage() {
		if (fxImage == null) {
			calculateSize();
			fxImage = new WritableImage((int) width, (int) height);
			final PixelWriter pixelWriter = fxImage.getPixelWriter();
			for (final PixelList pixelList : pixelListSet) {
				final Color color = pixelList.getColor().asFX();
				for (final Pixel pixel : pixelList.getPixelSet()) {
					pixelWriter.setColor(pixel.getX(), pixel.getY(), color);
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

	public Collection<PixelList> getPixelLists() {
		if(!indexChanged) {
			return pixelListSet;
		}
		int index = 0;
		for (final PixelList pixelList : pixelListSet) {
			pixelList.setIndex(index++);
		}
		indexChanged = false;
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
}
