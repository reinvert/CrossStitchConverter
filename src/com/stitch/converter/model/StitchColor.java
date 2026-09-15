package com.stitch.converter.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javafx.scene.paint.Color;

public class StitchColor implements Serializable, Cloneable, Comparable<StitchColor> {
	private static final long serialVersionUID = 1L;

	private final String name;
	// 내부 저장 방식을 float(0.0f ~ 1.0f)으로 변경
	private final float red, green, blue;
	
	private transient Color fxColor = null;
	private transient String toString;

	// === 생성자 (Constructors) ===

	public StitchColor(final int rgb, final String name) {
		this((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, name);
	}

	public StitchColor(final int red, final int green, final int blue, final String name) {
		if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
			throw new IllegalStateException(new StringBuilder("Wrong input for color value: red=").append(red)
					.append(", green=").append(green).append(", blue=").append(blue).toString());
		}
		this.red = red / 255.0f;
		this.green = green / 255.0f;
		this.blue = blue / 255.0f;
		this.name = name;
	}

	// float 전용 새 생성자 추가
	public StitchColor(final float red, final float green, final float blue, final String name) {
		if (red < 0.0f || red > 1.0f || green < 0.0f || green > 1.0f || blue < 0.0f || blue > 1.0f) {
			throw new IllegalStateException(new StringBuilder("Wrong input for float color value: red=").append(red)
					.append(", green=").append(green).append(", blue=").append(blue).toString());
		}
		this.red = red;
		this.green = green;
		this.blue = blue;
		this.name = name;
	}

	public StitchColor(final Color color, final String name) {
		this((float) color.getRed(), (float) color.getGreen(), (float) color.getBlue(), name);
		this.fxColor = color;
	}

	// === JavaFX 연동 ===

	public Color asFX() {
		if (fxColor == null) {
			// float 필드를 직접 이용하여 생성 (반올림 오차 최소화)
			fxColor = new Color(red, green, blue, 1.0d);
		}
		return fxColor;
	}

	// === Getters (하위 호환성 유지) ===

	/** @return 0~255 사이의 int값 (하위 호환) */
	public int getRed() {
		return Math.round(red * 255.0f);
	}

	/** @return 0~255 사이의 int값 (하위 호환) */
	public int getGreen() {
		return Math.round(green * 255.0f);
	}

	/** @return 0~255 사이의 int값 (하위 호환) */
	public int getBlue() {
		return Math.round(blue * 255.0f);
	}

	// 신규 float 전용 Getters
	public float getRedFloat() {
		return red;
	}

	public float getGreenFloat() {
		return green;
	}

	public float getBlueFloat() {
		return blue;
	}

	public int getRGB() {
		// 괄호로 연산자 우선순위 보정
		return (getRed() << 16) | (getGreen() << 8) | getBlue();
	}

	public String getColorString() {
		return String.format("#%02X%02X%02X", getRed(), getGreen(), getBlue());
	}

	public String getName() {
		return name;
	}

	// === Override Methods ===

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
		if (obj == null || getClass() != obj.getClass())
			return false;
		final StitchColor other = (StitchColor) obj;
		// float 비교 시 Float.compare 사용 권장 (오차 방지)
		if (Float.compare(other.red, red) != 0)
			return false;
		if (Float.compare(other.green, green) != 0)
			return false;
		if (Float.compare(other.blue, blue) != 0)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(blue);
		result = prime * result + Float.floatToIntBits(green);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Float.floatToIntBits(red);
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

	// === 커스텀 직렬화 (이전 int 버전 파일과의 역직렬화 하위 호환) ===

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}