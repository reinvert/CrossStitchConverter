package com.stitch.converter;

import java.io.File;

import com.stitch.converter.model.StitchImage;

public class HTMLConverter {
	private final StitchImage image;
	private String title = "";
	
	public HTMLConverter(final StitchImage image) {
		this.image = image;
	}
	
	public void setTitle(final String title) {
		this.title = title;
	}
	
	public String convert() {
		final StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n").append("\n");
		html.append("<html>").append("\n");
		html.append("\t<head>").append("\n");
		html.append("\t\t<title>").append(title).append("</title>").append("\n");
		html.append("\t</head>").append("\n");
		html.append("\t<body>").append("\n");
		html.append("\t\t<canvas id=\"canvas\" />").append("\n");
		html.append("").append("\n");
		html.append("").append("\n");
		html.append("").append("\n");
		html.append("").append("\n");
		
		return html.toString();
	}
}
