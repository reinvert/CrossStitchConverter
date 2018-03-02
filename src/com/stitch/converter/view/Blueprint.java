package com.stitch.converter.view;

import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchImage;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

public class Blueprint extends CanvasController {

	private double listScale = scale;
	private double listZeroX, listZeroY;

	public Blueprint(final StitchImage image, final Canvas canvas) {
		super(image, canvas);
		final int listSize = image.getPixelLists().size();
		final int row = (int) (image.getHeight() * scale / listScale);
		final int column = Integer.max((int) (listSize / row) + 1, 1);
		listZeroX = image.getWidth() * scale + 2 * margin;
		listZeroY = margin;
		canvas.setWidth(canvas.getWidth() + column * listScale * 10 + margin);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		renderLists();
	}

	@Override
	public void setScale(final double scale) {
		super.setScale(scale);
		final int listSize = image.getPixelLists().size();
		final int row = (int) (image.getHeight() * scale / listScale);
		final int column = Integer.max((int) (listSize / row) + 1, 1);
		listZeroX = image.getWidth() * scale + 2 * margin;
		canvas.setWidth(listZeroX + column * listScale * 10 + margin);
	}

	public void setListScale(final double listScale) {
		this.listScale = listScale;
		final int listSize = image.getPixelLists().size();
		final int row = (int) (image.getHeight() * scale / listScale);
		final int column = Integer.max((int) (listSize / row) + 1, 1);
		canvas.setWidth(listZeroX + column * listScale * 10 + margin);
	}

	public double getListScale() {
		return listScale;
	}

	private void renderLists() {
		context.setTextBaseline(VPos.TOP);
		context.setFont(new Font(listScale / 1.3));
		int xCount = 0, yCount = 0;
		for (final PixelList pixelList : image.getPixelLists()) {
			final Color color = pixelList.getColor().asFX();
			double x = listZeroX + xCount * listScale * 10;
			double y = listZeroY + yCount * listScale;
			if (y + listScale > canvas.getHeight() - margin * 2) {
				xCount++;
				yCount = 0;
				x = listZeroX + xCount * listScale * 10;
				y = listZeroY + yCount * listScale;
			}
			yCount++;
			
			context.setFill(color);
			context.fillRect(x, y, listScale * 5, listScale * 0.9);
			
			context.setFill(Color.BLACK);
			final String index = Integer.toString(pixelList.getIndex());
			context.setTextAlign(TextAlignment.RIGHT);
			context.fillText(index, x + listScale * 6.5, y);
			
			final double red = color.getRed();
			final double green = color.getGreen();
			final double blue = color.getBlue();
			if (red + green + blue > 2.0d) {
				final double textWidth = getTextWidth(context.getFont(), pixelList.getColor().getName());
				final double textHeight = getTextHeight(context.getFont(), pixelList.getColor().getName());
				context.fillRect(x + listScale * 7.5, y, textWidth, textHeight);
			}
			
			context.setFill(color);
			context.setTextAlign(TextAlignment.LEFT);
			context.fillText(pixelList.getColor().getName(), x + listScale * 7.5, y);
		}
	}

	private double getTextWidth(final Font font, final String input) {
		final Text text = new Text();
		text.setFont(font);
		text.setText(input);
		text.setWrappingWidth(0);
		text.setLineSpacing(0);
		return Math.ceil(text.getLayoutBounds().getWidth());
	}

	private double getTextHeight(final Font font, final String input) {
		final Text text = new Text();
		text.setFont(font);
		text.setText(input);
		text.setWrappingWidth(0);
		text.setLineSpacing(0);
		return Math.ceil(text.getLayoutBounds().getHeight());
	}
}
