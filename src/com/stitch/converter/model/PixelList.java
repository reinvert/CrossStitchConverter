package com.stitch.converter.model;

import java.io.Serializable;
import java.util.TreeSet;

public class PixelList implements Serializable, Comparable<PixelList> {
	private static final long serialVersionUID = 1L;
	private final StitchColor color;
	private int index = -1;
	private boolean isHighlighted = false, isCompleted = false;
	private final TreeSet<Pixel> pixelSet;

	public PixelList(final StitchColor color) {
		this.color = color;
		pixelSet = new TreeSet<Pixel>();
	}

	public void add(final int x, final int y) {
		final Pixel pixel = new Pixel(x, y, color);
		if (pixelSet.contains(pixel)) {
			return;
		}
		pixelSet.add(pixel);
	}

	public void add(final Pixel pixel) {
		if (pixelSet.contains(pixel)) {
			return;
		}
		pixelSet.add(pixel);
	}

	@Override
	public int compareTo(final PixelList arg0) {
		return this.getColor().compareTo(arg0.getColor());
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final PixelList other = (PixelList) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		return true;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((color == null) ? 0 : color.hashCode());
		result = prime * result + index;
		result = prime * result + (isCompleted ? 1231 : 1237);
		result = prime * result + (isHighlighted ? 1231 : 1237);
		result = prime * result + ((pixelSet == null) ? 0 : pixelSet.hashCode());
		return result;
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
