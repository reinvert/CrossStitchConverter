package com.stitch.converter.model;

import java.io.Serializable;

public final class Pixel implements Serializable, Comparable<Pixel> {
	private static final long serialVersionUID = 1L;
	private StitchColor color;
	private final int x, y;
	private transient String toString;

	public Pixel(final int x, final int y, final StitchColor color) {
		if(x < 0 || y < 0) {
			throw new IllegalStateException(new StringBuilder("Wrong pixel coordinate: x=").append(x).append(", y=").append(y).toString());
		}
		if(color == null) {
			throw new NullPointerException("StitchColor is null");
		}
		this.x = x;
		this.y = y;
		this.color = color;
	}

	@Override
	public int compareTo(final Pixel other) {
		if (other == null)
			throw new NullPointerException();
		int comparison = x - other.getX();
		if (comparison == 0) {
			comparison = y - other.getY();
		}
		return comparison;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		final Pixel other = (Pixel) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	public StitchColor getColor() {
		return color;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
		return result;
	}

	public void setColor(final StitchColor color) {
		this.color = color;
	}

	@Override
	public String toString() {
		if (toString == null) {
			toString = new StringBuilder("Pixel [color=").append(color).append(", x=").append(x).append(", y=")
					.append(y).append("]").toString();
		}
		return toString;
	}
}
