package com.stitch.converter.view;

import java.util.HashMap;

import com.stitch.converter.Preferences;
import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;
import com.stitch.converter.model.StitchImage;

import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class CanvasController {
    protected final Canvas canvas;
    protected final GraphicsContext context;
    protected final StitchImage image;
    private boolean isHighlightExist = false;
    private boolean isHighlightAlternate = false;
    protected double scale = 10.0d;
    protected double margin = scale;
    
    private Color backgroundColor;
    private Color highlightAlternateColor;
    private Color darkerColor;

    private final String fontName;
    private int highlightX = -1;
    private int highlightY = -1;

    private final Font originalFont;
    private final HashMap<Integer, Font> fontCache = new HashMap<>();

    private static final Color DARK_GRAY = new Color(0.0d, 0.0d, 0.0d, 1d);
    private static final Color DARK_GRAY_SEMI_TRANSPARENT = new Color(0.0d, 0.0d, 0.0d, 0.25d);
    private static final Color LIGHT_GRAY = new Color(1.0d, 1.0d, 1.0d, 1d);

    public CanvasController(final StitchImage image, final Canvas canvas) {
        this.image = image;
        this.canvas = canvas;
        this.context = canvas.getGraphicsContext2D();
        setupCanvas();

        backgroundColor = Preferences.getColor("completedFillColor", new StitchColor(255, 255, 0, "")).asFX();
        highlightAlternateColor = Preferences.getColor("highlightAlternateColor", new StitchColor(128, 255, 128, "")).asFX();
        isHighlightAlternate = Preferences.getBoolean("isHighlightAlternate", false);
        darkerColor = new Color(0d, 0d, 0d, Preferences.getDouble("highlightBrightnessLevel", 0.75d));

        fontName = Preferences.getValue("fontType", "");
        originalFont = new Font(fontName, scale);

        canvas.setCache(true);
        canvas.setCacheHint(CacheHint.SPEED);
    }

    private void setupCanvas() {
        canvas.setWidth(image.getWidth() * scale + 2 * margin);
        canvas.setHeight(image.getHeight() * scale + 2 * margin);
    }

    private void drawGrid(int x, int y, int width, int height, boolean isHighlight) {
        Color primaryColor = isHighlight ? LIGHT_GRAY : DARK_GRAY_SEMI_TRANSPARENT;
        Color gridColor = isHighlight ? LIGHT_GRAY : DARK_GRAY;
        
        drawGridLines(x, y, width, height, primaryColor, gridColor);
    }

    private void drawGridLines(int x, int y, int width, int height, Color primaryColor, Color gridColor) {
        context.setFill(primaryColor);
        for (int i = 0; i <= width; i++) {
            if (i % 5 != 0) {
                context.fillRect((x + i) * scale + margin, y * scale + margin, 1, height * scale);
            }
        }
        for (int i = 0; i <= height; i++) {
            if (i % 5 != 0) {
                context.fillRect(x * scale + margin, (y + i) * scale + margin, width * scale, 1);
            }
        }

        context.setFill(gridColor);
        for (int i = 5; i <= width; i += 10) {
            context.fillRect((x + i) * scale + margin, y * scale + margin, 1, height * scale);
        }
        for (int i = 5; i <= height; i += 10) {
            context.fillRect(x * scale + margin, (y + i) * scale + margin, width * scale, 1);
        }
    }

    private void drawIndex() {
        context.setFill(Color.BLACK);
        context.setTextAlign(TextAlignment.CENTER);
        context.setTextBaseline(VPos.CENTER);

        for (final PixelList pixelList : image.getPixelLists()) {
            if (pixelList.isCompleted()) continue;

            for (final Pixel pixel : pixelList.getPixelSet()) {
                if (!pixel.getColor().equals(image.getBackground())) {
                    context.setFill(getTextColor(pixel));
                    drawText(pixel.getX(), pixel.getY(), Integer.toString(pixelList.getIndex()));
                }
            }
        }
    }

    private Color getTextColor(Pixel pixel) {
        if (isHighlightExist) return Color.BLACK;
        int colorSum = pixel.getColor().getRed() + pixel.getColor().getGreen() + pixel.getColor().getBlue();
        return colorSum < 128 * 3 ? Color.WHITE : Color.BLACK;
    }

    private void drawText(final int x, final int y, final String text) {
        context.setFont(originalFont);

        double textWidth = getTextWidth(originalFont, text);
        if (textWidth > scale) {
            int scaledFontSize = (int) (context.getFont().getSize() * (scale / textWidth));
            context.setFont(fontCache.computeIfAbsent(scaledFontSize, size -> new Font(fontName, size)));
        }

        context.fillText(text, x * scale + (scale / 2) + margin, y * scale + (scale / 2) + margin);
    }

    private double getTextWidth(final Font font, final String text) {
        Text tempText = new Text(text);
        tempText.setFont(font);
        return Math.ceil(tempText.getLayoutBounds().getWidth());
    }

    public void invalidate() {
        renderImage();
        drawGrid(0, 0, (int) image.getWidth(), (int) image.getHeight(), isHighlightExist);

        if (Preferences.getBoolean("drawGridNumber", true)) {
            drawIndex();
        }

        if (highlightX != -1 && highlightY != -1) {
            drawHighlightPixel(highlightX, highlightY);
        }

        if (isDrawDistanceCircle) {
            drawDistanceCircle();
        }
    }

    private void renderImage() {
        context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        boolean drawGridNumber = Preferences.getBoolean("drawGridNumber", true);
        image.setNumberVisible(drawGridNumber);
        
        isHighlightExist = image.getPixelLists().stream().anyMatch(PixelList::isHighlighted);

        if (isHighlightExist) {
            context.setFill(darkerColor);
            context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }

        for (final PixelList pixelList : image.getPixelLists()) {
            Color pixelColor = pixelList.getColor().asFX();
            drawPixels(pixelList, pixelColor);
        }
    }

    private void drawPixels(final PixelList pixelList, Color pixelColor) {
        for (final Pixel pixel : pixelList.getPixelSet()) {
            if (pixelList.isCompleted()) {
                context.setFill(backgroundColor);
            } else if (isHighlightExist) {
                context.setFill(getPixelHighlightColor(pixelList, pixel));
            } else {
                context.setFill(pixelColor);
            }
            context.fillRect(pixel.getX() * scale + margin, pixel.getY() * scale + margin, scale, scale);
        }
    }

    private Color getPixelHighlightColor(final PixelList pixelList, final Pixel pixel) {
        if (pixelList.isHighlighted()) {
            return isHighlightAlternate && (pixel.getX() + pixel.getY()) % 2 == 0
                ? highlightAlternateColor
                : Color.WHITE;
        }
        return darkerColor;
    }

    public void setScale(double scale) {
        this.scale = scale;
        setupCanvas();
    }

    public void setMargin(double margin) {
        this.margin = margin;
        setupCanvas();
    }
    
    public double getScale() {
    	return scale;
    }
    
    public double getMargin() {
    	return margin;
    }
    
    public StitchImage getImage() {
    	return image;
    }
    
    public Canvas getCanvas() {
    	return canvas;
    }

    public void setHighlightPixel(final int x, final int y) {
        this.highlightX = x;
        this.highlightY = y;
    }

    public void drawHighlightPixel(final int x, final int y) {
        context.setStroke(Color.RED);
        context.strokeRect(x * scale + margin, y * scale + margin, scale, scale);
    }

    private boolean isDrawDistanceCircle = false;
    private double distanceCircleX = 0;
    private double distanceCircleY = 0;

    public void startDrawDistanceCircle(final double x, final double y) {
        isDrawDistanceCircle = true;
        distanceCircleX = x;
        distanceCircleY = y;
    }

    public void stopDrawDistanceCircle() {
        isDrawDistanceCircle = false;
    }

    public void drawDistanceCircle() {
        double circleSize = Preferences.getDouble("distanceCircleSize", 20d) * scale;
        Color circleColor = Preferences.getColor("distanceCircleColor", new StitchColor(128, 128, 255, "")).asFX();
        context.setStroke(circleColor);
        context.strokeOval(distanceCircleX - circleSize / 2, distanceCircleY - circleSize / 2, circleSize, circleSize);
    }
}
