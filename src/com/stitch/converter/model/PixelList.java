package com.stitch.converter.model;

import java.io.Serializable;
import java.util.TreeSet;

public class PixelList implements Serializable {
	private static final long serialVersionUID = 1L;
	private StitchColor color;
	private int index = -1;
	private boolean isHighlighted = false, isCompleted = false;
	private final TreeSet<Pixel> pixelSet;

	public PixelList(final StitchColor color) {
		this.color = color;
		pixelSet = new TreeSet<Pixel>();
	}

	public void add(final int x, final int y) {
		final Pixel pixel = new Pixel(x, y, color);
		pixelSet.add(pixel);
	}

	public void add(final Pixel pixel) {
		pixelSet.add(pixel);
	}

	public StitchColor getColor() {
		return color;
	}

	public int getCount() {
		return pixelSet.size();
	}

	public int getIndex() {
		if (index == -1) {
			throw new IllegalArgumentException();
		}
		return index;
	}

	public TreeSet<Pixel> getPixelSet() {
		return pixelSet;
	}
	
	public boolean hasPixel(final Pixel pixel) {
		return pixelSet.contains(pixel);
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public boolean isHighlighted() {
		return isHighlighted;
	}

	public void setColor(final StitchColor color) {
		this.color = color;
		for (final Pixel pixel : pixelSet) {
			pixel.setColor(color);
		}
	}

	public void setCompleted(final boolean isCompleted) {
		this.isCompleted = isCompleted;
	}

	public void setHighlighted(final boolean isHighlighted) {
		this.isHighlighted = isHighlighted;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	@Override
	public String toString() {
		return new StringBuilder("PixelList [color=").append(color).append(", pixelList=").append(pixelSet)
				.append(", isHighlighted=").append(isHighlighted).append(", isCompleted=").append(isCompleted)
				.append(", index=").append(index).append("]").toString();
	}

}
