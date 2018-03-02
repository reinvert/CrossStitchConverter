package com.stitch.converter.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class StitchList {
	private final PixelList pixelList;
	private final IntegerProperty index;
	private final BooleanProperty isHighlighted, isCompleted;
	private final ObjectProperty<StitchColor> color;
	private final StringProperty name, colorString;
	
	@SuppressWarnings("unused")
	private StitchList() {
		throw new AssertionError();
	}
	
	public StitchList(final PixelList pixelList) {
		this.pixelList = pixelList;
		this.index = new SimpleIntegerProperty(pixelList.getIndex());
		this.color = new SimpleObjectProperty<StitchColor>(pixelList.getColor());
		isHighlighted = new SimpleBooleanProperty(pixelList.isHighlighted());
		isCompleted = new SimpleBooleanProperty(pixelList.isCompleted());
		this.name = new SimpleStringProperty(pixelList.getColor().getName());
		colorString = new SimpleStringProperty(pixelList.getColor().getColorString());
	}
	
	public BooleanProperty highlightProperty() {
		return isHighlighted;
	}
	
	public BooleanProperty completeProperty() {
		return isCompleted;
	}
	
	public IntegerProperty indexProperty() {
		return index;
	}
	
	public StringProperty nameProperty() {
		return name;
	}
	
	public StringProperty colorStringProperty() {
		return colorString;
	}
	
	public String getName() {
		return color.get().getName();
	}
	
	public int getIndex() {
		return index.get();
	}
	
	public PixelList getPixelList() {
		return pixelList;
	}

	public Color getColor() {
		return color.get().asFX();
	}
	
	public String getColorString() {
		return color.get().getColorString();
	}
	
	public boolean isHighlighted() {
		return isHighlighted.get();
	}
	
	public boolean isCompleted() {
		return isCompleted.get();
	}
	
	public void setHighlight(boolean highlight) {
		pixelList.setHighlighted(highlight);
		isHighlighted.set(highlight);
	}
	
	public void setCompleted(boolean completed) {
		pixelList.setCompleted(completed);
		isCompleted.set(completed);
	}
	
}
