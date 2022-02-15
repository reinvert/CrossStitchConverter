package com.stitch.converter;

import java.util.ArrayList;

import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.StitchColor;

import javafx.scene.paint.Color;

public class DitheredColorConverter extends ColorConverter {
	
	private int convertMode = GraphicsEngine.FLOYD;
	private boolean isGammaBased = true;

	DitheredColorConverter(Builder builder) {
		super(builder);
		convertMode = builder.convertMode;
		isGammaBased = builder.isGammaBased;
	}
	
	@Override
	public void run() {
		final ArrayList<ArrayList<Color>> doubleValueImage = new ArrayList<>();
		for(int x=0; x<image.getWidth(); x++) {
			final ArrayList<Color> row = new ArrayList<>();
			for(int y=0; y<image.getHeight(); y++) {
				row.add(gammaToLinear(new StitchColor(image.getRGB(x, y), "").asFX()));
			}
			doubleValueImage.add(row);
		}
		

		for(int y=0; y<image.getHeight(); y++) {
			for(int x=0; x<image.getWidth(); x++) {
				final Color color = doubleValueImage.get(x).get(y);
				double difference = Double.MAX_VALUE;
				StitchColor outputColor = null;
				double calculatedDifference = 0;

				for (final StitchColor listColor : colorList) {
					calculatedDifference = ImageTools.calculateDifference(linearToGamma(color), listColor.asFX());
					if (calculatedDifference < difference) {
						outputColor = listColor;
						difference = calculatedDifference;
					}
				}
				final Pixel pixel = new Pixel(x, y, outputColor);
				image.setRGB(x,  y, pixel.getColor().getRGB());
				stitchImage.add(pixel);
				
				final Color outputLinearColor = gammaToLinear(outputColor.asFX());

				final double redDifference = color.getRed() - outputLinearColor.getRed();
				final double greenDifference = color.getGreen() - outputLinearColor.getGreen();
				final double blueDifference = color.getBlue() - outputLinearColor.getBlue();
				DifferenceCalc calc = new DifferenceCalc(redDifference, greenDifference, blueDifference);
				if(convertMode == GraphicsEngine.FLOYD) {
					try {
						final int nextX = x+1, nextY = y;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 7d/16d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x-1, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 3d/16d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 5d/16d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x+1, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 1d/16d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
				} else if(convertMode == GraphicsEngine.SIERRA) {
					try {
						final int nextX = x+1, nextY = y;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 5d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x+2, nextY = y;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 3d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x-2, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 2d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x-1, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 4d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 5d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x+1, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 4d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x+2, nextY = y+1;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 2d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x-1, nextY = y+2;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 2d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x, nextY = y+2;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 3d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
					try {
						final int nextX = x+1, nextY = y+2;
						final Color nextColor = doubleValueImage.get(nextX).get(nextY);
						final double factor = 2d/32d;
						doubleValueImage.get(nextX).set(nextY, calc.calc(nextColor, factor));
					} catch(final IndexOutOfBoundsException e) { }
				}
			}
		}
	}
	
	private Color gammaToLinear(Color color) {
		final double red = gammaToLinear(color.getRed());
		final double green = gammaToLinear(color.getGreen());
		final double blue = gammaToLinear(color.getBlue());
		return new Color(red, green, blue, 1d);
	}
	
	private Color linearToGamma(Color color) {
		final double red = linearToGamma(color.getRed());
		final double green = linearToGamma(color.getGreen());
		final double blue = linearToGamma(color.getBlue());
		return new Color(red, green, blue, 1d);
	}
	
	private double linearToGamma(double value) {
		if(isGammaBased == true) {
			if(value <= 0.0031308d) {
				return value * 12.92d;
			} else {
				return Math.pow(value, 1d / 2.4d) * 1.055 - 0.055;
			}
		} else {
			return value;
		}
	}
	
	private double gammaToLinear(double value) {
		if(isGammaBased == true) {
			if(value <= 0.04045d) {
				return value / 12.92d;
			} else {
				return Math.pow((value + 0.055d) / 1.055d, 2.4d);
			}
		} else {
			return value;
		}
	}
	
	private class DifferenceCalc{
		final double red, green, blue;
		DifferenceCalc(final double red, final double green, final double blue){
			this.red = red;
			this.green = green;
			this.blue = blue;
		};

		Color calc(Color nextColor, final double factor) {
			final double red = Math.max(0d, Math.min(1d, nextColor.getRed() + this.red * factor));
			final double green = Math.max(0d, Math.min(1d, nextColor.getGreen() + this.green * factor));
			final double blue = Math.max(0d, Math.min(1d, nextColor.getBlue() + this.blue * factor));
			return new Color(red, green, blue, 1d);
		}
	}
}
