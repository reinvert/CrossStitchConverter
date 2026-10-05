package com.stitch.converter;

import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.StitchColor;

import javafx.scene.paint.Color;

public class DitheredColorConverter extends ColorConverter {
          
       private final int[][] offsets;
       private final double[] factors;

       DitheredColorConverter(Builder builder) {
           super(builder);

           if (builder.convertMode == GraphicsEngine.FLOYD) {

               offsets = new int[][] {
                   { 1,  0 },
                   {-1,  1 },
                   { 0,  1 },
                   { 1,  1 }
               };

               factors = new double[] {
                   7.0 / 16.0,
                   3.0 / 16.0,
                   5.0 / 16.0,
                   1.0 / 16.0
               };

           } else if (builder.convertMode == GraphicsEngine.SIERRA) {

               offsets = new int[][] {
                   { 1,  0 },
                   { 2,  0 },
                   {-2,  1 },
                   {-1,  1 },
                   { 0,  1 },
                   { 1,  1 },
                   { 2,  1 },
                   {-1,  2 },
                   { 0,  2 },
                   { 1,  2 }
               };

               factors = new double[] {
                   5.0 / 32.0,
                   3.0 / 32.0,
                   2.0 / 32.0,
                   4.0 / 32.0,
                   5.0 / 32.0,
                   4.0 / 32.0,
                   2.0 / 32.0,
                   2.0 / 32.0,
                   3.0 / 32.0,
                   2.0 / 32.0
               };

           } else {
               offsets = new int[][] {
                   { 0,  0 }
               };

               factors = new double[] {
                   0.0
               };
           }
       }

       @Override
       public void run() {

           final int width = image.getWidth();
           final int height = image.getHeight();
           final int imageSize = width * height;

           int count = 0;
              
           double[][][] workingImage = new double[width][height][3];

           for (int x = 0; x < width; x++) {
               for (int y = 0; y < height; y++) {

            	   final int rgb = image.getRGB(x, y);

            	   workingImage[x][y][0] = srgbToLinear(((rgb >> 16) & 0xFF) / 255.0f);
            	   workingImage[x][y][1] = srgbToLinear(((rgb >> 8) & 0xFF) / 255.0f);
            	   workingImage[x][y][2] = srgbToLinear((rgb & 0xFF) / 255.0f);
               }
           }

           for (int y = 0; y < height; y++) {

               boolean leftToRight = (y % 2 == 0);

               if (leftToRight) {

                   for (int x = 0; x < width; x++) {

                       processPixel(
                           workingImage,
                           x,
                           y,
                           true
                       );

                       progressListener.onProgress(
                           (double) ++count / imageSize,
                           Resources.getString(
                               "conversion_processing_colors"
                           )
                       );
                   }

               } else {

                   for (int x = width - 1; x >= 0; x--) {

                       processPixel(
                           workingImage,
                           x,
                           y,
                           false
                       );

                       progressListener.onProgress(
                           (double) ++count / imageSize,
                           Resources.getString(
                               "conversion_processing_colors"
                           )
                       );
                   }
               }
           }
       }
          


       private void processPixel(
           double[][][] workingImage,
           int x,
           int y,
           boolean leftToRight
       ) {

           double linearRed = workingImage[x][y][0];
           double linearGreen = workingImage[x][y][1];
           double linearBlue = workingImage[x][y][2];

           double clampedSrgbRed = linearToSrgbClamped(linearRed);
           double clampedSrgbGreen = linearToSrgbClamped(linearGreen);
           double clampedSrgbBlue = linearToSrgbClamped(linearBlue);

           final ImageTools.Lab colorLab =
                   ImageTools.rgbToLab(
                           clampedSrgbRed,
                           clampedSrgbGreen,
                           clampedSrgbBlue
                   );

           final StitchColor outputColor =
                   findClosestColor(colorLab);

           Pixel pixel = new Pixel(x, y, outputColor);
           this.image.setRGB(x, y, pixel.getColor().getRGB());
           stitchImage.add(pixel);


           Color outputFX = outputColor.asFX();

           double outputLinearRed = srgbToLinear(outputFX.getRed());
           double outputLinearGreen = srgbToLinear(outputFX.getGreen());
           double outputLinearBlue = srgbToLinear(outputFX.getBlue());

           double clampedLinearRed = srgbToLinear(clampedSrgbRed);
           double clampedLinearGreen = srgbToLinear(clampedSrgbGreen);
           double clampedLinearBlue = srgbToLinear(clampedSrgbBlue);

           double redDifference = clampedLinearRed - outputLinearRed;
           double greenDifference = clampedLinearGreen - outputLinearGreen;
           double blueDifference = clampedLinearBlue - outputLinearBlue;

           applyErrorDiffusion(
               workingImage,
               x,
               y,
               redDifference,
               greenDifference,
               blueDifference,
               leftToRight
           );
       }

       private void applyErrorDiffusion(
           double[][][] workingImage,
           int x,
           int y,
           double redDifference,
           double greenDifference,
           double blueDifference,
           boolean leftToRight
       ) {

           for (int i = 0; i < offsets.length; i++) {

               int offsetX = offsets[i][0];

               if (!leftToRight) {
                   offsetX = -offsetX;
               }

               int newX = x + offsetX;
               int newY = y + offsets[i][1];

               if (!isValidCoordinate(newX, newY, workingImage)) {
                   continue;
               }

               double factor = factors[i];

               workingImage[newX][newY][0] += redDifference     * factor;
               workingImage[newX][newY][1] += greenDifference     * factor;
               workingImage[newX][newY][2] += blueDifference     * factor;
           }
       }

       private boolean isValidCoordinate(
           int x,
           int y,
           double[][][] image
       ) {
           return x >= 0 && x < image.length && y >= 0 && y < image[0].length;
       }

       private double srgbToLinear(double value) {
           if (value <= 0.04045) {
               return value / 12.92;
           } else {
               return Math.pow((value + 0.055) / 1.055, 2.4);
           }
       }

       private double linearToSrgbClamped(double value) {
           value = Math.max(0.0, Math.min(1.0, value));

           if (value <= 0.0031308) {
               return value * 12.92;
           } else {
               return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
           }
       }
}