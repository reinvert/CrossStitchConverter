package com.stitch.converter.view;

import java.util.AbstractMap.SimpleEntry;
import java.util.MissingResourceException;
import java.util.SortedMap;
import java.util.stream.Collectors;

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
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Text;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class SettingController extends Controller {

    static class EditCell<S, T> extends TableCell<S, T> {
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

        public static <S> EditCell<S, String> createStringEditCell() {
            return new EditCell<>(IDENTITY_CONVERTER);
        }

        private final StringConverter<T> converter;
        private T item;
        private final TextField textField = new TextField();

        public EditCell(final StringConverter<T> converter) {
            this.converter = converter;
            initializeListeners();
            setGraphic(textField);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        private void initializeListeners() {
            itemProperty().addListener((obx, oldItem, newItem) -> updateText(newItem));
            textField.setOnAction(evt -> commitEditFromTextField());
            textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (!isNowFocused) {
                    commitEditFromTextField();
                }
            });
            textField.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
        }

        private void updateText(T newItem) {
            setText(newItem == null ? null : converter.toString(newItem));
        }

        private void commitEditFromTextField() {
            commitEdit(converter.fromString(textField.getText()));
        }

        private void handleKeyPressed(KeyEvent event) {
            switch (event.getCode()) {
                case ESCAPE:
                    cancelEditAndResetText();
                    event.consume();
                    break;
                case UP:
                    getTableView().getSelectionModel().selectAboveCell();
                    cancelEdit();
                    event.consume();
                    break;
                case DOWN:
                    getTableView().getSelectionModel().selectBelowCell();
                    cancelEdit();
                    event.consume();
                    break;
                default:
                    break;
            }
        }

        private void cancelEditAndResetText() {
            textField.setText(converter.toString(getItem()));
            cancelEdit();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        @Override
        public void commitEdit(T item) {
            this.item = item;
            if (!isEditing() && !item.equals(getItem())) {
                fireEditEvent(item);
            }
            super.commitEdit(item);
            setContentDisplay(ContentDisplay.TEXT_ONLY);
        }

        private void fireEditEvent(T item) {
            TableView<S> table = getTableView();
            if (table != null) {
                TableColumn<S, T> column = getTableColumn();
                CellEditEvent<S, T> event = new CellEditEvent<>(table,
                        new TablePosition<>(table, getIndex(), column), TableColumn.editCommitEvent(), item);
                Event.fireEvent(column, event);
                itemProperty().setValue(item);
            }
        }

        @Override
        public void startEdit() {
            super.startEdit();
            textField.setText(converter.toString(item != null ? item : itemProperty().getValue()));
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
        setupTableKeyPressHandler();
        setupTableColumns();
        loadTableData();
    }

    private void setupTableKeyPressHandler() {
        settingTable.setOnKeyPressed(event -> {
            @SuppressWarnings("unchecked")
			TablePosition<SimpleEntry<String, String>, ?> pos = settingTable.getFocusModel().getFocusedCell();
            if (pos != null && event.getCode().isLetterKey()) {
                settingTable.edit(pos.getRow(), pos.getTableColumn());
            }
        });
        settingTable.setColumnResizePolicy((param) -> true);
    }

    private void setupTableColumns() {
        setupKeyColumn();
        setupValueColumn();
        setupDescriptionColumn();
    }

    private void setupKeyColumn() {
        key.setCellFactory(column -> new TextFieldTableCell<>());
        key.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getKey()));
        key.setEditable(false);
    }

    private void setupValueColumn() {
        value.setCellFactory(column -> new EditCell<>(new DefaultStringConverter()));
        value.setCellValueFactory(cellData -> {
            SimpleEntry<String, String> cellValue = cellData.getValue();
            StringProperty property = new SimpleStringProperty(cellValue.getValue());
            property.addListener((observable, oldValue, newValue) -> {
                Preferences.setValue(cellValue.getKey(), newValue);
            });
            return property;
        });
        value.setEditable(true);
    }

    private void setupDescriptionColumn() {
        description.setCellFactory(cellData -> createDescriptionCell());
        description.setCellValueFactory(cellData -> {
            SimpleEntry<String, String> cellValue = cellData.getValue();
            String descriptionText = getDescription(cellValue.getKey());
            return new SimpleStringProperty(descriptionText);
        });
        description.setEditable(false);
    }

    private TableCell<SimpleEntry<String, String>, String> createDescriptionCell() {
        TableCell<SimpleEntry<String, String>, String> cell = new TableCell<>();
        Text text = new Text();
        cell.setGraphic(text);
        cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
        text.wrappingWidthProperty().bind(description.widthProperty());
        text.textProperty().bind(cell.itemProperty());
        return cell;
    }

    private String getDescription(String key) {
        try {
            return Resources.getString(key + "_description");
        } catch (MissingResourceException e) {
            return ""; // Return empty string if resource is not found
        }
    }

    private void loadTableData() {
        SortedMap<String, String> keyStore = Preferences.getKeyStore();
        ObservableList<SimpleEntry<String, String>> entryList = FXCollections.observableArrayList(
            keyStore.entrySet().stream()
                     .map(entry -> new SimpleEntry<>(entry.getKey(), entry.getValue()))
                     .collect(Collectors.toList())
        );
        settingTable.setItems(entryList);
    }
}
