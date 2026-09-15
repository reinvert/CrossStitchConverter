package com.stitch.converter;

import java.util.ArrayList;
import java.util.List;

import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.StitchColor;

import javafx.scene.paint.Color;

public class DitheredColorConverter extends ColorConverter {

    private int convertMode = GraphicsEngine.FLOYD;

    /*
     * 팔레트 Lab 값 사전 계산용 내부 클래스
     */
    private static class PaletteEntry {
        final StitchColor stitchColor;
        final ImageTools.Lab lab;

        PaletteEntry(StitchColor stitchColor, ImageTools.Lab lab) {
            this.stitchColor = stitchColor;
            this.lab = lab;
        }
    }

    private List<PaletteEntry> cachedPalette;

    DitheredColorConverter(Builder builder) {
        super(builder);
        convertMode = builder.convertMode;
    }

    @Override
    public void run() {

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int imageSize = width * height;

        int count = 0;

        /*
         * 팔레트 Lab 색상을 미리 1회만 계산하여 캐싱 (성능 최적화)
         */
        cachedPalette = new ArrayList<>(colorList.size());
        for (StitchColor listColor : colorList) {
            ImageTools.Lab lab = ImageTools.rgbToLab(listColor.asFX());
            cachedPalette.add(new PaletteEntry(listColor, lab));
        }

        /*
         * Working image (Linear RGB)
         */
        double[][][] workingImage = new double[width][height][3];

        /*
         * 원본 sRGB → Linear RGB
         */
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                Color color = new StitchColor(
                        image.getRGB(x, y),
                        ""
                ).asFX();

                workingImage[x][y][0] = srgbToLinear(color.getRed());
                workingImage[x][y][1] = srgbToLinear(color.getGreen());
                workingImage[x][y][2] = srgbToLinear(color.getBlue());
            }
        }

        /*
         * Serpentine scan.
         */
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

    /**
     * 현재 픽셀을 처리한다.
     */
    private void processPixel(
        double[][][] workingImage,
        int x,
        int y,
        boolean leftToRight
    ) {

        double linearRed = workingImage[x][y][0];
        double linearGreen = workingImage[x][y][1];
        double linearBlue = workingImage[x][y][2];

        /*
         * 팔레트 비교용 sRGB Clamped 값 계산
         */
        double clampedSrgbRed = linearToSrgbClamped(linearRed);
        double clampedSrgbGreen = linearToSrgbClamped(linearGreen);
        double clampedSrgbBlue = linearToSrgbClamped(linearBlue);

        Color color = new Color(
            clampedSrgbRed,
            clampedSrgbGreen,
            clampedSrgbBlue,
            1.0
        );

        /*
         * CIEDE2000으로 가장 가까운 자수 색상을 찾는다.
         */
        StitchColor outputColor = findClosestColor(color);

        /*
         * 결과 픽셀 생성 및 저장
         */
        Pixel pixel = new Pixel(x, y, outputColor);
        this.image.setRGB(x, y, pixel.getColor().getRGB());
        stitchImage.add(pixel);

        /*
         * 선택된 팔레트 색상을 Linear RGB로 변환한다.
         */
        Color outputFX = outputColor.asFX();

        double outputLinearRed = srgbToLinear(outputFX.getRed());
        double outputLinearGreen = srgbToLinear(outputFX.getGreen());
        double outputLinearBlue = srgbToLinear(outputFX.getBlue());

        /*
         * 핵심 수정사항:
         * 실제로 선택에 사용된 '클램핑된 Linear RGB'와 '선택된 팔레트 Linear RGB' 간의
         * 양자화 오차를 계산합니다. 
         * 이렇게 해야 범위를 벗어난 음수 오차가 무한히 축적되어 청회색으로 뭉치는 현상을 방지합니다.
         */
        double clampedLinearRed = srgbToLinear(clampedSrgbRed);
        double clampedLinearGreen = srgbToLinear(clampedSrgbGreen);
        double clampedLinearBlue = srgbToLinear(clampedSrgbBlue);

        double redDifference = clampedLinearRed - outputLinearRed;
        double greenDifference = clampedLinearGreen - outputLinearGreen;
        double blueDifference = clampedLinearBlue - outputLinearBlue;

        /*
         * 오차 확산.
         */
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

    /**
     * CIEDE2000을 이용하여 캐싱된 팔레트 중 가장 가까운 색상을 찾는다.
     */
    private StitchColor findClosestColor(Color color) {

        double difference = Double.MAX_VALUE;
        StitchColor closestColor = null;

        ImageTools.Lab colorLab = ImageTools.rgbToLab(color);

        for (PaletteEntry entry : cachedPalette) {

            double calculatedDifference = ImageTools.calculateDifference(
                colorLab,
                entry.lab
            );

            if (calculatedDifference < difference) {
                closestColor = entry.stitchColor;
                difference = calculatedDifference;
            }
        }

        return closestColor;
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

        int[][] offsets;
        double[] factors;

        if (convertMode == GraphicsEngine.FLOYD) {

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

        } else if (convertMode == GraphicsEngine.SIERRA) {

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
            return;
        }

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

            workingImage[newX][newY][0] += redDifference * factor;
            workingImage[newX][newY][1] += greenDifference * factor;
            workingImage[newX][newY][2] += blueDifference * factor;
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