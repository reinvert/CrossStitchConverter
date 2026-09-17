module CrossStitchConverter {

    exports com.stitch.converter;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;

    requires json.simple;
    requires com.opencsv;

    opens com.stitch.converter.view to javafx.fxml;
}