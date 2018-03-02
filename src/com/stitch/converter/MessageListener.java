package com.stitch.converter;

import com.stitch.converter.model.StitchImage;

interface MessageListener {
	void onTaskCompleted(final StitchImage stitchImage, final double scale);
}
