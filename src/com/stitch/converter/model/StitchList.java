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
	private final ObjectProperty<StitchColor> color;
	private final IntegerProperty index, totalNumber;
	private final BooleanProperty isHighlighted, isCompleted;
	private final StringProperty name, colorString;
	private final PixelList pixelList;

	@SuppressWarnings("unused")
	private StitchList() {
		throw new AssertionError();
	}

	public StitchList(final PixelList pixelList) {
		this.pixelList = pixelList;
		this.index = new SimpleIntegerProperty(pixelList.getIndex());
		this.totalNumber = new SimpleIntegerProperty(pixelList.getCount());
		this.color = new SimpleObjectProperty<StitchColor>(pixelList.getColor());
		isHighlighted = new SimpleBooleanProperty(pixelList.isHighlighted());
		isCompleted = new SimpleBooleanProperty(pixelList.isCompleted());
		this.name = new SimpleStringProperty(pixelList.getColor().getName());
		colorString = new SimpleStringProperty(pixelList.getColor().getColorString());
	}

	public StringProperty colorStringProperty() {
		return colorString;
	}

	public BooleanProperty completeProperty() {
		return isCompleted;
	}

	public Color getColor() {
		return color.get().asFX();
	}

	public String getColorString() {
		return color.get().getColorString();
	}

	public int getIndex() {
		return index.get();
	}

	public String getName() {
		return color.get().getName();
	}

	public PixelList getPixelList() {
		return pixelList;
	}

	public BooleanProperty highlightProperty() {
		return isHighlighted;
	}

	public IntegerProperty indexProperty() {
		return index;
	}

	public boolean isCompleted() {
		return isCompleted.get();
	}

	public boolean isHighlighted() {
		return isHighlighted.get();
	}

	public StringProperty nameProperty() {
		return name;
	}

	public void setCompleted(final boolean completed) {
		pixelList.setCompleted(completed);
		isCompleted.set(completed);
	}

	public void setHighlight(final boolean highlight) {
		pixelList.setHighlighted(highlight);
		isHighlighted.set(highlight);
	}

	public IntegerProperty totalNumberProperty() {
		return totalNumber;
	}

}
