package com.stitch.converter.view;

import com.stitch.converter.Preferences;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchImage;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

class Blueprint {

    private final CanvasController canvasController;
    private double listScale;
    
	private double listZeroX, listZeroY;

	protected Blueprint(final StitchImage image, final Canvas canvas) {
	    canvasController = new CanvasController(image, canvas);
	    listScale = canvasController.getScale();
	    listZeroY = canvasController.getMargin();

	    updateListLayout();
	}
	
	protected void invalidate() {
	    canvasController.invalidate();
	    renderLists();
	}

	private void renderLists() {
	    final GraphicsContext context = canvasController.getGraphicsContext();
	    final StitchImage image = canvasController.getImage();
	    final double canvasHeight = canvasController.getCanvasHeight();
	    final double margin = canvasController.getMargin();

	    context.setTextBaseline(VPos.TOP);
	    context.setFont(
	        new Font(
	            Preferences.getValue("fontType", ""),
	            listScale / 1.3
	        )
	    );

	    int xCount = 0;
	    int yCount = 0;

	    for (final PixelList pixelList : image.getPixelLists()) {
	    	final Color color = pixelList.getColor().asFX();
	    	final String name = pixelList.getColor().getName();

	        double x = listZeroX + xCount * listScale * 10;
	        double y = listZeroY + yCount * listScale;

	        if (y + listScale > canvasHeight - margin * 2) {
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

	        context.setTextAlign(TextAlignment.LEFT);
	        context.fillText(
	            name,
	            x + listScale * 7.5,
	            y
	        );
	    }
	}
	
	private void updateListLayout() {
	    final StitchImage image = canvasController.getImage();
	    final double scale = canvasController.getScale();
	    final double margin = canvasController.getMargin();

	    final int listSize = image.getPixelLists().size();
	    final int row = (int) (image.getHeight() * scale / listScale);
	    final int column = Integer.max((int) (listSize / row) + 1, 1);

	    listZeroX = image.getWidth() * scale + 2 * margin;

	    canvasController.setCanvasWidth(
	        listZeroX + column * listScale * 10 + margin
	    );
	}
	
	protected void setScale(final double scale) {
	    canvasController.setScale(scale);
	    updateListLayout();
	}
	
	protected void setListScale(final double listScale) {
	    this.listScale = listScale;
	    updateListLayout();
	}
	
	protected Canvas getCanvas() {
		return canvasController.getCanvas();
	}
}
