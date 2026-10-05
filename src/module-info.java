module CrossStitchConverter {

    exports com.stitch.converter;

    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;

    requires json.simple;
    requires com.opencsv;
	requires javafx.graphics;

    opens com.stitch.converter.view to javafx.fxml;
}