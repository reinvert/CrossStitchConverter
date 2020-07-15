package com.stitch.converter.model;

import java.io.Serializable;

import javafx.scene.paint.Color;

public class StitchColor implements Serializable, Cloneable, Comparable<StitchColor> {
	private static final long serialVersionUID = 1L;

	private final String name;
	private final int red, green, blue;
	private transient Color fxColor = null;
	private String toString;

	public StitchColor(final int rgb, final String name) {
		red = (rgb >> 16) & 0xFF;
		green = (rgb >> 8) & 0xFF;
		blue = (rgb >> 0) & 0xFF;
		this.name = name;
	}

	public StitchColor(final int red, final int green, final int blue, final String name) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.name = name;
	}

	public StitchColor(final Color color, final String name) {
		this((int) color.getRed() * 255, (int) color.getGreen() * 255, (int) color.getBlue() * 255, name);
		fxColor = color;
	}

	public Color asFX() {
		if (fxColor == null) {
			fxColor = new Color(getRed() / 255d, getGreen() / 255d, getBlue() / 255d, 1d);
		}
		return fxColor;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	@Override
	public int compareTo(final StitchColor target) {
		try {
			int integerName = Integer.parseInt(this.getName());
			int targetIntegerName = Integer.parseInt(target.getName());
			return Integer.compare(integerName, targetIntegerName);
		} catch (final NumberFormatException e) {
			return this.getName().compareTo(target.getName());
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final StitchColor other = (StitchColor) obj;
		if (red != other.red)
			return false;
		if (blue != other.blue)
			return false;
		if (green != other.green)
			return false;
		return true;
	}

	public int getRGB() {
		return red << 16 + green << 8 + blue;
	}

	public int getBlue() {
		return blue;
	}

	public String getColorString() {
		return String.format("#%02X%02X%02X", red, green, blue);
	}

	public int getGreen() {
		return green;
	}

	public String getName() {
		return name;
	}

	public int getRed() {
		return red;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + blue;
		result = prime * result + green;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + red;
		return result;
	}

	@Override
	public String toString() {
		if (toString == null) {
			toString = new StringBuilder("StitchColor [red=").append(red).append(", green=").append(green)
					.append(", blue=").append(blue).append(", name=").append(name).append("]").toString();
		}
		return toString;
	}
}
