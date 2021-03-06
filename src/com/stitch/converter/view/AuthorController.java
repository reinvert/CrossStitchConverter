package com.stitch.converter.view;

import com.stitch.converter.Resources;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

public class AuthorController extends Controller {

	@FXML
	public Label copyright;
	
	@FXML
	public Hyperlink hyperlink, iconlink;

	@FXML
	public void initialize() {
		hyperlink.setOnAction(event -> main.getHostServices().showDocument(hyperlink.getText()));
		iconlink.setOnAction(event -> main.getHostServices().showDocument(Resources.getString("icon_url")));
		copyright.setText(Resources.getString("copyright", Resources.getString("version")));
	}
}
