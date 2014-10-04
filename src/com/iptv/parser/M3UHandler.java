package com.iptv.parser;

/**
 * The handler. It's important to work with M3UParser.
 * 
 * @author Ke
 */
public interface M3UHandler {
	/**
	 * When M3UParser get a M3UHead, this method will be called.
	 * 
	 * @param header
	 *            the instance of M3UHead.
	 */
	public void onReadEXTM3U(M3UHead header);

	/**
	 * When M3UParser get a M3UItem, this method will be called.
	 * 
	 * @param item
	 *            the instance of M3UItem.
	 */
	public void onReadEXTINF(M3UItem item);
}
