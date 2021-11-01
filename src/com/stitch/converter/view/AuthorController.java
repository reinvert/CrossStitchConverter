package com.stitch.converter.view;

import com.stitch.converter.Resources;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;

public class AuthorController extends Controller {

	@FXML
	public TextArea copyright;
	
	@FXML
	public Hyperlink hyperlink, iconlink, googleiconlink;

	@FXML
	public void initialize() {
		hyperlink.setOnAction(event -> main.getHostServices().showDocument(hyperlink.getText()));
		iconlink.setOnAction(event -> main.getHostServices().showDocument(Resources.getString("icon_url")));
		googleiconlink.setOnAction(event -> main.getHostServices().showDocument(Resources.getString("google_icon_url")));
		copyright.setText(Resources.getString("copyright", Resources.getString("version")));
	}
}
