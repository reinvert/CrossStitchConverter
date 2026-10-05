
package com.stitch.converter.view;

import java.util.Collection;
import java.util.HashMap;

import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;
import com.stitch.converter.model.Pixel;
import com.stitch.converter.model.PixelList;
import com.stitch.converter.model.StitchColor;
import com.stitch.converter.model.StitchImage;

import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

class CanvasController {
	final Canvas canvas;
	final GraphicsContext context;
	final StitchImage image;
	private boolean isHighlightExist = false, isHighlightAlternate = false, drawGridNumber = false;
	private int highlightedCount = 0;
	protected double scale = 10.0d, margin = scale;

	private static final Color DARK_GRAY_1 = new Color(0.0d, 0.0d, 0.0d, 1d);
	private static final Color DARK_GRAY_2 = new Color(0.0d, 0.0d, 0.0d, 0.25d);
	private static final Color DARK_GRAY_3 = new Color(0.0d, 0.0d, 0.0d, 0.1d);

	private static final Color BRIGHT_GRAY_1 = new Color(1.0d, 1.0d, 1.0d, 1d);
	private static final Color BRIGHT_GRAY_2 = new Color(1.0d, 1.0d, 1.0d, 0.25d);
	private static final Color BRIGHT_GRAY_3 = new Color(1.0d, 1.0d, 1.0d, 0.1d);

	private Color backgroundColor, highlightAlternateColor, circleColor;

	private final String fontName;
	
	private int highlightX = -1, highlightY = -1;
	
	private Font originalFont;
	private final HashMap<Integer, Font> fontBySize = new HashMap<>();
	
	protected CanvasController(final StitchImage image, final Canvas canvas) {
		this.image = image;
		this.canvas = canvas;
		
		this.context = canvas.getGraphicsContext2D();
	    context.setTextAlign(TextAlignment.CENTER);
	    context.setTextBaseline(VPos.CENTER);
		
		backgroundColor = Preferences.getColor("completedFillColor", new StitchColor(255, 255, 0, "")).asFX();
		highlightAlternateColor = Preferences.getColor("highlightAlternateColor", new StitchColor(128, 255, 128, "")).asFX();
		isHighlightAlternate = Preferences.getBoolean("isHighlightAlternate", false);
		
		circleColor = Preferences.getColor("distanceCircleColor", new StitchColor(128, 128, 255, "")).asFX();
		
		fontName = Preferences.getValue("fontType", "");
		originalFont = new Font(fontName, scale);
	    text.setFont(originalFont);
	    text.setWrappingWidth(0);
	    text.setLineSpacing(0);
		
		{
			for (final PixelList pixelList : image.getPixelLists()) {
	            if (pixelList.isHighlighted()) {
	                highlightedCount++;
	            }
	        }
			isHighlightExist = highlightedCount > 0;

		}
	}

	private void drawGrid(
	        final int x,
	        final int y,
	        final int width,
	        final int height,
	        final boolean isHighlightExist) {

	    final double startX = x * scale + margin;
	    final double startY = y * scale + margin;
	    final double gridWidth = width * scale;
	    final double gridHeight = height * scale;

	    final Color gridColor1 = isHighlightExist
	            ? BRIGHT_GRAY_3
	            : DARK_GRAY_3;

	    final Color gridColor2 = isHighlightExist
	            ? BRIGHT_GRAY_2
	            : DARK_GRAY_2;

	    final Color gridColor3 = isHighlightExist
	            ? BRIGHT_GRAY_1
	            : DARK_GRAY_1;

	    context.setFill(gridColor1);

	    for (int count = 1; count <= width; count++) {
	        if (count % 5 != 0) {
	            context.fillRect(
	                    startX + count * scale,
	                    startY,
	                    1,
	                    gridHeight
	            );
	        }
	    }

	    for (int count = 1; count <= height; count++) {
	        if (count % 5 != 0) {
	            context.fillRect(
	                    startX,
	                    startY + count * scale,
	                    gridWidth,
	                    1
	            );
	        }
	    }

	    context.setFill(gridColor2);

	    for (int count = 5; count <= width; count += 10) {
	        context.fillRect(
	                startX + count * scale,
	                startY,
	                1,
	                gridHeight
	        );
	    }

	    for (int count = 5; count <= height; count += 10) {
	        context.fillRect(
	                startX,
	                startY + count * scale,
	                gridWidth,
	                1
	        );
	    }

	    context.setFill(gridColor3);

	    for (int count = 0; count <= width; count += 10) {
	        context.fillRect(
	                startX + count * scale,
	                startY,
	                1,
	                gridHeight
	        );
	    }

	    for (int count = 0; count <= height; count += 10) {
	        context.fillRect(
	                startX,
	                startY + count * scale,
	                gridWidth,
	                1
	        );
	    }
	}

	private void drawIndex() {
	    final double cellOffset = scale / 2 + margin;
	    Font currentFont = null;
	    Color currentFill = null;

	    for (final PixelList pixelList : image.getPixelLists()) {
	    	final Color indexColor;
	        if (pixelList.isCompleted()) continue;

	        final StitchColor color = pixelList.getColor();
	        if (color.equals(image.getBackground())) continue;

	        final String indexText = Integer.toString(pixelList.getIndex());
	        final Font indexFont = getFontForText(indexText);

	        if (indexFont != currentFont) {
	            context.setFont(indexFont);
	            currentFont = indexFont;
	        }

	        if (isHighlightExist) {
	            indexColor = Color.BLACK;
	        } else {
	            if (color.getRed() + color.getGreen() + color.getBlue() < 128 * 3) {
	                indexColor = Color.WHITE;
	            } else {
	            	indexColor = Color.BLACK;
	            }
	        }
	        
	        if (indexColor != currentFill) {
	            context.setFill(indexColor);
	            currentFill = indexColor;
	        }

	        for (final Pixel pixel : pixelList.getPixelSet()) {
	            context.fillText(
	                    indexText,
	                    pixel.getX() * scale + cellOffset,
	                    pixel.getY() * scale + cellOffset
	            );
	        }
	    }
	}

	final Text text = new Text();

	private Font getFontForText(final String input) {
		text.setFont(originalFont);
	    text.setText(input);
	    
	    final Bounds bounds = text.getLayoutBounds();

	    final double textWidth =
	    		bounds.getWidth();
	    final double textHeight =
	    		bounds.getHeight();
	    final double textScale = Math.min(
	            scale / textWidth,
	            scale / textHeight
	    );

	    final int fontSize = (int) (originalFont.getSize() * textScale);

	    Font newFont = fontBySize.get(fontSize);

	    if (newFont == null) {
	        newFont = new Font(fontName, fontSize);
	        fontBySize.put(fontSize, newFont);
	    }

	    return newFont;
	}
	
	protected Canvas getCanvas() {
		return canvas;
	}

	protected StitchImage getImage() {
		return image;
	}

	protected double getMargin() {
		return margin;
	}

	protected double getScale() {
		return scale;
	}
	
	protected void updateHighlighted(final boolean highlighted) {
	    if (highlighted) {
	        highlightedCount++;
	    } else {
	        highlightedCount--;
	    }

	    isHighlightExist = highlightedCount > 0;
	}
	
	protected void invalidate() {
		drawGridNumber = Preferences.getBoolean("drawGridNumber", true);
		renderImage();
		drawGrid(0, 0, (int) image.getWidth(), (int) image.getHeight(), isHighlightExist);
		if (drawGridNumber) {
			drawIndex();
		}
		if(highlightX != -1 && highlightY != -1) {
			drawHighlightPixel(highlightX, highlightY);
		}
		if(isDrawDistanceCircle) {
			drawDistanceCircle();
		}
	}

	private void renderImage() {
		final double highlightBrightnessLevel =
		        isHighlightExist
		            ? Preferences.getDouble("highlightBrightnessLevel", 0.75d)
		            : 0.0d;
	    context.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
	    image.setNumberVisible(drawGridNumber);

	    final Collection<PixelList> pixelLists = image.getPixelLists();
	    for (final PixelList pixelList : pixelLists) {
	        final Collection<Pixel> pixels = pixelList.getPixelSet();
	        
	        final boolean isCompleted = pixelList.isCompleted();
	        if (isCompleted) {
	            context.setFill(backgroundColor);

	            for (final Pixel pixel : pixels) {
	                final double pixelX = margin + pixel.getX() * scale;
	                final double pixelY = margin + pixel.getY() * scale;

	                context.fillRect(pixelX, pixelY, scale, scale);
	            }

	            continue;
	        }

	        final boolean isHighlighted = pixelList.isHighlighted();
	        if (isHighlighted) {
	            if (isHighlightAlternate) {
	                for (final Pixel pixel : pixels) {
	                    final double pixelX = margin + pixel.getX() * scale;
	                    final double pixelY = margin + pixel.getY() * scale;

	                    if ((pixel.getX() + pixel.getY()) % 2 == 0) {
	                        context.setFill(highlightAlternateColor);
	                    } else {
	                        context.setFill(Color.WHITE);
	                    }

	                    context.fillRect(pixelX, pixelY, scale, scale);
	                }
	            } else {
	                context.setFill(Color.WHITE);

	                for (final Pixel pixel : pixels) {
	                    final double pixelX = margin + pixel.getX() * scale;
	                    final double pixelY = margin + pixel.getY() * scale;

	                    context.fillRect(pixelX, pixelY, scale, scale);
	                }
	            }

	            continue;
	        }

	        final Color pixelColor = pixelList.getColor().asFX();
	        final Color fillColor = isHighlightExist
	                ? getDarkenedColor(pixelColor, highlightBrightnessLevel)
	                : pixelColor;

	        context.setFill(fillColor);

	        for (final Pixel pixel : pixels) {
	            final double pixelX = margin + pixel.getX() * scale;
	            final double pixelY = margin + pixel.getY() * scale;

	            context.fillRect(pixelX, pixelY, scale, scale);
	        }
	    }
	}
	
	private Color getDarkenedColor(
	        final Color color,
	        final double alpha) {

	    return new Color(
	        color.getRed() * (1.0d - alpha),
	        color.getGreen() * (1.0d - alpha),
	        color.getBlue() * (1.0d - alpha),
	        color.getOpacity()
	    );
	}
	
	private void updateCanvasSize() {
	    canvas.setWidth(image.getWidth() * scale + 2 * margin);
	    canvas.setHeight(image.getHeight() * scale + 2 * margin);
	}

	protected void setMargin(final double margin) {
	    this.margin = margin;
	    updateCanvasSize();
	}

	protected double setScale(final double scale) {
	    final double canvasWidth = image.getWidth() * scale + 2.0d * margin;
	    final double canvasHeight = image.getHeight() * scale + 2.0d * margin;
	    final double canvasPixels = canvasWidth * canvasHeight;

	    double actualScale = scale;

	    if (canvasPixels > MAX_CANVAS_PIXELS) {
	        final ButtonType reduceButton = new ButtonType(
	                Resources.getString("zoom_number_reduce")
	        );
	        final ButtonType ignoreButton = new ButtonType(
	                Resources.getString("zoom_number_ignore")
	        );
	        final ButtonType cancelButton = new ButtonType(
	                Resources.getString("cancel_button"),
	                ButtonData.CANCEL_CLOSE
	        );

	        final Alert alert = new Alert(Alert.AlertType.WARNING);

	        alert.getDialogPane().getStylesheets().add(Resources.getCSS());
	        alert.setTitle(Resources.getString("warning"));
	        alert.setHeaderText(Resources.getString("zoom_number_too_large"));
	        alert.setContentText(
	                Resources.getString("zoom_number_large_description")
	        );
	        alert.getButtonTypes().setAll(
	                reduceButton,
	                ignoreButton,
	                cancelButton
	        );
	        
	        try {
		        final ButtonType result = alert.showAndWait()
		                .orElse(cancelButton);

		        if (result == reduceButton) {
		            actualScale = getMaximumScale();

		        } else if (result == ignoreButton) {
		            final Alert confirmation = new Alert(Alert.AlertType.WARNING);
		            confirmation.setTitle(Resources.getString("warning"));
		            confirmation.getDialogPane().getStylesheets().add(Resources.getCSS());
		            confirmation.setHeaderText(null);
		            confirmation.setContentText(
		                    Resources.getString("zoom_number_ignore_description")
		            );

		            final ButtonType yesButton = new ButtonType(
		                    Resources.getString("ok_button")
		            );
		            final ButtonType noButton = new ButtonType(
		                    Resources.getString("cancel_button"),
		                    ButtonData.CANCEL_CLOSE
		            );

		            confirmation.getButtonTypes().setAll(
		                    yesButton,
		                    noButton
		            );
		            
		            final ButtonType confirmationResult = confirmation.showAndWait()
			                .orElse(noButton);
		            if(confirmationResult == noButton) {
		            	actualScale = getMaximumScale();
		            }

		        }
	        } catch(final IllegalStateException e) {
	        	
	        }
	    }

	    this.scale = actualScale;
	    originalFont = new Font(fontName, actualScale);
	    fontBySize.clear();
	    updateCanvasSize();
        return this.scale;
	}
	
	protected void setHighlightPixel(final int x, final int y) {
		highlightX = x;
		highlightY = y;
	}
	
	protected void drawHighlightPixel(final int x, final int y) {
		context.setStroke(Color.RED);
		context.strokeRect(x * scale + margin, y * scale + margin, scale, scale);
	}
	
	private boolean isDrawDistanceCircle = false;
	private double distanceCircleX = 0, distanceCircleY = 0;
	
	protected void startDrawDistanceCircle(final double x, final double y) {
		isDrawDistanceCircle = true;
		distanceCircleX = x;
		distanceCircleY = y;
	}
	
	protected void stopDrawDistanceCircle() {
		isDrawDistanceCircle = false;
	}
	
	protected void drawDistanceCircle() {
		final double circleSize = Preferences.getDouble("distanceCircleSize", 20d) * scale;
		
		context.setStroke(circleColor);
		context.strokeOval(distanceCircleX - circleSize/2, distanceCircleY - circleSize/2, circleSize, circleSize);
	}
	
	private static final double MAX_CANVAS_PIXELS = 10_000_000.0d;

	private double getMaximumScale() {
	    final double width = image.getWidth();
	    final double height = image.getHeight();
	    final double marginSize = 2.0d * margin;

	    final double a = width * height;
	    final double b = (width + height) * marginSize;
	    final double c = marginSize * marginSize - MAX_CANVAS_PIXELS;

	    return (-b + Math.sqrt(b * b - 4.0d * a * c)) / (2.0d * a);
	}
	
	
	protected GraphicsContext getGraphicsContext() {
	    return context;
	}
	
	protected double getCanvasHeight() {
	    return canvas.getHeight();
	}

	protected void setCanvasWidth(final double width) {
	    canvas.setWidth(width);
	}
}