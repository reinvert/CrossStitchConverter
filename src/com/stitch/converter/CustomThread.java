package com.stitch.converter;

import java.util.ArrayList;

import com.stitch.converter.model.StitchImage;

/**
 * Extending thread to custom use.
 * @author Reinvert
 */
class CustomThread extends Thread {
	protected final ArrayList<MessageListener> listenerArray = new ArrayList<MessageListener>();
	
	/**
	 * Adds {@link MessageListener} to receive message.
	 * @param listener - {@link MessageListener} to receive message.
	 */
	protected void addListener(final MessageListener listener) {
		listenerArray.add(listener);
	}
	
	/**
	 * Adds {@link MessageListener} into {@link java.util.ArrayList ArrayList} to receive message.
	 * @param listenerArray - {@link java.util.ArrayList ArrayList} containing {@link MessageListener} to receive message.
	 */
	protected void addListener(final ArrayList<MessageListener> listenerArray) {
		for(final MessageListener listener:listenerArray) {
			this.listenerArray.add(listener);
		}
	}
	
	/**
	 * Sends a {@link java.awt.image.BufferedImage BufferedImage} to the {@link MessageListener}
	 * @param image - the {@link java.awt.image.BufferedImage BufferedImage} to send.
	 */
	protected void sendGraphics(final StitchImage stitchImage, final double scale) {
		for(final MessageListener listener:listenerArray) {
			listener.onTaskCompleted(stitchImage, scale);
		}
	}
}
