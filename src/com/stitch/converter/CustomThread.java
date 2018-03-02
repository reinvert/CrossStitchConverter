package com.stitch.converter;

import java.util.ArrayList;
import java.util.Collection;

import com.stitch.converter.model.StitchImage;

/**
 * Extending thread to custom use.
 * 
 * @author Reinvert
 */
class CustomThread extends Thread {
	final ArrayList<MessageListener> listenerArray = new ArrayList<MessageListener>();

	/**
	 * Adds {@link MessageListener} into {@link ArrayList} to receive message.
	 * 
	 * @param listeners
	 *            - {@link MessageListener MessageListeners} to receive message.
	 */
	void addListener(final MessageListener... listeners) {
		for (final MessageListener listener : listeners) {
			listenerArray.add(listener);
		}
	}

	/**
	 * Adds {@link MessageListener} into {@link ArrayList} to receive message.
	 * 
	 * @param listener
	 *            - {@link MessageListener} to receive message.
	 */
	void addListener(final Collection<MessageListener> listeners) {
		for (final MessageListener listener : listeners) {
			listenerArray.add(listener);
		}
	}

	/**
	 * Sends a {@link StitchImage} to the {@link MessageListener}
	 * 
	 * @param image
	 *            - the {@link StitchImage} to send.
	 */
	void sendGraphics(final StitchImage stitchImage, final double scale) {
		for (final MessageListener listener : listenerArray) {
			listener.onTaskCompleted(stitchImage, scale);
		}
	}
}
