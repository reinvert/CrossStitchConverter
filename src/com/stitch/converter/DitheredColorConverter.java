package com.stitch.converter;

import java.util.ArrayList;
import java.util.List;

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
    	final int imageSize = image.getWidth()*image.getHeight();
    	//final int updateInterval = 1000;
    	int count=0;
        List<List<Color>> doubleValueImage = new ArrayList<>();
        
        for (int x = 0; x < image.getWidth(); x++) {
            List<Color> row = new ArrayList<>();
            for (int y = 0; y < image.getHeight(); y++) {
                row.add(gammaToLinear(new StitchColor(image.getRGB(x, y), "").asFX(), isGammaBased));
            }
            doubleValueImage.add(row);
        }
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = doubleValueImage.get(x).get(y);
                StitchColor outputColor = findClosestColor(color);

                Pixel pixel = new Pixel(x, y, outputColor);
                image.setRGB(x, y, pixel.getColor().getRGB());
                stitchImage.add(pixel);

                Color outputLinearColor = gammaToLinear(outputColor.asFX(), isGammaBased);

                double redDifference = color.getRed() - outputLinearColor.getRed();
                double greenDifference = color.getGreen() - outputLinearColor.getGreen();
                double blueDifference = color.getBlue() - outputLinearColor.getBlue();

                DifferenceCalc calc = new DifferenceCalc(redDifference, greenDifference, blueDifference);
                applyErrorDiffusion(doubleValueImage, x, y, calc);
                progressListener.onProgress((double) ++count / imageSize, Resources.getString("conversion_processing_colors"));
            }
        }
    }

    private StitchColor findClosestColor(Color color) {
        double difference = Double.MAX_VALUE;
        StitchColor closestColor = null;
        for (StitchColor listColor : colorList) {
            double calculatedDifference = ImageTools.calculateDifference(linearToGamma(color, isGammaBased), listColor.asFX());
            if (calculatedDifference < difference) {
                closestColor = listColor;
                difference = calculatedDifference;
            }
        }
        return closestColor;
    }

    private void applyErrorDiffusion(List<List<Color>> image, int x, int y, DifferenceCalc calc) {
        int[][] offsets;
        double[] factors;

        if (convertMode == GraphicsEngine.FLOYD) {
            offsets = new int[][]{{1, 0}, {-1, 1}, {0, 1}, {1, 1}};
            factors = new double[]{7.0 / 16.0, 3.0 / 16.0, 5.0 / 16.0, 1.0 / 16.0};
        } else if (convertMode == GraphicsEngine.SIERRA) {
            offsets = new int[][]{{1, 0}, {2, 0}, {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1}, {-1, 2}, {0, 2}, {1, 2}};
            factors = new double[]{5.0 / 32.0, 3.0 / 32.0, 2.0 / 32.0, 4.0 / 32.0, 5.0 / 32.0, 4.0 / 32.0, 2.0 / 32.0, 2.0 / 32.0, 3.0 / 32.0, 2.0 / 32.0};
        } else {
            return;
        }

        for (int i = 0; i < offsets.length; i++) {
            int newX = x + offsets[i][0];
            int newY = y + offsets[i][1];
            if (isValidCoordinate(newX, newY, image)) {
                Color nextColor = image.get(newX).get(newY);
                image.get(newX).set(newY, calc.calc(nextColor, factors[i]));
            }
        }
    }

    private boolean isValidCoordinate(int x, int y, List<List<Color>> image) {
        return x >= 0 && x < image.size() && y >= 0 && y < image.get(0).size();
    }

    private static Color gammaToLinear(Color color, boolean isGammaBased) {
        if (!isGammaBased) {
            return color;
        }
        return new Color(gammaToLinear(color.getRed()), gammaToLinear(color.getGreen()), gammaToLinear(color.getBlue()), 1.0);
    }

    private static Color linearToGamma(Color color, boolean isGammaBased) {
        if (!isGammaBased) {
            return color;
        }
        return new Color(linearToGamma(color.getRed()), linearToGamma(color.getGreen()), linearToGamma(color.getBlue()), 1.0);
    }

    private static double linearToGamma(double value) {
        if (value < 0 || value > 1) {
            throw new ArithmeticException("Input value should be between 0 and 1. Input Value: " + value);
        }
        if (value <= 0.0031308) {
            return value * 12.92;
        }
        return Math.pow(value, 1.0 / 2.4) * 1.055 - 0.055;
    }

    private static double gammaToLinear(double value) {
        if (value < 0 || value > 1) {
            throw new ArithmeticException("Input value should be between 0 and 1. Input Value: " + value);
        }
        if (value <= 0.04045) {
            return value / 12.92;
        }
        return Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static class DifferenceCalc {
        private final double red, green, blue;

        DifferenceCalc(double red, double green, double blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        Color calc(Color nextColor, double factor) {
            double red = clamp(nextColor.getRed() + this.red * factor);
            double green = clamp(nextColor.getGreen() + this.green * factor);
            double blue = clamp(nextColor.getBlue() + this.blue * factor);
            return new Color(red, green, blue, 1.0);
        }

        private double clamp(double value) {
            return Math.max(0.0, Math.min(1.0, value));
        }
    }
}
