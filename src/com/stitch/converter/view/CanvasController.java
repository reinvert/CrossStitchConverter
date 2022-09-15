
package com.stitch.converter.view;

import java.util.Collection;
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
	final Canvas canvas;
	final GraphicsContext context;
	final StitchImage image;
	private boolean isHighlightExist = false, isHighlightAlternate = false;
	protected double scale = 10.0d, margin = scale;

	private static final Color DARK_GRAY_1 = new Color(0.0d, 0.0d, 0.0d, 1d);
	private static final Color DARK_GRAY_2 = new Color(0.0d, 0.0d, 0.0d, 0.25d);
	private static final Color DARK_GRAY_3 = new Color(0.0d, 0.0d, 0.0d, 0.1d);

	private static final Color BRIGHT_GRAY_1 = new Color(1.0d, 1.0d, 1.0d, 1d);
	private static final Color BRIGHT_GRAY_2 = new Color(1.0d, 1.0d, 1.0d, 0.25d);
	private static final Color BRIGHT_GRAY_3 = new Color(1.0d, 1.0d, 1.0d, 0.1d);

	private Color background, highlightAlternateColor;
	private Color darkerColor;

	private final String fontName;
	
	private int highlightX = -1, highlightY = -1;
	
	private final Font originalFont;
	private final HashMap<Integer, Font> fontBySize = new HashMap<>();
	
	public CanvasController(final StitchImage image, final Canvas canvas) {
		this.image = image;
		this.canvas = canvas;
		this.context = canvas.getGraphicsContext2D();
		this.canvas.setWidth(image.getWidth() * scale + 2 * margin);
		this.canvas.setHeight(image.getHeight() * scale + 2 * margin);
		background = Preferences.getColor("completedFillColor", new StitchColor(255, 255, 0, "")).asFX();
		highlightAlternateColor = Preferences.getColor("highlightAlternateColor", new StitchColor(128, 255, 128, "")).asFX();
		isHighlightAlternate = Preferences.getBoolean("isHighlightAlternate", false);
		darkerColor = new Color(0d, 0d, 0d, Preferences.getDouble("highlightBrightnessLevel", 0.75d));
		fontName = Preferences.getString("fontType", "");
		originalFont = new Font(fontName, scale);
		canvas.setCache(true);
		canvas.setCacheHint(CacheHint.SPEED);
	}

	private void drawGrid(int x, int y, int width, int height, boolean isHighlightExist) {
		context.setFill(isHighlightExist ? BRIGHT_GRAY_3 : DARK_GRAY_3);
		for (int count = 0; count <= width; count++) {
			if (count % 5 == 0) {
				continue;
			}
			context.fillRect((int) ((x + count) * scale) + margin, (int) (y * scale) + margin, 1, height * scale);
		}
		for (int count = 0; count <= height; count++) {
			if (count % 5 == 0) {
				continue;
			}
			context.fillRect((int) (x * scale) + margin, (int) ((y + count) * scale) + margin, width * scale, 1);
		}

		context.setFill(isHighlightExist ? BRIGHT_GRAY_2 : DARK_GRAY_2);
		for (int count = 5; count <= width; count += 10) {
			context.fillRect((int) ((x + count) * scale) + margin, (int) (y * scale) + margin, 1, height * scale);
		}
		for (int count = 5; count <= height; count += 10) {
			context.fillRect((int) (x * scale) + margin, (int) ((y + count) * scale) + margin, width * scale, 1);
		}

		context.setFill(isHighlightExist ? BRIGHT_GRAY_1 : DARK_GRAY_1);
		for (int count = 0; count <= width; count += 10) {
			context.fillRect((int) ((x + count) * scale) + margin, (int) (y * scale) + margin, 1, height * scale);
		}
		for (int count = 0; count <= height; count += 10) {
			context.fillRect((int) (x * scale) + margin, (int) ((y + count) * scale) + margin, width * scale, 1);
		}
	}

	private void drawIndex() {
		context.setFill(Color.BLACK);
		context.setTextAlign(TextAlignment.CENTER);
		context.setTextBaseline(VPos.CENTER);
		for (final PixelList pixelList : image.getPixelLists()) {
			if (pixelList.isCompleted() == true) {
				continue;
			}
			for (final Pixel pixel : pixelList.getPixelSet()) {
				if (pixel.getColor().equals(image.getBackground())) {
					continue;
				}
				if (isHighlightExist == false) {
					if (pixel.getColor().getRed() + pixel.getColor().getBlue() + pixel.getColor().getGreen() < 128
							* 3) {
						context.setFill(Color.WHITE);
					} else {
						context.setFill(Color.BLACK);
					}
				} else {
					context.setFill(Color.BLACK);
				}
				drawString(pixel.getX(), pixel.getY(), Integer.toString(pixelList.getIndex()));
			}
		}
	}

	private void drawString(final int x, final int y, final String text) {
		context.setFont(originalFont);
		if (getTextWidth(originalFont, text) > scale) {
			final double textScale = scale / getTextWidth(originalFont, text);
			int fontSize = (int)(context.getFont().getSize() * textScale);
			Font newFont = fontBySize.get(fontSize);
			if(newFont == null) {
				newFont = new Font(fontName, fontSize);
				fontBySize.put(fontSize, newFont);
			}
			context.setFont(newFont);
		}
		context.fillText(text, x * scale + (scale / 2) + margin, y * scale + (scale / 2) + margin);
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public StitchImage getImage() {
		return image;
	}

	public double getMargin() {
		return margin;
	}

	public double getScale() {
		return scale;
	}

	final Text text = new Text();
	private double getTextWidth(final Font font, final String input) {
		text.setFont(font);
		text.setText(input);
		text.setWrappingWidth(0);
		text.setLineSpacing(0);
		return Math.ceil(text.getLayoutBounds().getWidth());
	}

	public void invalidate() {
		renderImage();
		drawGrid(0, 0, (int) image.getWidth(), (int) image.getHeight(), isHighlightExist);
		if (Preferences.getBoolean("drawGridNumber", true)) {
			drawIndex();
		}
		if(highlightX != -1 && highlightY != -1) {
			drawHighlightPixel(highlightX, highlightY);
		}
		if(isDrawDistanceCircle == true) {
			drawDistanceCircle();
		}
	}

	private void renderImage() {
		context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		image.setNumberVisible(Preferences.getBoolean("drawGridNumber", true));
		isHighlightExist = false;
		final Collection<PixelList> pixelLists = image.getPixelLists();
		for (final PixelList pixelList : pixelLists) {
			if (pixelList.isHighlighted()) {
				isHighlightExist = true;
				break;
			}
		}
		if (isHighlightExist == true) {
			context.setFill(darkerColor);
			context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		}
		for (final PixelList pixelList : pixelLists) {
			final Color pixelColor = pixelList.getColor().asFX();
			for (final Pixel pixel : pixelList.getPixelSet()) {
				if (pixelList.isCompleted() == true) {
					context.setFill(background);
					context.fillRect(pixel.getX() * scale + margin, pixel.getY() * scale + margin, scale, scale);
				} else if (isHighlightExist == true) {
					if (pixelList.isHighlighted() == true) {
						if(isHighlightAlternate == true && (pixel.getX() + pixel.getY()) %2 == 0) {
							context.setFill(highlightAlternateColor);
						} else {
							context.setFill(Color.WHITE);
						}
						context.fillRect(pixel.getX() * scale + margin, pixel.getY() * scale + margin, scale, scale);
					} else {
						context.setFill(pixelColor);
						context.fillRect(margin + pixel.getX() * scale, margin + pixel.getY() * scale, scale, scale);
						context.setFill(darkerColor);
						context.fillRect(pixel.getX() * scale + margin, pixel.getY() * scale + margin, scale, scale);
					}
				} else {
					context.setFill(pixelList.getColor().asFX());
					context.fillRect(margin + pixel.getX() * scale, margin + pixel.getY() * scale, scale, scale);
				}
			}
		}
	}

	public void setMargin(final double margin) {
		this.margin = margin;
		canvas.setWidth(image.getWidth() * scale + 2 * margin);
		canvas.setHeight(image.getHeight() * scale + 2 * margin);
	}

	public void setScale(final double scale) {
		this.scale = scale;
		canvas.setWidth(image.getWidth() * scale + 2 * margin);
		canvas.setHeight(image.getHeight() * scale + 2 * margin);
	}
	
	public void setHighlightPixel(final int x, final int y) {
		highlightX = x;
		highlightY = y;
	}
	
	public void drawHighlightPixel(final int x, final int y) {
		context.setStroke(Color.RED);
		context.strokeRect(x * scale + margin, y * scale + margin, scale, scale);
	}
	
	private boolean isDrawDistanceCircle = false;
	private double distanceCircleX = 0, distanceCircleY = 0;
	
	public void startDrawDistanceCircle(final double x, final double y) {
		isDrawDistanceCircle = true;
		distanceCircleX = x;
		distanceCircleY = y;
	}
	
	public void stopDrawDistanceCircle() {
		isDrawDistanceCircle = false;
	}
	
	public void drawDistanceCircle() {
		final double circleSize = Preferences.getDouble("distanceCircleSize", 20d) * scale;
		final Color color = Preferences.getColor("distanceCircleColor", new StitchColor(128, 128, 255, "")).asFX();
		context.setStroke(color);
		context.strokeOval(distanceCircleX - circleSize/2, distanceCircleY - circleSize/2, circleSize, circleSize);
	}
}