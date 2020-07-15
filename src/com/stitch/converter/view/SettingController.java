package com.stitch.converter.view;

import java.util.AbstractMap.SimpleEntry;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.SortedMap;

import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Control;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class SettingController extends Controller {
	static class EditCell<S, T> extends TableCell<S, T> {

		/**
		 * Convenience converter that does nothing (converts Strings to themselves and
		 * vice-versa...).
		 */
		public static final StringConverter<String> IDENTITY_CONVERTER = new StringConverter<String>() {

			@Override
			public String fromString(String string) {
				return string;
			}

			@Override
			public String toString(String object) {
				return object;
			}

		};

		/**
		 * Convenience method for creating an EditCell for a String value.
		 * 
		 * @return
		 */
		public static <S> EditCell<S, String> createStringEditCell() {
			return new EditCell<S, String>(IDENTITY_CONVERTER);
		}

		// Converter for converting the text in the text field to the user type, and
		// vice-versa:
		private final StringConverter<T> converter;

		private T item;

		// Text field for editing
		// TODO: allow this to be a plugable control.
		private final TextField textField = new TextField();

		public EditCell(final StringConverter<T> converter) {
			this.converter = converter;

			itemProperty().addListener((obx, oldItem, newItem) -> {
				if (newItem == null) {
					setText(null);
				} else {
					setText(converter.toString(newItem));
				}
			});
			setGraphic(textField);
			setContentDisplay(ContentDisplay.TEXT_ONLY);

			textField.setOnAction(evt -> {
				commitEdit(this.converter.fromString(textField.getText()));
			});
			textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
				if (!isNowFocused) {
					commitEdit(this.converter.fromString(textField.getText()));
				}
			});
			textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
				if (event.getCode() == KeyCode.ESCAPE) {
					textField.setText(converter.toString(getItem()));
					cancelEdit();
					event.consume();
				} else if (event.getCode() == KeyCode.RIGHT) {
					getTableView().getSelectionModel().selectRightCell();
					event.consume();
				} else if (event.getCode() == KeyCode.LEFT) {
					getTableView().getSelectionModel().selectLeftCell();
					event.consume();
				} else if (event.getCode() == KeyCode.UP) {
					getTableView().getSelectionModel().selectAboveCell();
					event.consume();
				} else if (event.getCode() == KeyCode.DOWN) {
					getTableView().getSelectionModel().selectBelowCell();
					event.consume();
				}
			});
		}

		// revert to text display
		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}

		// commits the edit. Update property if possible and revert to text display
		@Override
		public void commitEdit(T item) {

			this.item = item;
			// This block is necessary to support commit on losing focus, because the
			// baked-in mechanism
			// sets our editing state to false before we can intercept the loss of focus.
			// The default commitEdit(...) method simply bails if we are not editing...
			if (!isEditing() && !item.equals(getItem())) {
				TableView<S> table = getTableView();
				if (table != null) {
					TableColumn<S, T> column = getTableColumn();
					CellEditEvent<S, T> event = new CellEditEvent<>(table,
							new TablePosition<S, T>(table, getIndex(), column), TableColumn.editCommitEvent(), item);
					Event.fireEvent(column, event);
					itemProperty().setValue(item);
				}
			}

			super.commitEdit(item);

			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}

		// set the text of the text field and display the graphic
		@Override
		public void startEdit() {
			super.startEdit();
			if (item != null) {
				textField.setText(converter.toString(item));
			} else {
				textField.setText(converter.toString(itemProperty().getValue()));
			}
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			textField.requestFocus();
		}

	}

	@FXML
	public TableColumn<SimpleEntry<String, String>, String> key, value, description;

	@FXML
	public TableView<SimpleEntry<String, String>> settingTable;

	@FXML
	public void initialize() {
		settingTable.setOnKeyPressed(event -> {
			@SuppressWarnings("unchecked")
			final TablePosition<SimpleEntry<String, String>, ?> pos = settingTable.getFocusModel().getFocusedCell();
			if (pos != null && event.getCode().isLetterKey()) {
				settingTable.edit(pos.getRow(), pos.getTableColumn());
			}
		});
		settingTable.setColumnResizePolicy((param) -> true);
		key.setCellFactory(column -> new TextFieldTableCell<>());
		key.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
		key.setEditable(false);
		value.setCellFactory(column -> new EditCell<SimpleEntry<String, String>, String>(new DefaultStringConverter()));
		value.setCellValueFactory(cellData -> {
			final SimpleEntry<String, String> cellValue = cellData.getValue();
			final StringProperty property = new SimpleStringProperty(cellValue.getValue());
			property.addListener((observable, oldValue, newValue) -> {
				Preferences.setValue(cellValue.getKey(), newValue);
			});
			return property;
		});
		value.setEditable(true);
		description.setCellFactory(cellData -> {
			final TableCell<SimpleEntry<String, String>, String> cell = new TableCell<>();
			final Text text = new Text();
			cell.setGraphic(text);
			cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
			text.wrappingWidthProperty().bind(description.widthProperty());
			text.textProperty().bind(cell.itemProperty());
			return cell;
		});
		description.setCellValueFactory(cellData -> {
			final SimpleEntry<String, String> cellValue = cellData.getValue();
			String description;
			try {
				description = Resources.getString(new StringBuilder(cellValue.getKey()).append("_description").toString());
			} catch (final MissingResourceException e) {
				description = "";
			}
			final StringProperty property = new SimpleStringProperty(description);
			return property;
		});
		description.setEditable(false);
		final SortedMap<String, String> keyStore = Preferences.getKeyStore();
		final Set<String> keySet = keyStore.keySet();
		final ObservableList<SimpleEntry<String, String>> entryList = FXCollections.observableArrayList();
		for (final String key : keySet) {
			entryList.add(new SimpleEntry<String, String>(key, keyStore.get(key)));
		}
		settingTable.setItems(entryList);
	}
}
