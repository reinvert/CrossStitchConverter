package com.stitch.converter;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.imageio.ImageIO;

import com.stitch.converter.model.StitchImage;

import javafx.scene.paint.Color;

import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;

/**
 * A class that contains methods for manipulating
 * {@link java.awt.image.BufferedImage BufferedImage}.
 * 
 * @author Reinvert
 *
 */
final class ImageTools {
    /*
     * CIELAB color.
     *
     * CIEDE2000의 입력은 CIELAB L*, a*, b*이다.
     */
    public static final class Lab {
        public final double L;
        public final double a;
        public final double b;

        public Lab(double L, double a, double b) {
            this.L = L;
            this.a = a;
            this.b = b;
        }
    }

    /*
     * sRGB -> CIELAB
     *
     * D65 reference white.
     *
     * sRGB
     *   -> linear RGB
     *   -> XYZ
     *   -> CIELAB
     */
    public static Lab rgbToLab(Color color) {
        double r = srgbToLinear(color.getRed());
        double g = srgbToLinear(color.getGreen());
        double b = srgbToLinear(color.getBlue());

        // sRGB D65 -> XYZ
        double x = r * 0.4124564
                 + g * 0.3575761
                 + b * 0.1804375;

        double y = r * 0.2126729
                 + g * 0.7151522
                 + b * 0.0721750;

        double z = r * 0.0193339
                 + g * 0.1191920
                 + b * 0.9503041;

        // D65 reference white.
        double xn = 0.95047;
        double yn = 1.00000;
        double zn = 1.08883;

        double fx = labF(x / xn);
        double fy = labF(y / yn);
        double fz = labF(z / zn);

        double L = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double bb = 200.0 * (fy - fz);

        return new Lab(L, a, bb);
    }

    private static double labF(double t) {
        /*
         * CIELAB definition:
         *
         * epsilon = (6/29)^3
         * kappa   = (29/3)^3
         */
        final double epsilon = 216.0 / 24389.0;
        final double kappa = 24389.0 / 27.0;

        if (t > epsilon) {
            return Math.cbrt(t);
        }

        return (kappa * t + 16.0) / 116.0;
    }

    /*
     * sRGB encoded value -> linear RGB.
     */
    public static double srgbToLinear(double value) {
        if (value <= 0.04045) {
            return value / 12.92;
        }

        return Math.pow(
                (value + 0.055) / 1.055,
                2.4
        );
    }

    /*
     * Linear RGB -> sRGB encoded value.
     */
    public static double linearToSrgb(double value) {
        if (value <= 0.0031308) {
            return 12.92 * value;
        }

        return 1.055 * Math.pow(value, 1.0 / 2.4) - 0.055;
    }

    /*
     * ============================================================
     * CIEDE2000
     * ============================================================
     *
     * ISO/CIE 11664-6
     *
     * Input:
     *   CIELAB L*, a*, b*
     *
     * Output:
     *   ΔE00
     *
     * kL = kC = kH = 1
     * under the reference conditions.
     */
    public static double calculateDifference(Lab color1, Lab color2) {
        return calculateCIEDE2000(color1, color2);
    }

    public static double calculateCIEDE2000(Lab c1, Lab c2) {

        final double L1 = c1.L;
        final double a1 = c1.a;
        final double b1 = c1.b;

        final double L2 = c2.L;
        final double a2 = c2.a;
        final double b2 = c2.b;

        final double kL = 1.0;
        final double kC = 1.0;
        final double kH = 1.0;

        final double C1 = Math.hypot(a1, b1);
        final double C2 = Math.hypot(a2, b2);

        final double Cbar = (C1 + C2) / 2.0;

        final double Cbar7 = Math.pow(Cbar, 7.0);
        final double twentyFive7 = Math.pow(25.0, 7.0);

        final double G = 0.5 * (
                1.0 - Math.sqrt(
                        Cbar7 / (Cbar7 + twentyFive7)
                )
        );

        final double a1Prime = (1.0 + G) * a1;
        final double a2Prime = (1.0 + G) * a2;

        final double C1Prime = Math.hypot(a1Prime, b1);
        final double C2Prime = Math.hypot(a2Prime, b2);

        final double h1Prime = calculateHue(a1Prime, b1, C1Prime);
        final double h2Prime = calculateHue(a2Prime, b2, C2Prime);

        final double deltaLPrime = L2 - L1;
        final double deltaCPrime = C2Prime - C1Prime;

        final double deltaHuePrime;

        if (C1Prime * C2Prime == 0.0) {
            deltaHuePrime = 0.0;
        } else {
            double dh = h2Prime - h1Prime;

            if (Math.abs(dh) <= 180.0) {
                deltaHuePrime = dh;
            } else if (dh > 180.0) {
                deltaHuePrime = dh - 360.0;
            } else {
                deltaHuePrime = dh + 360.0;
            }
        }

        final double deltaHPrime =
                2.0
                * Math.sqrt(C1Prime * C2Prime)
                * Math.sin(Math.toRadians(deltaHuePrime / 2.0));

        final double LbarPrime = (L1 + L2) / 2.0;
        final double CbarPrime = (C1Prime + C2Prime) / 2.0;

        final double hbarPrime;

        if (C1Prime * C2Prime == 0.0) {
            hbarPrime = h1Prime + h2Prime;
        } else {
            double dh = Math.abs(h1Prime - h2Prime);

            if (dh <= 180.0) {
                hbarPrime = (h1Prime + h2Prime) / 2.0;
            } else {
                double sum = h1Prime + h2Prime;

                if (sum < 360.0) {
                    hbarPrime = (sum + 360.0) / 2.0;
                } else {
                    hbarPrime = (sum - 360.0) / 2.0;
                }
            }
        }

        final double hbarRad = Math.toRadians(hbarPrime);

        final double T =
                1.0
                - 0.17 * Math.cos(hbarRad - Math.toRadians(30.0))
                + 0.24 * Math.cos(2.0 * hbarRad)
                + 0.32 * Math.cos(3.0 * hbarRad + Math.toRadians(6.0))
                - 0.20 * Math.cos(4.0 * hbarRad - Math.toRadians(63.0));

        final double deltaTheta =
                30.0
                * Math.exp(
                        -Math.pow(
                                (hbarPrime - 275.0) / 25.0,
                                2.0
                        )
                );

        final double RC =
                2.0 * Math.sqrt(
                        Math.pow(CbarPrime, 7.0)
                        /
                        (
                                Math.pow(CbarPrime, 7.0)
                                + Math.pow(25.0, 7.0)
                        )
                );

        final double RT =
                -Math.sin(Math.toRadians(2.0 * deltaTheta))
                * RC;

        final double LMinus50 = LbarPrime - 50.0;

        final double SL =
                1.0
                + (
                    0.015
                    * LMinus50
                    * LMinus50
                  )
                /
                Math.sqrt(
                        20.0
                        + LMinus50 * LMinus50
                );

        final double SC =
                1.0
                + 0.045 * CbarPrime;

        final double SH =
                1.0
                + 0.015 * CbarPrime * T;

        final double lightnessTerm =
                deltaLPrime / (kL * SL);

        final double chromaTerm =
                deltaCPrime / (kC * SC);

        final double hueTerm =
                deltaHPrime / (kH * SH);

        double deltaE2 =
                lightnessTerm * lightnessTerm
                + chromaTerm * chromaTerm
                + hueTerm * hueTerm
                + RT * chromaTerm * hueTerm;

        if (deltaE2 < 0.0) {
            deltaE2 = 0.0;
        }

        return Math.sqrt(deltaE2);
    }

    private static double calculateHue(
            double aPrime,
            double b,
            double chromaPrime
    ) {
        if (chromaPrime == 0.0) {
            return 0.0;
        }

        double hue = Math.toDegrees(
                Math.atan2(b, aPrime)
        );

        if (hue < 0.0) {
            hue += 360.0;
        }

        return hue;
    }

    /*
     * RGB 간단 차이 계산 (필요 시 유지)
     */
    public static double calculateDifference(
            Color color1,
            Color color2
    ) {
        double dr = color1.getRed() - color2.getRed();
        double dg = color1.getGreen() - color2.getGreen();
        double db = color1.getBlue() - color2.getBlue();

        return dr * dr + dg * dg + db * db;
    }

    /**
     * StitchColor 간 CIEDE2000 차이를 계산하도록 수정.
     */
    static double calculateDifference(final StitchColor originalColor, final StitchColor targetColor) {		
        return calculateDifference(rgbToLab(originalColor.asFX()), rgbToLab(targetColor.asFX()));
    }

    static StitchColor calculateRemoveString(final StitchImage stitchImage, final Map<String, Integer> usedColorCount) {
        StitchColor uselessColor = null;
        double difference = Double.MAX_VALUE;
        final ArrayList<PixelList> list = new ArrayList<PixelList>(stitchImage.getPixelLists());
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                final StitchColor originalColor = list.get(i).getColor();
                final StitchColor targetColor = list.get(j).getColor();

                final double avgcolor = calculateDifference(originalColor, targetColor);
                if (difference > avgcolor) {
                    final int orgcount = usedColorCount.get(list.get(i).getColor().getName());
                    final int tarcount = usedColorCount.get(list.get(j).getColor().getName());
                    difference = avgcolor;
                    if (orgcount >= tarcount) {
                        uselessColor = list.get(j).getColor();
                    } else {
                        uselessColor = list.get(i).getColor();
                    }
                }
            }
        }
        return uselessColor;
    }

    static double getScaleFactor(final double originalSize, final double targetSize) {
        double scale = 1d;
        if (originalSize > targetSize) {
            scale = targetSize / originalSize;
        } else {
            scale = originalSize / targetSize;
        }
        return scale;
    }

    static double getScaleFactorToFit(final Dimension originalSize, final Dimension targetSize) {
        final double widthScale = getScaleFactor(originalSize.getWidth(), targetSize.getWidth());
        final double heightScale = getScaleFactor(originalSize.getHeight(), targetSize.getHeight());
        return Math.min(widthScale, heightScale);
    }

    static BufferedImage readImage(final File file) throws IOException {
        final BufferedImage image = ImageIO.read(file);
        return readImage(image);
    }
    
    static BufferedImage readImage(final File file, final int width, final int height)
            throws IOException, NullPointerException {
        BufferedImage image = ImageIO.read(file);
        final double scaleFactor = Math.min(1d, ImageTools.getScaleFactorToFit(
                new Dimension(image.getWidth(), image.getHeight()), new Dimension(width, height)));
        final int scaleWidth = (int) Math.round(image.getWidth() * scaleFactor);
        final int scaleHeight = (int) Math.round(image.getHeight() * scaleFactor);
        image = ImageTools.resize(image, scaleWidth, scaleHeight);
        return readImage(image);
    }
    
    private static BufferedImage readImage(final BufferedImage image) throws IOException {
        final GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsConfiguration graphicsConfiguration = graphicsEnvironment.getDefaultScreenDevice()
                .getDefaultConfiguration();
        final BufferedImage backgroundFilledBufferedImage = graphicsConfiguration
                .createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.OPAQUE);
        backgroundFilledBufferedImage.createGraphics().drawImage(image, 0, 0, java.awt.Color.WHITE, null);
        backgroundFilledBufferedImage.setAccelerationPriority(1);
        return backgroundFilledBufferedImage;
    }

    private static BufferedImage resize(final BufferedImage img, final int newW, final int newH) {
        if(newW < 0) {
            throw new ArithmeticException("Input value should above 0. Input Value(newW): " + newW);
        }
        if(newH < 0) {
            throw new ArithmeticException("Input value should above 0. Input Value(newH): " + newH);
        }
        final java.awt.Image originalImage = img.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
        final BufferedImage outputImage = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        return outputImage;
    }

    private ImageTools() {
        throw new AssertionError("Singleton class should not be accessed by constructor.");
    }
}