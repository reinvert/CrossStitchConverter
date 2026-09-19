package com.stitch.converter.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.io.Serializable;

import javafx.scene.paint.Color;

public class StitchColor implements Serializable, Cloneable, Comparable<StitchColor> {

	private static final long serialVersionUID = 1L;

	/*
	 * 직렬화 호환성을 위한 필드 정의.
	 *
	 * red / green / blue:
	 *   구버전에서 int로 저장되었던 필드.
	 *   새 버전에서도 직렬화 포맷상 int로 유지한다.
	 *
	 * redFloat / greenFloat / blueFloat:
	 *   새 버전에서 사용하는 0.0f ~ 1.0f 값.
	 */
	private static final ObjectStreamField[] serialPersistentFields = {
		new ObjectStreamField("name", String.class),

		// 구버전 호환용
		new ObjectStreamField("red", int.class),
		new ObjectStreamField("green", int.class),
		new ObjectStreamField("blue", int.class),

		// 신버전용
		new ObjectStreamField("redFloat", float.class),
		new ObjectStreamField("greenFloat", float.class),
		new ObjectStreamField("blueFloat", float.class)
	};

	/*
	 * 실제 내부 저장값.
	 * 0.0f ~ 1.0f
	 */
	private String name;
	private float red;
	private float green;
	private float blue;

	private transient Color fxColor = null;
	private transient String toString;

	// === Constructors ===

	public StitchColor(final int rgb, final String name) {
		this(
			(rgb >> 16) & 0xFF,
			(rgb >> 8) & 0xFF,
			rgb & 0xFF,
			name
		);
	}

	public StitchColor(
			final int red,
			final int green,
			final int blue,
			final String name) {

		if (red < 0 || red > 255 ||
			green < 0 || green > 255 ||
			blue < 0 || blue > 255) {

			throw new IllegalStateException(
				new StringBuilder("Wrong input for color value: red=")
					.append(red)
					.append(", green=")
					.append(green)
					.append(", blue=")
					.append(blue)
					.toString()
			);
		}

		this.red = red / 255.0f;
		this.green = green / 255.0f;
		this.blue = blue / 255.0f;
		this.name = name;
	}

	public StitchColor(
			final float red,
			final float green,
			final float blue,
			final String name) {

		if (red < 0.0f || red > 1.0f ||
			green < 0.0f || green > 1.0f ||
			blue < 0.0f || blue > 1.0f) {

			throw new IllegalStateException(
				new StringBuilder("Wrong input for float color value: red=")
					.append(red)
					.append(", green=")
					.append(green)
					.append(", blue=")
					.append(blue)
					.toString()
			);
		}

		this.red = red;
		this.green = green;
		this.blue = blue;
		this.name = name;
	}

	public StitchColor(final Color color, final String name) {
		this(
			(float) color.getRed(),
			(float) color.getGreen(),
			(float) color.getBlue(),
			name
		);

		this.fxColor = color;
	}

	// === JavaFX ===

	public Color asFX() {
		if (fxColor == null) {
			fxColor = new Color(red, green, blue, 1.0d);
		}
		return fxColor;
	}

	// === Getters ===

	/**
	 * @return 0 ~ 255 범위의 int 값
	 */
	public int getRed() {
		return Math.round(red * 255.0f);
	}

	/**
	 * @return 0 ~ 255 범위의 int 값
	 */
	public int getGreen() {
		return Math.round(green * 255.0f);
	}

	/**
	 * @return 0 ~ 255 범위의 int 값
	 */
	public int getBlue() {
		return Math.round(blue * 255.0f);
	}

	/**
	 * @return 0.0f ~ 1.0f 범위의 float 값
	 */
	public float getRedFloat() {
		return red;
	}

	/**
	 * @return 0.0f ~ 1.0f 범위의 float 값
	 */
	public float getGreenFloat() {
		return green;
	}

	/**
	 * @return 0.0f ~ 1.0f 범위의 float 값
	 */
	public float getBlueFloat() {
		return blue;
	}

	public int getRGB() {
		return (getRed() << 16)
			| (getGreen() << 8)
			| getBlue();
	}

	public String getColorString() {
		return String.format(
			"#%02X%02X%02X",
			getRed(),
			getGreen(),
			getBlue()
		);
	}

	public String getName() {
		return name;
	}

	// === Serialization ===

	/**
	 * 직렬화 시:
	 *
	 * 1. 기존 버전과 호환되도록 red/green/blue를 int로 저장
	 * 2. 새로운 버전에서는 float 값도 별도로 저장
	 */
	private void writeObject(final ObjectOutputStream out) throws IOException {
		final ObjectOutputStream.PutField fields = out.putFields();

		fields.put("name", name);

		// 구버전 호환용 int 값
		fields.put("red", getRed());
		fields.put("green", getGreen());
		fields.put("blue", getBlue());

		// 신버전용 float 값
		fields.put("redFloat", red);
		fields.put("greenFloat", green);
		fields.put("blueFloat", blue);

		out.writeFields();
	}

	/**
	 * 역직렬화 시:
	 *
	 * - 신버전 파일:
	 *     redFloat / greenFloat / blueFloat 사용
	 *
	 * - 구버전 파일:
	 *     red / green / blue(int)를 읽어서 float로 변환
	 */
	private void readObject(
			final ObjectInputStream in)
			throws IOException, ClassNotFoundException {

		final ObjectInputStream.GetField fields = in.readFields();

		name = (String) fields.get("name", null);

		if (!fields.defaulted("redFloat")) {
			// 신버전 파일
			red = fields.get("redFloat", 0.0f);
			green = fields.get("greenFloat", 0.0f);
			blue = fields.get("blueFloat", 0.0f);

		} else {
			// 구버전 파일
			red = fields.get("red", 0) / 255.0f;
			green = fields.get("green", 0) / 255.0f;
			blue = fields.get("blue", 0) / 255.0f;
		}

		// transient 필드는 직렬화되지 않으므로 초기화
		fxColor = null;
		toString = null;
	}

	// === Object / Comparable ===

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
			final int integerName = Integer.parseInt(this.getName());
			final int targetIntegerName = Integer.parseInt(target.getName());

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

		return Float.compare(red, other.red) == 0
			&& Float.compare(green, other.green) == 0
			&& Float.compare(blue, other.blue) == 0
			&& (name == null
				? other.name == null
				: name.equals(other.name));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		result = prime * result + Float.floatToIntBits(blue);
		result = prime * result + Float.floatToIntBits(green);
		result = prime * result + (name == null ? 0 : name.hashCode());
		result = prime * result + Float.floatToIntBits(red);

		return result;
	}

	@Override
	public String toString() {
		if (toString == null) {
			toString = new StringBuilder("StitchColor [red=")
				.append(red)
				.append(", green=")
				.append(green)
				.append(", blue=")
				.append(blue)
				.append(", name=")
				.append(name)
				.append("]")
				.toString();
		}

		return toString;
	}
}