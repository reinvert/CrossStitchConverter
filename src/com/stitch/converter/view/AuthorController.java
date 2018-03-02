package com.stitch.converter.view;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;

public class AuthorController extends Controller{
	
	@FXML
	Hyperlink hyperlink;
	
	@FXML
	public void initialize() {
        hyperlink.setOnAction(event -> main.getHostServices().showDocument(hyperlink.getText()));
	}
}
