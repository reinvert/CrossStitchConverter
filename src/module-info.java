/**
 * 
 */
/**"fxml"
 * @author reinv
 *
 */
module CrossStitchConverter {
	exports com.stitch.converter.model;
	exports com.stitch.converter.view;
	exports com.stitch.converter;

	requires transitive javafx.base;
	requires transitive javafx.controls;
	requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive javafx.swing;
	requires transitive json.simple;
	requires com.opencsv;
	
    opens com.stitch.converter.view to javafx.fxml;

}