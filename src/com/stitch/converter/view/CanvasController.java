
package com.stitch.converter.view;

import java.util.TreeSet;

import com.stitch.converter.Preferences;
import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;
import com.stitch.converter.model.StitchImage;

import javafx.geometry.VPos;
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
	boolean isHighlightExist = false;
	double scale = 10.0d, margin = scale;

	public CanvasController(final StitchImage image, final Canvas canvas) {
		this.image = image;
		this.canvas = canvas;
		this.context = canvas.getGraphicsContext2D();
		this.canvas.setWidth(image.getWidth() * scale + 2 * margin);
		this.canvas.setHeight(image.getHeight() * scale + 2 * margin);
	}

	private void drawGrid(int x, int y, int width, int height, boolean isHighlightExist) {
		final double brightness = isHighlightExist ? 1.0d : 0.0d;
		context.setFill(new Color(brightness, brightness, brightness, 0.1d));
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
		context.setFill(new Color(brightness, brightness, brightness, 0.25d));
		for (int count = 5; count <= width; count += 10) {
			context.fillRect((int) ((x + count) * scale) + margin, (int) (y * scale) + margin, 1, height * scale);
		}
		for (int count = 5; count <= height; count += 10) {
			context.fillRect((int) (x * scale) + margin, (int) ((y + count) * scale) + margin, width * scale, 1);
		}
		context.setFill(new Color(brightness, brightness, brightness, 1d));
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
		final String fontName = Preferences.getString("fontType", "");
		context.setFont(Font.font(fontName, 0.6 * scale));
		while (getTextWidth(context.getFont(), text) > scale) {
			final double fontSize = scale / getTextWidth(context.getFont(), text);
			context.setFont(new Font(context.getFont().getSize() * fontSize));
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

	private double getTextWidth(final Font font, final String input) {
		final Text text = new Text();
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
	}

	private void renderImage() {
		context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
		image.setNumberVisible(Preferences.getBoolean("drawGridNumber", true));
		isHighlightExist = false;
		for (final PixelList pixelList : image.getPixelLists()) {
			final TreeSet<Pixel> pixelSet = pixelList.getPixelSet();
			for (final Pixel pixel : pixelSet) {
				if (pixelList.isHighlighted()) {
					isHighlightExist = true;
					continue;
				}
				context.setFill(pixelList.getColor().asFX());
				context.fillRect(margin + pixel.getX() * scale, margin + pixel.getY() * scale, scale, scale);

			}
		}
		if (isHighlightExist == true) {
			context.setFill(new Color(0d, 0d, 0d, Preferences.getDouble("highlightBrightnessLevel", 0.75d)));
			context.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
		}
		for (final PixelList pixelList : image.getPixelLists()) {
			final TreeSet<Pixel> pixelSet = pixelList.getPixelSet();
			for (final Pixel pixel : pixelSet) {
				if (isHighlightExist == true) {
					if (pixelList.isHighlighted() == true) {
						context.setFill(Color.WHITE);
						context.fillRect(pixel.getX() * scale + margin, pixel.getY() * scale + margin, scale, scale);
						continue;
					}
				}
				if (pixelList.isCompleted() == true) {
					context.setFill(Preferences.getColor("completedFillColor", new StitchColor(255, 255, 0, "")).asFX());
					context.fillRect(margin + pixel.getX() * scale, margin + pixel.getY() * scale, scale, scale);
					continue;
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
}