package com.stitch.converter.model;

import java.io.Serializable;

public class StitchColor implements Serializable, Cloneable, Comparable<StitchColor> {
	private static final long serialVersionUID = 1L;

	private final int red, green, blue;
	
	private final String name;

	public StitchColor(final int red, final int green, final int blue, final String name) {
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.name = name;
	}
	
	public StitchColor(final java.awt.Color color, final String name) {
		this(color.getRed(), color.getGreen(), color.getBlue(), name);
	}
	
	public StitchColor(final javafx.scene.paint.Color color, final String name) {
		this((int)color.getRed()*255, (int)color.getGreen()*255, (int)color.getBlue()*255, name);
	}

	public java.awt.Color asAWT() {
		return new java.awt.Color(red, green, blue);
	}

	public javafx.scene.paint.Color asFX() {
		return new javafx.scene.paint.Color((double)(getRed()/255), (double)(getGreen()/255), (double)(getBlue()/255), 1d);
	}

	public double getRed() {
		return red;
	}
	
	public double getGreen() {
		return green;
	}

	public double getBlue() {
		return blue;
	}

	public String getName() {
		return name;
	}
	
	public String getColorString() {
		return "#"+String.format("%02X", red)+String.format("%02X", green)+String.format("%02X", blue);
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
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StitchColor other = (StitchColor) obj;
		if (red != other.red)
			return false;
		if (blue != other.blue)
			return false;
		if (green != other.green)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "StitchColor [red=" + red + ", green=" + green + ", blue=" + blue + ", name=" + name + "]";
	}

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
		} catch(final NumberFormatException e) {
			return this.getName().compareTo(target.getName());
		}
	}
}
