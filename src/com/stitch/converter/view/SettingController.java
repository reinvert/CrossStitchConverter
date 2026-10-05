package com.stitch.converter.view;

import java.util.MissingResourceException;

import com.stitch.converter.Preferences;
import com.stitch.converter.Resources;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.text.Text;

public class SettingController extends Controller {

    /**
     * TableView에서 사용하는 설정 항목 모델.
     *
     * UI 데이터는 JavaFX Property로 관리하여 TableView의 편집과
     * 값 변경을 자연스럽게 연결한다.
     */
    public static class SettingItem {

        private final StringProperty key = new SimpleStringProperty();
        private final StringProperty value = new SimpleStringProperty();
        private final StringProperty description = new SimpleStringProperty();

        public SettingItem(String key, String value, String description) {
            this.key.set(key);
            this.value.set(value);
            this.description.set(description);
        }

        public String getKey() {
            return key.get();
        }
        
        public void setValue(String value) {
            this.value.set(value);
        }

        public StringProperty valueProperty() {
            return value;
        }

        public StringProperty descriptionProperty() {
            return description;
        }
    }

    @FXML
    private TableColumn<SettingItem, String> key;

    @FXML
    private TableColumn<SettingItem, String> value;

    @FXML
    private TableColumn<SettingItem, String> description;

    @FXML
    private TableView<SettingItem> settingTable;

    @FXML
    public void initialize() {
        setupTable();
        loadTableData();
    }

    private void setupTable() {
        setupKeyColumn();
        setupValueColumn();
        setupDescriptionColumn();

        settingTable.setEditable(true);
    }

    private void setupKeyColumn() {
    	key.setCellValueFactory(cellData ->
        new SimpleStringProperty(cellData.getValue().getKey()));
    }

    private void setupValueColumn() {
        value.setCellValueFactory(cellData ->
                cellData.getValue().valueProperty());

        value.setCellFactory(TextFieldTableCell.forTableColumn());

        /*
         * 직접 편집 이벤트를 처리한다.
         *
         * TextFieldTableCell이 새 값을 생성하면 이 값을 Property에 반영하고,
         * Property의 listener가 Preferences에 저장한다.
         */
        value.setOnEditCommit(event -> {
            SettingItem item = event.getRowValue();

            if (item != null) {
                String newValue = event.getNewValue();

                item.setValue(newValue);
                Preferences.setValue(item.getKey(), newValue);
            }
        });

        value.setEditable(true);
    }

    private void setupDescriptionColumn() {
        description.setCellValueFactory(cellData ->
                cellData.getValue().descriptionProperty());

        description.setCellFactory(column -> createDescriptionCell());
        description.setEditable(false);
    }

    private TableCell<SettingItem, String> createDescriptionCell() {
        TableCell<SettingItem, String> cell = new TableCell<>() {

            private final Text text = new Text();

            {
                text.wrappingWidthProperty()
                        .bind(widthProperty().subtract(8));
                setPrefHeight(TableCell.USE_COMPUTED_SIZE);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null || item.isEmpty()) {
                    text.setText(null);
                    setGraphic(null);
                } else {
                    text.setText(item);
                    setGraphic(text);
                }
            }
        };

        return cell;
    }

    private String getDescription(final String key) {
        String description;
        try {
        	description = Resources.getString(key + "_description");
        } catch(final NullPointerException | ClassCastException | MissingResourceException e) {
        	description = "";
        }

        return description;
    }

    private void loadTableData() {
        ObservableList<SettingItem> items =
            FXCollections.observableArrayList();

        for (String key : Preferences.getKeys().stream().sorted().toList()) {
            String value = Preferences.getValue(key, "");
            String description = getDescription(key);

            items.add(new SettingItem(key, value, description));
        }

        settingTable.setItems(items);
    }
}