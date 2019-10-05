package com.stitch.converter.model;

import java.io.Serializable;
import java.util.TreeSet;

public class PixelList implements Serializable, Comparable<PixelList> {
	private static final long serialVersionUID = 1L;
	private final StitchColor color;
	private final TreeSet<Pixel> pixelSet;
	private boolean isHighlighted = false, isCompleted = false;
	private int index = -1;

	public PixelList(final StitchColor color) {
		this.color = color;
		pixelSet = new TreeSet<Pixel>();
	}

	public void add(final int x, final int y) {
		if(pixelSet.contains(new Pixel(x, y, null))) {
			pixelSet.remove(new Pixel(x, y, null));
		}
		pixelSet.add(new Pixel(x, y, color));
	}

	public void add(final Pixel pixel) {
		if(pixelSet.contains(pixel)) {
			pixelSet.remove(pixel);
		}
		pixelSet.add(pixel);
	}

	@Override
	public int compareTo(final PixelList arg0) {
		return this.getColor().compareTo(arg0.getColor());
	}

	public StitchColor getColor() {
		return color;
	}

	public TreeSet<Pixel> getPixelSet() {
		return pixelSet;
	}

	public boolean hasPixel(final Pixel pixel) {
		return pixelSet.contains(pixel);
	}

	public boolean isHighlighted() {
		return isHighlighted;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public int getIndex() {
		if (index == -1) {
			throw new IllegalArgumentException();
		}
		return index;
	}

	public void setHighlighted(boolean isHighlighted) {
		this.isHighlighted = isHighlighted;
	}

	public void setCompleted(boolean isCompleted) {
		this.isCompleted = isCompleted;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	public void setColor(final StitchColor color) {
		for (final Pixel pixel : pixelSet) {
			pixel.setColor(color);
		}
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PixelList other = (PixelList) obj;
		if (color == null) {
			if (other.color != null)
				return false;
		} else if (!color.equals(other.color))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PixelList [color=" + color + ", pixelList=" + pixelSet + ", isHighlighted=" + isHighlighted
				+ ", isCompleted=" + isCompleted + ", index=" + index + "]";
	}

}
