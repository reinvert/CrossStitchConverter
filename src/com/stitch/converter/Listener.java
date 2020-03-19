package com.stitch.converter;

import com.stitch.converter.model.StitchImage;

public interface Listener {
	void onFinished(final StitchImage image);
}
